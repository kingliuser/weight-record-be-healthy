package com.health.tracker.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 按天聚合后的一天，成就规则只读这个视图，不关心数据来自哪张表。 */
data class DayStats(
    val date: LocalDate,
    val intakeKcal: Double,
    val exerciseKcal: Double,
    val tdeeKcal: Double,
    val exerciseTargetKcal: Double,
    val weightKg: Double? = null,
) {
    val totalBurnKcal: Double get() = tdeeKcal + exerciseKcal
    val netKcal: Double get() = totalBurnKcal - intakeKcal
}

enum class AchievementFamily(val label: String) {
    CONTINUOUS_LOSS("持续减重"),
    MILESTONE("阶段减重"),
    INTAKE_DISCIPLINE("坚持控卡"),
    BURN_SURPLUS("消耗超额"),
}

enum class AchievementCode(val title: String, val description: String, val family: AchievementFamily) {
    WEIGHT_DROP_STREAK(
        "连掉七天",
        "体重连续 7 天逐日下降",
        AchievementFamily.CONTINUOUS_LOSS,
    ),
    LOSE_5_PERCENT(
        "首个 5%",
        "相对起始体重累计减掉 5%",
        AchievementFamily.MILESTONE,
    ),
    LOSE_10_PERCENT(
        "减重 10%",
        "相对起始体重累计减掉 10%",
        AchievementFamily.MILESTONE,
    ),
    INTAKE_BELOW_BURN_STREAK(
        "摄入低于消耗 7 天",
        "连续 7 天总摄入低于总消耗",
        AchievementFamily.INTAKE_DISCIPLINE,
    ),
    EXERCISE_TARGET_STREAK(
        "运动达标 7 天",
        "连续 7 天运动消耗达到每日推荐下限",
        AchievementFamily.BURN_SURPLUS,
    ),
}

data class Achievement(
    val code: AchievementCode,
    val targetValue: Double,
)

data class AchievementProgress(
    val code: AchievementCode,
    val currentValue: Double,
    val targetValue: Double,
) {
    val unlocked: Boolean get() = currentValue >= targetValue
    val ratio: Double get() = if (targetValue <= 0.0) 1.0 else (currentValue / targetValue).coerceIn(0.0, 1.0)
}

object AchievementCatalog {
    const val STREAK_TARGET_DAYS: Int = 7
    const val WEIGHT_DROP_STREAK_DAYS: Int = 7

    val ALL: List<Achievement> = listOf(
        Achievement(AchievementCode.WEIGHT_DROP_STREAK, WEIGHT_DROP_STREAK_DAYS.toDouble()),
        Achievement(AchievementCode.LOSE_5_PERCENT, 5.0),
        Achievement(AchievementCode.LOSE_10_PERCENT, 10.0),
        Achievement(AchievementCode.INTAKE_BELOW_BURN_STREAK, STREAK_TARGET_DAYS.toDouble()),
        Achievement(AchievementCode.EXERCISE_TARGET_STREAK, STREAK_TARGET_DAYS.toDouble()),
    )

    fun byCode(code: AchievementCode): Achievement = ALL.first { it.code == code }
}

object AchievementEvaluator {

    /** days 必须按日期升序；缺记录的日子直接缺席，连串会被日期断档打断。 */
    fun evaluate(days: List<DayStats>): List<AchievementProgress> {
        val sorted = days.sortedBy { it.date }
        val baseline = sorted.firstNotNullOfOrNull { it.weightKg }
        val latest = sorted.lastNotNullWeight()

        val lossPercent = if (baseline == null || latest == null || baseline <= 0.0) {
            0.0
        } else {
            ((baseline - latest) / baseline * 100.0).coerceAtLeast(0.0)
        }

        val progress = mapOf(
            AchievementCode.WEIGHT_DROP_STREAK to longestWeightDropStreak(sorted),
            AchievementCode.LOSE_5_PERCENT to lossPercent,
            AchievementCode.LOSE_10_PERCENT to lossPercent,
            AchievementCode.INTAKE_BELOW_BURN_STREAK to longestStreak(sorted) { it.netKcal > 0.0 },
            AchievementCode.EXERCISE_TARGET_STREAK to longestStreak(sorted) {
                it.exerciseTargetKcal > 0.0 && it.exerciseKcal >= it.exerciseTargetKcal
            },
        )

        return AchievementCatalog.ALL.map { achievement ->
            AchievementProgress(
                code = achievement.code,
                currentValue = progress.getValue(achievement.code),
                targetValue = achievement.targetValue,
            )
        }
    }

    fun newlyUnlocked(
        progress: List<AchievementProgress>,
        alreadyUnlocked: Set<AchievementCode>,
    ): List<AchievementProgress> = progress.filter { it.unlocked && it.code !in alreadyUnlocked }

    private fun List<DayStats>.lastNotNullWeight(): Double? = lastOrNull { it.weightKg != null }?.weightKg

    private fun longestWeightDropStreak(days: List<DayStats>): Double {
        var best = 0
        var run = 0
        var previousDate: LocalDate? = null
        var previousWeight: Double? = null
        for (day in days) {
            val weight = day.weightKg
            // 缺称重的那天直接断串，"连续下降"要求每天都上秤。
            if (weight == null) {
                run = 0
                previousDate = null
                previousWeight = null
                continue
            }
            val continues = previousWeight != null &&
                previousDate != null &&
                weight < previousWeight &&
                ChronoUnit.DAYS.between(previousDate, day.date) == 1L
            run = if (continues) run + 1 else 1
            if (run > best) best = run
            previousDate = day.date
            previousWeight = weight
        }
        return best.toDouble()
    }

    private fun longestStreak(days: List<DayStats>, predicate: (DayStats) -> Boolean): Double {
        var best = 0
        var run = 0
        var previousDate: LocalDate? = null
        for (day in days) {
            val qualifies = predicate(day) &&
                (previousDate == null || ChronoUnit.DAYS.between(previousDate, day.date) == 1L)
            run = if (qualifies) run + 1 else if (predicate(day)) 1 else 0
            if (run > best) best = run
            previousDate = day.date
        }
        return best.toDouble()
    }
}
