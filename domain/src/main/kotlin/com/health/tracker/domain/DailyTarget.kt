package com.health.tracker.domain

import java.time.LocalDate

/** 一天里用户实际面对的两个数字：吃不超过 intakeCapKcal，练不少于 exerciseFloorKcal。 */
data class DailyTarget(
    val bmrKcal: Double,
    val tdeeKcal: Double,
    val intakeCapKcal: Double,
    val exerciseFloorKcal: Double,
    val plannedWeeklyLossKg: Double,
    val bmi: Double,
    val warnings: Set<TargetWarning>,
) {
    val netDailyDeficitKcal: Double
        get() = EnergyMath.dailyDeficitKcal(plannedWeeklyLossKg)
}

enum class TargetWarning(val message: String) {
    UNDERWEIGHT("BMI 低于 18.5，不建议减重，以下按维持体重计算。"),
    WEEKLY_TARGET_REDUCED("每周减重目标上限 1.0 kg，已按上限重算。"),
    INTAKE_FLOOR_BINDS("按饮食削减会跌破安全摄入下限，缺口已改由运动承担。"),
}

object TargetRecommender {

    fun recommend(profile: Profile, weightKg: Double, on: LocalDate): DailyTarget {
        val bmi = EnergyMath.bmi(weightKg, profile.heightCm)
        val bmr = EnergyMath.bmrKcal(weightKg, profile.heightCm, profile.ageOn(on), profile.sex)
        val tdee = EnergyMath.tdeeKcal(bmr, profile.activityLevel)

        if (bmi < EnergyMath.UNDERWEIGHT_BMI) {
            return DailyTarget(
                bmrKcal = bmr,
                tdeeKcal = tdee,
                intakeCapKcal = tdee,
                exerciseFloorKcal = 0.0,
                plannedWeeklyLossKg = 0.0,
                bmi = bmi,
                warnings = setOf(TargetWarning.UNDERWEIGHT),
            )
        }

        val warnings = mutableSetOf<TargetWarning>()
        val requested = profile.targetWeeklyLossKg
        val planned = requested.coerceIn(0.0, EnergyMath.MAX_WEEKLY_LOSS_KG)
        if (planned != requested) warnings += TargetWarning.WEEKLY_TARGET_REDUCED

        val totalDeficit = EnergyMath.dailyDeficitKcal(planned)
        val intakeFloor = maxOf(
            EnergyMath.MIN_INTAKE_KCAL,
            tdee * EnergyMath.MIN_INTAKE_TDEE_RATIO,
        )
        val maxDietDeficit = (tdee - intakeFloor).coerceAtLeast(0.0)
        val dietDeficit = (totalDeficit * EnergyMath.DIET_SHARE_OF_DEFICIT).coerceAtMost(maxDietDeficit)
        if (dietDeficit < totalDeficit * EnergyMath.DIET_SHARE_OF_DEFICIT - 1e-6) {
            warnings += TargetWarning.INTAKE_FLOOR_BINDS
        }

        return DailyTarget(
            bmrKcal = bmr,
            tdeeKcal = tdee,
            intakeCapKcal = tdee - dietDeficit,
            exerciseFloorKcal = totalDeficit - dietDeficit,
            plannedWeeklyLossKg = planned,
            bmi = bmi,
            warnings = warnings,
        )
    }
}
