package com.health.tracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.tracker.domain.ActivityLevel
import com.health.tracker.domain.DailyTarget
import com.health.tracker.domain.Profile
import com.health.tracker.domain.ProfileRepository
import com.health.tracker.domain.Sex
import com.health.tracker.domain.TargetRecommender
import com.health.tracker.domain.WeightRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val heightCm: String = "",
    val weightKg: String = "",
    val goalWeightKg: String = "",
    val sex: Sex = Sex.FEMALE,
    val birthYear: String = "",
    val activityLevel: ActivityLevel = ActivityLevel.LIGHT,
    val weeklyLossKg: String = "0.5",
    val loading: Boolean = true,
    val error: String? = null,
    val saved: Boolean = false,
) {
    val previewTarget: DailyTarget?
        get() = buildProfileAndWeight()?.let { (profile, weight) ->
            TargetRecommender.recommend(profile, weight, LocalDate.now())
        }

    internal fun buildProfileAndWeight(): Pair<Profile, Double>? {
        val height = heightCm.toDoubleOrNull() ?: return null
        val weight = weightKg.toDoubleOrNull() ?: return null
        val year = birthYear.toIntOrNull() ?: return null
        val weekly = weeklyLossKg.toDoubleOrNull() ?: return null
        if (weight <= 0.0) return null
        return Profile(
            heightCm = height,
            sex = sex,
            birthYear = year,
            activityLevel = activityLevel,
            targetWeeklyLossKg = weekly,
            goalWeightKg = goalWeightKg.toDoubleOrNull(),
        ) to weight
    }
}

class SettingsViewModel(
    private val profileRepository: ProfileRepository,
    private val weightRepository: WeightRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = profileRepository.current()
            val latestWeight = weightRepository.latest()?.weightKg
            _state.update { current ->
                SettingsUiState(
                    heightCm = profile?.heightCm?.formatted() ?: current.heightCm,
                    weightKg = latestWeight?.formatted() ?: current.weightKg,
                    goalWeightKg = profile?.goalWeightKg?.formatted() ?: current.goalWeightKg,
                    sex = profile?.sex ?: current.sex,
                    birthYear = profile?.birthYear?.toString() ?: current.birthYear,
                    activityLevel = profile?.activityLevel ?: current.activityLevel,
                    weeklyLossKg = profile?.targetWeeklyLossKg?.formatted() ?: current.weeklyLossKg,
                    loading = false,
                )
            }
        }
    }

    fun onHeightChange(value: String) = update { copy(heightCm = value) }
    fun onWeightChange(value: String) = update { copy(weightKg = value) }
    fun onGoalWeightChange(value: String) = update { copy(goalWeightKg = value) }
    fun onBirthYearChange(value: String) = update { copy(birthYear = value) }
    fun onWeeklyLossChange(value: String) = update { copy(weeklyLossKg = value) }
    fun onSexChange(value: Sex) = update { copy(sex = value) }
    fun onActivityLevelChange(value: ActivityLevel) = update { copy(activityLevel = value) }

    fun save() {
        val snapshot = _state.value
        val (profile, weight) = snapshot.buildProfileAndWeight() ?: run {
            _state.update { it.copy(error = "身高、当前体重、出生年份、每周减重都要填对数字。") }
            return
        }
        if (profile.heightCm !in 80.0..250.0) {
            _state.update { it.copy(error = "身高看起来不在合理范围（80-250 cm）。") }
            return
        }
        if (weight !in 20.0..400.0) {
            _state.update { it.copy(error = "体重看起来不在合理范围（20-400 kg）。") }
            return
        }
        val thisYear = LocalDate.now().year
        if (profile.birthYear !in (thisYear - 120)..thisYear) {
            _state.update { it.copy(error = "出生年份看起来不对。") }
            return
        }
        if (profile.targetWeeklyLossKg !in 0.0..3.0) {
            _state.update { it.copy(error = "每周减重请填 0 到 3 kg 之间的数。") }
            return
        }

        viewModelScope.launch {
            profileRepository.save(profile)
            weightRepository.record(LocalDate.now(), weight)
            _state.update { it.copy(saved = true, error = null, loading = false) }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    private fun update(transform: SettingsUiState.() -> SettingsUiState) = _state.update(transform)

    private fun Double.formatted(): String = if (this == toLong().toDouble()) toLong().toString() else toString()
}
