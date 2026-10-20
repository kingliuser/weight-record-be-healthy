package com.health.tracker.domain

object EnergyMath {
    // 1 kg 体脂 ≈ 7700 kcal，热量差值与体重差值互转都走这个常数。
    const val KCAL_PER_KG_FAT: Double = 7700.0

    const val MIN_INTAKE_KCAL: Double = 1200.0
    const val MIN_INTAKE_TDEE_RATIO: Double = 0.8
    const val MAX_WEEKLY_LOSS_KG: Double = 1.0
    const val UNDERWEIGHT_BMI: Double = 18.5
    const val DIET_SHARE_OF_DEFICIT: Double = 0.7

    fun dailyDeficitKcal(weeklyLossKg: Double): Double = weeklyLossKg * KCAL_PER_KG_FAT / 7.0

    fun expectedLossKg(totalKcalDeficit: Double): Double = totalKcalDeficit / KCAL_PER_KG_FAT

    /** Mifflin-St Jeor，对超重人群比 Harris-Benedict 偏差更小。 */
    fun bmrKcal(weightKg: Double, heightCm: Double, ageYears: Int, sex: Sex): Double {
        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * ageYears
        return if (sex == Sex.MALE) base + 5.0 else base - 161.0
    }

    fun tdeeKcal(bmrKcal: Double, level: ActivityLevel): Double = bmrKcal * level.multiplier

    fun bmi(weightKg: Double, heightCm: Double): Double {
        val metres = heightCm / 100.0
        return if (metres <= 0.0) 0.0 else weightKg / (metres * metres)
    }
}
