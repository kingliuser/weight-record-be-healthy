package com.health.tracker.domain

import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class AchievementEvaluatorTest {

    private val start = LocalDate.of(2026, 6, 1)

    private fun day(
        index: Int,
        intakeKcal: Double = 0.0,
        exerciseKcal: Double = 0.0,
        tdeeKcal: Double = 2000.0,
        exerciseTargetKcal: Double = 0.0,
        weightKg: Double? = null,
    ) = DayStats(
        date = start.plusDays(index.toLong()),
        intakeKcal = intakeKcal,
        exerciseKcal = exerciseKcal,
        tdeeKcal = tdeeKcal,
        exerciseTargetKcal = exerciseTargetKcal,
        weightKg = weightKg,
    )

    private fun progressOf(days: List<DayStats>, code: AchievementCode) =
        AchievementEvaluator.evaluate(days).first { it.code == code }

    @Test
    fun `连续七天体重下降解锁连掉七天`() {
        val days = (0..6).map { day(it, weightKg = 72.0 - it * 0.5) }
        val progress = progressOf(days, AchievementCode.WEIGHT_DROP_STREAK)
        assertEquals(7.0, progress.currentValue, 0.001)
        assertTrue(progress.unlocked)
    }

    @Test
    fun `体重下降串断一天就归零重数`() {
        val weights = listOf(72.0, 71.5, 71.0, 70.5, 70.0, 69.5, 73.0)
        val days = weights.mapIndexed { index, weight -> day(index, weightKg = weight) }
        assertEquals(6.0, progressOf(days, AchievementCode.WEIGHT_DROP_STREAK).currentValue, 0.001)
    }

    @Test
    fun `缺称重记录的日子打断连续下降`() {
        val days = listOf(
            day(0, weightKg = 72.0),
            day(1, weightKg = 71.0),
            day(2),
            day(3, weightKg = 70.0),
            day(4, weightKg = 69.0),
        )
        assertEquals(2.0, progressOf(days, AchievementCode.WEIGHT_DROP_STREAK).currentValue, 0.001)
    }

    @Test
    fun `相对起始体重减百分之五解锁里程碑`() {
        val days = listOf(
            day(0, weightKg = 80.0),
            day(1, weightKg = 78.0),
            day(2, weightKg = 76.0),
        )
        assertTrue(progressOf(days, AchievementCode.LOSE_5_PERCENT).unlocked)
        assertFalse(progressOf(days, AchievementCode.LOSE_10_PERCENT).unlocked)
        assertEquals(5.0, progressOf(days, AchievementCode.LOSE_5_PERCENT).currentValue, 0.001)
    }

    @Test
    fun `体重回升不产生负进度`() {
        val days = listOf(day(0, weightKg = 80.0), day(1, weightKg = 84.0))
        assertEquals(0.0, progressOf(days, AchievementCode.LOSE_5_PERCENT).currentValue, 0.001)
    }

    @Test
    fun `没有任何体重记录时进度为零`() {
        val days = listOf(day(0, intakeKcal = 1000.0), day(1, intakeKcal = 1000.0))
        assertEquals(0.0, progressOf(days, AchievementCode.LOSE_5_PERCENT).currentValue, 0.001)
        assertEquals(0.0, progressOf(days, AchievementCode.WEIGHT_DROP_STREAK).currentValue, 0.001)
    }

    @Test
    fun `摄入低于消耗连续七天达标`() {
        val days = (0..6).map { day(it, intakeKcal = 1600.0, exerciseKcal = 100.0) }
        val progress = progressOf(days, AchievementCode.INTAKE_BELOW_BURN_STREAK)
        assertEquals(7.0, progress.currentValue, 0.001)
        assertTrue(progress.unlocked)
    }

    @Test
    fun `日期断档会打断控卡串`() {
        val days = (0..6).filter { it != 3 }.map { day(it, intakeKcal = 1600.0) }
        assertEquals(3.0, progressOf(days, AchievementCode.INTAKE_BELOW_BURN_STREAK).currentValue, 0.001)
    }

    @Test
    fun `摄入超标那天串归零`() {
        val days = (0..7).map { day(it, intakeKcal = if (it == 4) 4000.0 else 1600.0) }
        assertEquals(4.0, progressOf(days, AchievementCode.INTAKE_BELOW_BURN_STREAK).currentValue, 0.001)
    }

    @Test
    fun `运动达标串要求每天都吃到推荐下限`() {
        val days = (0..6).map { day(it, exerciseKcal = 165.0, exerciseTargetKcal = 165.0) }
        assertTrue(progressOf(days, AchievementCode.EXERCISE_TARGET_STREAK).unlocked)

        val shortDays = (0..6).map { day(it, exerciseKcal = if (it == 2) 100.0 else 165.0, exerciseTargetKcal = 165.0) }
        assertEquals(4.0, progressOf(shortDays, AchievementCode.EXERCISE_TARGET_STREAK).currentValue, 0.001)
    }

    @Test
    fun `没有运动下限的目标不该白送超额成就`() {
        val days = (0..6).map { day(it, exerciseKcal = 0.0, exerciseTargetKcal = 0.0) }
        assertFalse(progressOf(days, AchievementCode.EXERCISE_TARGET_STREAK).unlocked)
    }

    @Test
    fun `新解锁只返回达成且未记录过的成就`() {
        val days = (0..6).map { day(it, intakeKcal = 1600.0, weightKg = 80.0 - it) }
        val all = AchievementEvaluator.evaluate(days)
        val unlocked = all.filter { it.unlocked }.map { it.code }.toSet()
        val fresh = AchievementEvaluator.newlyUnlocked(all, setOf(AchievementCode.WEIGHT_DROP_STREAK))

        assertTrue(AchievementCode.WEIGHT_DROP_STREAK in unlocked)
        assertFalse(AchievementCode.WEIGHT_DROP_STREAK in fresh.map { it.code }.toSet())
        assertTrue(fresh.all { it.unlocked })
    }

    @Test
    fun `每个成就都有目录定义和进度输出`() {
        val progress = AchievementEvaluator.evaluate(emptyList())
        assertEquals(AchievementCatalog.ALL.size, progress.size)
        progress.forEach { assertEquals(0.0, it.ratio, 0.001) }
    }
}
