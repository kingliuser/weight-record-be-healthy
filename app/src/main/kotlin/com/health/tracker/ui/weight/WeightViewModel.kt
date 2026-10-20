package com.health.tracker.ui.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.tracker.domain.ProfileRepository
import com.health.tracker.domain.WeightEntry
import com.health.tracker.domain.WeightRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WeightUiState(
    val entries: List<WeightEntry> = emptyList(),
    val goalWeightKg: Double? = null,
    val today: LocalDate = LocalDate.now(),
) {
    val startWeightKg: Double? get() = entries.firstOrNull()?.weightKg
    val latestWeightKg: Double? get() = entries.lastOrNull()?.weightKg
    val totalChangeKg: Double get() = (startWeightKg ?: 0.0) - (latestWeightKg ?: 0.0)
    val toGoalKg: Double? get() = latestWeightKg?.let { latest -> goalWeightKg?.let { latest - it } }

    /** 相对上一次记录的增减，正数表示涨了。 */
    fun deltaOf(index: Int): Double? {
        if (index <= 0) return null
        return entries[index].weightKg - entries[index - 1].weightKg
    }
}

class WeightViewModel(
    profileRepository: ProfileRepository,
    private val weightRepository: WeightRepository,
) : ViewModel() {

    private val today = LocalDate.now()

    val state: StateFlow<WeightUiState> = combine(
        profileRepository.observe(),
        weightRepository.observeDaily(),
    ) { profile, entries ->
        WeightUiState(entries = entries, goalWeightKg = profile?.goalWeightKg, today = today)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeightUiState())

    fun recordToday(weightText: String) {
        val weight = weightText.toDoubleOrNull()
        if (weight == null || weight !in 20.0..400.0) return
        viewModelScope.launch { weightRepository.record(today, weight) }
    }

    fun delete(id: Long) = viewModelScope.launch { weightRepository.delete(id) }
}
