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
        mealCount: Int = 0,
        activityCount: Int = 0,
        exerciseMinutes: Int = 0,
    ) = DayStats(
        date = start.plusDays(index.toLong()),
        intakeKcal = intakeKcal,
        exerciseKcal = exerciseKcal,
        tdeeKcal = tdeeKcal,
        exerciseTargetKcal = exerciseTargetKcal,
        weightKg = weightKg,
        mealCount = mealCount,
        activityCount = activityCount,
        exerciseMinutes = exerciseMinutes,
    )

    private fun progressOf(days: List<DayStats>, code: AchievementCode) =
        AchievementEvaluator.evaluate(days).first { it.code == code }

    private fun valueOf(days: List<DayStats>, code: AchievementCode) =
        progressOf(days, code).currentValue

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
        assertEquals(6.0, valueOf(days, AchievementCode.WEIGHT_DROP_STREAK), 0.001)
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
        assertEquals(2.0, valueOf(days, AchievementCode.WEIGHT_DROP_STREAK), 0.001)
    }

    @Test
    fun `连掉十四天要真的连续十四天下秤`() {
        val seven = (0..6).map { day(it, weightKg = 75.0 - it) }
        assertFalse(progressOf(seven, AchievementCode.WEIGHT_DROP_STREAK_14).unlocked)

        val fourteen = (0..13).map { day(it, weightKg = 80.0 - it) }
        assertTrue(progressOf(fourteen, AchievementCode.WEIGHT_DROP_STREAK_14).unlocked)
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
        assertEquals(5.0, valueOf(days, AchievementCode.LOSE_5_PERCENT), 0.001)
    }

    @Test
    fun `公斤里程碑和百分比里程碑各自独立`() {
        val days = listOf(day(0, weightKg = 100.0), day(1, weightKg = 95.0))
        assertTrue(progressOf(days, AchievementCode.LOSE_5_KG).unlocked)
        assertFalse(progressOf(days, AchievementCode.LOSE_15_PERCENT).unlocked)
        assertEquals(5.0, valueOf(days, AchievementCode.LOSE_1_KG), 0.001)

        val light = listOf(day(0, weightKg = 60.0), day(1, weightKg = 59.0))
        assertTrue(progressOf(light, AchievementCode.LOSE_1_KG).unlocked)
        assertFalse(progressOf(light, AchievementCode.LOSE_5_KG).unlocked)
    }

    @Test
    fun `体重回升不产生负进度`() {
        val days = listOf(day(0, weightKg = 80.0), day(1, weightKg = 84.0))
        assertEquals(0.0, valueOf(days, AchievementCode.LOSE_5_PERCENT), 0.001)
    }

    @Test
    fun `里程碑按历史最低算反弹不会打掉已到手的成就`() {
        val days = listOf(
            day(0, weightKg = 90.0),
            day(1, weightKg = 81.0),
            day(2, weightKg = 88.0),
        )
        assertEquals(9.0, valueOf(days, AchievementCode.LOSE_1_KG), 0.001)
        assertEquals(10.0, valueOf(days, AchievementCode.LOSE_10_PERCENT), 0.001)
    }

    @Test
    fun `没有任何体重记录时进度为零`() {
        val days = listOf(day(0, intakeKcal = 1000.0), day(1, intakeKcal = 1000.0))
        assertEquals(0.0, valueOf(days, AchievementCode.LOSE_5_PERCENT), 0.001)
        assertEquals(0.0, valueOf(days, AchievementCode.WEIGHT_DROP_STREAK), 0.001)
        assertEquals(0.0, valueOf(days, AchievementCode.FIRST_WEIGHT_LOG), 0.001)
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
        assertEquals(3.0, valueOf(days, AchievementCode.INTAKE_BELOW_BURN_STREAK), 0.001)
    }

    @Test
    fun `摄入超标那天串归零`() {
        val days = (0..7).map { day(it, intakeKcal = if (it == 4) 4000.0 else 1600.0) }
        assertEquals(4.0, valueOf(days, AchievementCode.INTAKE_BELOW_BURN_STREAK), 0.001)
    }

    @Test
    fun `三周热量缺口是同一串计数的高门槛版本`() {
        val twoWeeks = (0..13).map { day(it, intakeKcal = 1600.0) }
        assertFalse(progressOf(twoWeeks, AchievementCode.INTAKE_BELOW_BURN_STREAK_21).unlocked)

        val threeWeeks = (0..20).map { day(it, intakeKcal = 1600.0) }
        assertTrue(progressOf(threeWeeks, AchievementCode.INTAKE_BELOW_BURN_STREAK_21).unlocked)
    }

    @Test
    fun `累计缺口只加正数天`() {
        val days = listOf(
            day(0, intakeKcal = 1000.0),
            day(1, intakeKcal = 5000.0),
            day(2, intakeKcal = 1000.0),
        )
        // 2000-1000 各一天，超标那天不计入也不倒扣。
        assertEquals(2000.0, valueOf(days, AchievementCode.DEFICIT_7700_KCAL), 0.001)
    }

    @Test
    fun `运动达标串要求每天都吃到推荐下限`() {
        val days = (0..6).map { day(it, exerciseKcal = 165.0, exerciseTargetKcal = 165.0) }
        assertTrue(progressOf(days, AchievementCode.EXERCISE_TARGET_STREAK).unlocked)

        val shortDays = (0..6).map { day(it, exerciseKcal = if (it == 2) 100.0 else 165.0, exerciseTargetKcal = 165.0) }
        assertEquals(4.0, valueOf(shortDays, AchievementCode.EXERCISE_TARGET_STREAK), 0.001)
    }

    @Test
    fun `没有运动下限的目标不该白送超额成就`() {
        val days = (0..6).map { day(it, exerciseKcal = 0.0, exerciseTargetKcal = 0.0) }
        assertFalse(progressOf(days, AchievementCode.EXERCISE_TARGET_STREAK).unlocked)
    }

    @Test
    fun `第一次类成就只要出现过一条就达成`() {
        val days = listOf(day(0, mealCount = 1), day(1, activityCount = 2, exerciseMinutes = 30))
        assertTrue(progressOf(days, AchievementCode.FIRST_MEAL_LOG).unlocked)
        assertTrue(progressOf(days, AchievementCode.FIRST_ACTIVITY_LOG).unlocked)
        assertFalse(progressOf(days, AchievementCode.FIRST_WEIGHT_LOG).unlocked)
        assertEquals(2.0, valueOf(days, AchievementCode.FIRST_DAY_LOGGED), 0.001)
    }

    @Test
    fun `连续记录串看所有类型的记录`() {
        val days = (0..6).map { index ->
            when (index % 3) {
                0 -> day(index, mealCount = 1)
                1 -> day(index, exerciseKcal = 100.0, activityCount = 1)
                else -> day(index, weightKg = 70.0 - index * 0.1)
            }
        }
        assertTrue(progressOf(days, AchievementCode.RECORD_STREAK_7).unlocked)
        assertFalse(progressOf(days, AchievementCode.DIET_LOG_STREAK_7).unlocked)
    }

    @Test
    fun `累计一百天不等于连续`() {
        val days = (0 until 100 step 2).map { day(it, mealCount = 1) }
        assertEquals(50.0, valueOf(days, AchievementCode.RECORD_DAYS_100), 0.001)
        assertFalse(progressOf(days, AchievementCode.RECORD_DAYS_100).unlocked)

        val full = (0 until 100).map { day(it, mealCount = 1) }
        assertTrue(progressOf(full, AchievementCode.RECORD_DAYS_100).unlocked)
    }

    @Test
    fun `累计运动分钟和消耗`() {
        val days = (0 until 12).map { day(it, exerciseKcal = 450.0, activityCount = 1, exerciseMinutes = 50) }
        assertEquals(600.0, valueOf(days, AchievementCode.EXERCISE_MINUTES_600), 0.001)
        assertTrue(progressOf(days, AchievementCode.EXERCISE_MINUTES_600).unlocked)
        assertEquals(5400.0, valueOf(days, AchievementCode.EXERCISE_KCAL_5000), 0.001)
        assertTrue(progressOf(days, AchievementCode.EXERCISE_KCAL_5000).unlocked)
    }

    @Test
    fun `连续五天有运动按条数判定`() {
        val days = (0..4).map { day(it, activityCount = 1, exerciseMinutes = 10) }
        assertTrue(progressOf(days, AchievementCode.EXERCISE_STREAK_5).unlocked)

        val withGap = (0..5).filter { it != 2 }.map { day(it, activityCount = 1) }
        assertEquals(3.0, valueOf(withGap, AchievementCode.EXERCISE_STREAK_5), 0.001)
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

    @Test
    fun `目录每条都有单位正的门槛和文案`() {
        AchievementCatalog.ALL.forEach { code ->
            assertTrue(code.targetValue > 0.0, "${code.name} 门槛必须为正")
            assertTrue(code.unit.isNotBlank(), "${code.name} 必须有单位")
            assertTrue(code.title.isNotBlank() && code.description.isNotBlank(), "${code.name} 必须有文案")
        }
        assertTrue(AchievementCatalog.ALL.size >= 20)
    }

    @Test
    fun `进度比例和剩余量夹在合理区间`() {
        val over = AchievementProgress(AchievementCode.RECORD_STREAK_7, 40.0, 7.0)
        assertEquals(1.0, over.ratio, 0.001)
        assertEquals(0.0, over.remaining, 0.001)

        val under = AchievementProgress(AchievementCode.RECORD_STREAK_7, 2.0, 7.0)
        assertEquals(5.0, under.remaining, 0.001)
        assertFalse(under.unlocked)
    }
}
