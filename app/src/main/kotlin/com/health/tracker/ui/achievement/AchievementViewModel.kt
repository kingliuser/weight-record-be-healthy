package com.health.tracker.ui.achievement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.tracker.domain.AchievementCode
import com.health.tracker.domain.AchievementEvaluator
import com.health.tracker.domain.AchievementFamily
import com.health.tracker.domain.AchievementProgress
import com.health.tracker.domain.AchievementRepository
import com.health.tracker.domain.AchievementUnlock
import com.health.tracker.domain.DayStats
import com.health.tracker.domain.DayTotals
import com.health.tracker.domain.LedgerRepository
import com.health.tracker.domain.ProfileRepository
import com.health.tracker.domain.TargetRecommender
import com.health.tracker.domain.WeightRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 刚拿到手的成就，带日期，给顶部那条"最近解锁"用。 */
data class UnlockMoment(val progress: AchievementProgress, val date: LocalDate)

data class AchievementUiState(
    val progress: List<AchievementProgress> = emptyList(),
    val unlocked: Set<AchievementCode> = emptySet(),
    val unlockedAt: Map<AchievementCode, LocalDate> = emptyMap(),
    val daysConsidered: Int = 0,
) {
    val total: Int get() = progress.size
    val unlockedCount: Int get() = progress.count { it.code in unlocked }
    val completionPercent: Double get() = if (total == 0) 0.0 else unlockedCount * 100.0 / total
    val completionRatio: Double get() = if (total == 0) 0.0 else unlockedCount.toDouble() / total

    /** 差一点点就能拿的那条，用来提醒"再坚持一下"。 */
    val closestLocked: AchievementProgress? get() = progress.filter { it.code !in unlocked }.maxByOrNull { it.ratio }
    val latestUnlock: UnlockMoment?
        get() = unlockedAt.maxByOrNull { it.value }?.let { (code, date) ->
            progress.firstOrNull { it.code == code }?.let { UnlockMoment(it, date) }
        }

    fun unlockedIn(family: AchievementFamily): Int = progress.count { it.code.family == family && it.code in unlocked }

    fun totalIn(family: AchievementFamily): Int = progress.count { it.code.family == family }

    fun isUnlocked(code: AchievementCode): Boolean = code in unlocked
}

private data class Snapshot(
    val progress: List<AchievementProgress>,
    val stored: List<AchievementUnlock>,
    val daysConsidered: Int,
)

class AchievementViewModel(
    private val profileRepository: ProfileRepository,
    private val weightRepository: WeightRepository,
    private val ledgerRepository: LedgerRepository,
    private val achievementRepository: AchievementRepository,
) : ViewModel() {

    private val today = LocalDate.now()

    // 累计类成就要看得到全部历史，两年窗口对个人记录来说足够。
    private val windowFrom = today.minusDays(729)

    private val snapshot = combine(
        profileRepository.observe(),
        weightRepository.observeDaily(),
        ledgerRepository.observeDayTotals(windowFrom, today),
        achievementRepository.observeUnlocks(),
    ) { profile, weights, totals, stored ->
        val referenceWeight = totals.lastNotNullWeight() ?: weights.lastOrNull()?.weightKg
        val target = if (profile != null && referenceWeight != null) {
            TargetRecommender.recommend(profile, referenceWeight, today)
        } else {
            null
        }
        val stats = totals.map { day ->
            DayStats(
                date = day.date,
                intakeKcal = day.intakeKcal,
                exerciseKcal = day.exerciseKcal,
                tdeeKcal = target?.tdeeKcal ?: 0.0,
                exerciseTargetKcal = target?.exerciseFloorKcal ?: 0.0,
                weightKg = day.weightKg,
                mealCount = day.mealCount,
                activityCount = day.activityCount,
                exerciseMinutes = day.exerciseMinutes,
            )
        }
        Snapshot(
            progress = AchievementEvaluator.evaluate(stats),
            stored = stored,
            daysConsidered = stats.count { it.logged },
        )
    }

    val state: StateFlow<AchievementUiState> = snapshot
        .map { data ->
            val computed = data.progress.filter { it.unlocked }.map { it.code }.toSet()
            AchievementUiState(
                progress = data.progress,
                unlocked = data.stored.map { it.code }.toSet() + computed,
                unlockedAt = data.stored.associate { it.code to it.atMillis.toLocalDate() },
                daysConsidered = data.daysConsidered,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AchievementUiState())

    init {
        viewModelScope.launch {
            snapshot.collect { data ->
                val storedCodes = data.stored.map { it.code }.toSet()
                AchievementEvaluator.newlyUnlocked(data.progress, storedCodes).forEach { progress ->
                    achievementRepository.unlock(progress.code, progress.currentValue, System.currentTimeMillis())
                }
            }
        }
    }
}

private fun List<DayTotals>.lastNotNullWeight(): Double? = lastOrNull { it.weightKg != null }?.weightKg

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
