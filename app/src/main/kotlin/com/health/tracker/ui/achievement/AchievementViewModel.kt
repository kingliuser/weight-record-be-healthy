package com.health.tracker.ui.achievement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.tracker.domain.AchievementCode
import com.health.tracker.domain.AchievementEvaluator
import com.health.tracker.domain.AchievementProgress
import com.health.tracker.domain.AchievementRepository
import com.health.tracker.domain.DayStats
import com.health.tracker.domain.DayTotals
import com.health.tracker.domain.LedgerRepository
import com.health.tracker.domain.ProfileRepository
import com.health.tracker.domain.TargetRecommender
import com.health.tracker.domain.WeightRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AchievementUiState(
    val progress: List<AchievementProgress> = emptyList(),
    val unlocked: Set<AchievementCode> = emptySet(),
    val daysConsidered: Int = 0,
)

private data class Snapshot(
    val progress: List<AchievementProgress>,
    val storedUnlocked: Set<AchievementCode>,
    val daysConsidered: Int,
)

class AchievementViewModel(
    private val profileRepository: ProfileRepository,
    private val weightRepository: WeightRepository,
    private val ledgerRepository: LedgerRepository,
    private val achievementRepository: AchievementRepository,
) : ViewModel() {

    private val today = LocalDate.now()
    private val windowFrom = today.minusDays(119)

    private val snapshot = combine(
        profileRepository.observe(),
        weightRepository.observeDaily(),
        ledgerRepository.observeDayTotals(windowFrom, today),
        achievementRepository.observeUnlocked(),
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
            )
        }
        Snapshot(
            progress = AchievementEvaluator.evaluate(stats),
            storedUnlocked = stored,
            daysConsidered = stats.count { it.weightKg != null || it.intakeKcal > 0 || it.exerciseKcal > 0 },
        )
    }

    val state: StateFlow<AchievementUiState> = snapshot
        .map { data ->
            val computed = data.progress.filter { it.unlocked }.map { it.code }.toSet()
            AchievementUiState(
                progress = data.progress,
                unlocked = data.storedUnlocked + computed,
                daysConsidered = data.daysConsidered,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AchievementUiState())

    init {
        viewModelScope.launch {
            snapshot.collect { data ->
                AchievementEvaluator.newlyUnlocked(data.progress, data.storedUnlocked).forEach { progress ->
                    achievementRepository.unlock(progress.code, progress.currentValue, System.currentTimeMillis())
                }
            }
        }
    }
}

private fun List<DayTotals>.lastNotNullWeight(): Double? = lastOrNull { it.weightKg != null }?.weightKg
