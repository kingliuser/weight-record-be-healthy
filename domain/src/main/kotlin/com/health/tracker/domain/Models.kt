package com.health.tracker.domain

import java.time.LocalDate

enum class Sex {
    MALE,
    FEMALE,
}

enum class ActivityLevel(val label: String, val multiplier: Double) {
    SEDENTARY("久坐 · 基本不运动", 1.2),
    LIGHT("轻度 · 每周 1-3 天", 1.375),
    MODERATE("中度 · 每周 3-5 天", 1.55),
    ACTIVE("高度 · 每周 6-7 天", 1.725),
    VERY_ACTIVE("极高 · 体力劳动/训练", 1.9),
}

enum class MealType(val label: String) {
    BREAKFAST("早餐"),
    LUNCH("午餐"),
    DINNER("晚餐"),
    SNACK("加餐"),
}

data class Profile(
    val heightCm: Double,
    val sex: Sex,
    val birthYear: Int,
    val activityLevel: ActivityLevel,
    val targetWeeklyLossKg: Double,
    val goalWeightKg: Double?,
) {
    // 只采集出生年份，年龄按日历年差算，比真实年龄最多偏大 1 岁，对 BMR 影响 <5 kcal。
    fun ageOn(date: LocalDate): Int = (date.year - birthYear).coerceAtLeast(0)
}

data class WeightEntry(
    val id: Long,
    val date: LocalDate,
    val weightKg: Double,
)

data class FoodEntry(
    val id: Long,
    val date: LocalDate,
    val name: String,
    val kcal: Double,
    val mealType: MealType,
)

data class ExerciseEntry(
    val id: Long,
    val date: LocalDate,
    val name: String,
    val kcal: Double,
    val minutes: Int,
)
