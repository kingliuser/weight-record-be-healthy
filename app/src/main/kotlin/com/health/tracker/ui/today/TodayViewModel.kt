package com.health.tracker.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.tracker.domain.DailyTarget
import com.health.tracker.domain.ExerciseEntry
import com.health.tracker.domain.ExerciseRepository
import com.health.tracker.domain.FoodEntry
import com.health.tracker.domain.FoodRepository
import com.health.tracker.domain.MealType
import com.health.tracker.domain.ProfileRepository
import com.health.tracker.domain.TargetRecommender
import com.health.tracker.domain.WeightRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    val loading: Boolean = true,
    val target: DailyTarget? = null,
    val weightKg: Double? = null,
    val foods: List<FoodEntry> = emptyList(),
    val exercises: List<ExerciseEntry> = emptyList(),
    val today: LocalDate = LocalDate.now(),
) {
    val intakeKcal: Double get() = foods.sumOf { it.kcal }
    val exerciseKcal: Double get() = exercises.sumOf { it.kcal }
    val intakeRemainingKcal: Double get() = (target?.intakeCapKcal ?: 0.0) - intakeKcal
    val exerciseGapKcal: Double get() = (target?.exerciseFloorKcal ?: 0.0) - exerciseKcal
}

class TodayViewModel(
    private val profileRepository: ProfileRepository,
    private val weightRepository: WeightRepository,
    private val foodRepository: FoodRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val today = LocalDate.now()

    val state: StateFlow<TodayUiState> = combine(
        profileRepository.observe(),
        weightRepository.observeDaily(),
        foodRepository.observeForDay(today),
        exerciseRepository.observeForDay(today),
    ) { profile, weights, foods, exercises ->
        val latestWeight = weights.lastOrNull()?.weightKg
        val target = if (profile != null && latestWeight != null) {
            TargetRecommender.recommend(profile, latestWeight, today)
        } else {
            null
        }
        TodayUiState(
            loading = false,
            target = target,
            weightKg = latestWeight,
            foods = foods,
            exercises = exercises,
            today = today,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState())

    fun addFood(name: String, kcalText: String, mealType: MealType) {
        val kcal = kcalText.toDoubleOrNull()
        if (name.isBlank() || kcal == null || kcal <= 0.0) return
        viewModelScope.launch { foodRepository.add(today, name.trim(), kcal, mealType) }
    }

    fun addExercise(name: String, kcalText: String, minutesText: String) {
        val kcal = kcalText.toDoubleOrNull()
        val minutes = minutesText.toIntOrNull()
        if (name.isBlank() || kcal == null || kcal <= 0.0 || minutes == null || minutes <= 0) return
        viewModelScope.launch { exerciseRepository.add(today, name.trim(), kcal, minutes) }
    }

    fun deleteFood(id: Long) = viewModelScope.launch { foodRepository.delete(id) }

    fun deleteExercise(id: Long) = viewModelScope.launch { exerciseRepository.delete(id) }
}
