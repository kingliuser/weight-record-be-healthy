package com.health.tracker.domain

import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class DailyTargetTest {

    private val on = LocalDate.of(2026, 6, 1)

    private fun profile(
        heightCm: Double,
        sex: Sex,
        birthYear: Int,
        level: ActivityLevel,
        weeklyLossKg: Double,
    ) = Profile(
        heightCm = heightCm,
        sex = sex,
        birthYear = birthYear,
        activityLevel = level,
        targetWeeklyLossKg = weeklyLossKg,
        goalWeightKg = null,
    )

    @Test
    fun `标准男性按七成饮食缺口一成多运动缺口拆分`() {
        val target = TargetRecommender.recommend(
            profile(175.0, Sex.MALE, 1996, ActivityLevel.LIGHT, 0.5),
            weightKg = 90.0,
            on = on,
        )
        assertEquals(1848.75, target.bmrKcal, 0.01)
        assertEquals(2542.03, target.tdeeKcal, 0.01)
        assertEquals(2157.03, target.intakeCapKcal, 0.01)
        assertEquals(165.0, target.exerciseFloorKcal, 0.01)
        assertEquals(emptySet(), target.warnings)
    }

    @Test
    fun `摄入下限护栏把缺口推给运动`() {
        val target = TargetRecommender.recommend(
            profile(160.0, Sex.FEMALE, 1998, ActivityLevel.SEDENTARY, 0.5),
            weightKg = 55.0,
            on = on,
        )
        assertEquals(1249.0, target.bmrKcal, 0.01)
        assertEquals(1498.8, target.tdeeKcal, 0.01)
        assertEquals(1200.0, target.intakeCapKcal, 0.01)
        assertEquals(298.8, target.tdeeKcal - target.intakeCapKcal, 0.01)
        assertEquals(251.2, target.exerciseFloorKcal, 0.01)
        assertTrue(TargetWarning.INTAKE_FLOOR_BINDS in target.warnings)
    }

    @Test
    fun `每周减重目标封顶 1kg`() {
        val target = TargetRecommender.recommend(
            profile(175.0, Sex.MALE, 1996, ActivityLevel.MODERATE, 2.0),
            weightKg = 90.0,
            on = on,
        )
        assertEquals(1.0, target.plannedWeeklyLossKg, 0.001)
        assertEquals(1100.0, target.netDailyDeficitKcal, 0.01)
        assertTrue(TargetWarning.WEEKLY_TARGET_REDUCED in target.warnings)
    }

    @Test
    fun `偏瘦时不给减重建议`() {
        val target = TargetRecommender.recommend(
            profile(175.0, Sex.MALE, 1996, ActivityLevel.LIGHT, 0.5),
            weightKg = 50.0,
            on = on,
        )
        assertEquals(0.0, target.plannedWeeklyLossKg, 0.001)
        assertEquals(0.0, target.exerciseFloorKcal, 0.001)
        assertEquals(target.tdeeKcal, target.intakeCapKcal, 0.01)
        assertEquals(setOf(TargetWarning.UNDERWEIGHT), target.warnings)
    }

    @Test
    fun `两个目标相加等于当日总缺口`() {
        val target = TargetRecommender.recommend(
            profile(178.0, Sex.MALE, 1990, ActivityLevel.MODERATE, 0.75),
            weightKg = 95.0,
            on = on,
        )
        val dietDeficit = target.tdeeKcal - target.intakeCapKcal
        assertEquals(target.netDailyDeficitKcal, dietDeficit + target.exerciseFloorKcal, 0.01)
    }
}
