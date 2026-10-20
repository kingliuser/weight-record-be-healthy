package com.health.tracker.ui.monthly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.tracker.domain.DayTotals
import com.health.tracker.domain.EnergyMath
import com.health.tracker.domain.LedgerRepository
import com.health.tracker.domain.ProfileRepository
import com.health.tracker.domain.TargetRecommender
import com.health.tracker.domain.WeightRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class MonthSummary(
    val month: YearMonth,
    val daysRecorded: Int,
    val intakeKcal: Double,
    val exerciseKcal: Double,
    val tdeeKcal: Double,
    val firstWeightKg: Double?,
    val lastWeightKg: Double?,
) {
    val estimatedBurnKcal: Double get() = tdeeKcal * daysRecorded + exerciseKcal
    val netDeficitKcal: Double get() = estimatedBurnKcal - intakeKcal
    val theoryLossKg: Double get() = EnergyMath.expectedLossKg(netDeficitKcal)
    val actualLossKg: Double?
        get() = if (firstWeightKg != null && lastWeightKg != null) firstWeightKg - lastWeightKg else null

    /** 热量算出来的应减和秤上的实差常常不一致，差得远说明记录漏了或有水分。 */
    val lossGapKg: Double? get() = actualLossKg?.let { it - theoryLossKg }
}

data class MonthlyUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val months: List<MonthSummary> = emptyList(),
    val days: List<DayTotals> = emptyList(),
) {
    val current: MonthSummary? get() = months.firstOrNull { it.month == selectedMonth }
    val canGoPrevious: Boolean get() = months.any { it.month < selectedMonth }
    val canGoNext: Boolean get() = months.any { it.month > selectedMonth }
}

class MonthlyViewModel(
    profileRepository: ProfileRepository,
    weightRepository: WeightRepository,
    ledgerRepository: LedgerRepository,
) : ViewModel() {

    private val today = LocalDate.now()
    private val windowFrom = today.minusDays(400)
    private val selectedMonthFlow = MutableStateFlow(YearMonth.now())

    private val selected: StateFlow<YearMonth> = selectedMonthFlow.asStateFlow()

    val state: StateFlow<MonthlyUiState> = combine(
        profileRepository.observe(),
        weightRepository.observeDaily(),
        ledgerRepository.observeDayTotals(windowFrom, today),
        selected,
    ) { profile, weights, totals, month ->
        val byMonth = totals.groupBy { YearMonth.from(it.date) }
        val months = byMonth.entries.sortedBy { it.key }.map { (yearMonth, days) ->
            val monthWeights = days.mapNotNull { it.weightKg }
            val referenceWeight = monthWeights.lastOrNull() ?: weights.lastOrNull()?.weightKg
            val tdee = if (profile != null && referenceWeight != null) {
                TargetRecommender.recommend(profile, referenceWeight, yearMonth.atEndOfMonth()).tdeeKcal
            } else {
                0.0
            }
            MonthSummary(
                month = yearMonth,
                daysRecorded = days.size,
                intakeKcal = days.sumOf { it.intakeKcal },
                exerciseKcal = days.sumOf { it.exerciseKcal },
                tdeeKcal = tdee,
                firstWeightKg = monthWeights.firstOrNull(),
                lastWeightKg = monthWeights.lastOrNull(),
            )
        }
        MonthlyUiState(
            selectedMonth = month,
            months = months,
            days = byMonth[month].orEmpty().sortedBy { it.date },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlyUiState())

    fun previousMonth() = move(-1)
    fun nextMonth() = move(1)

    private fun move(delta: Long) {
        val current = selected.value
        val candidate = current.plusMonths(delta)
        if (state.value.months.any { it.month == candidate }) selectedMonthFlow.value = candidate
    }
}
