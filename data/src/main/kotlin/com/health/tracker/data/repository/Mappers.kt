package com.health.tracker.data.repository

import com.health.tracker.data.local.MealEntryEntity
import com.health.tracker.data.local.ProfileEntity
import com.health.tracker.data.local.SINGLE_PROFILE_ID
import com.health.tracker.data.local.WeightLogEntity
import com.health.tracker.data.local.ActivityEntryEntity
import com.health.tracker.domain.ActivityLevel
import com.health.tracker.domain.ExerciseEntry
import com.health.tracker.domain.FoodEntry
import com.health.tracker.domain.MealType
import com.health.tracker.domain.Profile
import com.health.tracker.domain.Sex
import com.health.tracker.domain.WeightEntry
import java.time.LocalDate

internal fun Profile.toEntity(updatedAtMillis: Long) = ProfileEntity(
    id = SINGLE_PROFILE_ID,
    heightCm = heightCm,
    sex = sex.name,
    birthYear = birthYear,
    activityLevel = activityLevel.name,
    targetWeeklyLossKg = targetWeeklyLossKg,
    goalWeightKg = goalWeightKg,
    updatedAtMillis = updatedAtMillis,
)

internal fun ProfileEntity.toDomain() = Profile(
    heightCm = heightCm,
    sex = Sex.valueOf(sex),
    birthYear = birthYear,
    activityLevel = ActivityLevel.valueOf(activityLevel),
    targetWeeklyLossKg = targetWeeklyLossKg,
    goalWeightKg = goalWeightKg,
)

internal fun WeightLogEntity.toDomain() = WeightEntry(
    id = id,
    date = LocalDate.ofEpochDay(dateEpochDay),
    weightKg = weightKg,
)

internal fun MealEntryEntity.toDomain() = FoodEntry(
    id = id,
    date = LocalDate.ofEpochDay(dateEpochDay),
    name = name,
    kcal = kcal,
    mealType = MealType.valueOf(mealType),
)

internal fun ActivityEntryEntity.toDomain() = ExerciseEntry(
    id = id,
    date = LocalDate.ofEpochDay(dateEpochDay),
    name = name,
    kcal = kcal,
    minutes = minutes,
)
