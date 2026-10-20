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
    val mealCount: Int = 0,
    val activityCount: Int = 0,
    val exerciseMinutes: Int = 0,
) {
    val totalBurnKcal: Double get() = tdeeKcal + exerciseKcal
    val netKcal: Double get() = totalBurnKcal - intakeKcal

    /** 这天到底动没动手，用来判"有没有坚持记录"。 */
    val logged: Boolean
        get() = mealCount > 0 || activityCount > 0 || weightKg != null || intakeKcal > 0 || exerciseKcal > 0
}

enum class AchievementFamily(val label: String) {
    START("迈出第一步"),
    CONSISTENCY("坚持记录"),
    CONTINUOUS_LOSS("持续减重"),
    MILESTONE("阶段减重"),
    INTAKE_DISCIPLINE("坚持控卡"),
    BURN_SURPLUS("运动消耗"),
}

/**
 * 成就定义。[targetValue] 和 [unit] 直接挂在枚举上，目录和进度渲染都从这里取，
 * 新增一个成就只要在这里加一行、再在 evaluator 里给一条规则。
 */
enum class AchievementCode(
    val title: String,
    val description: String,
    val family: AchievementFamily,
    val unit: String,
    val targetValue: Double,
) {
    FIRST_MEAL_LOG("第一笔饮食", "记下第一条吃了什么", AchievementFamily.START, "条", 1.0),
    FIRST_ACTIVITY_LOG("第一次开练", "记下第一条运动", AchievementFamily.START, "条", 1.0),
    FIRST_WEIGHT_LOG("站上秤", "完成第一次称重", AchievementFamily.START, "次", 1.0),
    FIRST_DAY_LOGGED("第一天开张", "有记录的第一天", AchievementFamily.START, "天", 1.0),

    RECORD_STREAK_7("连续记录 7 天", "连续 7 天每天都有记录", AchievementFamily.CONSISTENCY, "天", 7.0),
    RECORD_STREAK_30("一个月不断档", "连续 30 天每天都有记录", AchievementFamily.CONSISTENCY, "天", 30.0),
    RECORD_DAYS_100("累计一百天", "累计 100 天留下过记录", AchievementFamily.CONSISTENCY, "天", 100.0),
    DIET_LOG_STREAK_7("每天记吃 7 天", "连续 7 天都记了饮食", AchievementFamily.CONSISTENCY, "天", 7.0),

    WEIGHT_DROP_STREAK("连掉七天", "体重连续 7 天逐日下降", AchievementFamily.CONTINUOUS_LOSS, "天", 7.0),
    WEIGHT_DROP_STREAK_14("连掉十四天", "体重连续 14 天逐日下降", AchievementFamily.CONTINUOUS_LOSS, "天", 14.0),
    WEEKLY_LOSS_5_DAYS("五天下秤", "连续 5 天体重逐日下降", AchievementFamily.CONTINUOUS_LOSS, "天", 5.0),

    LOSE_1_KG("掉秤一公斤", "相对起始体重减掉 1 kg", AchievementFamily.MILESTONE, "kg", 1.0),
    LOSE_5_PERCENT("首个 5%", "相对起始体重减掉 5%", AchievementFamily.MILESTONE, "%", 5.0),
    LOSE_10_PERCENT("减重 10%", "相对起始体重减掉 10%", AchievementFamily.MILESTONE, "%", 10.0),
    LOSE_5_KG("甩掉五公斤", "相对起始体重减掉 5 kg", AchievementFamily.MILESTONE, "kg", 5.0),
    LOSE_15_PERCENT("减重 15%", "相对起始体重减掉 15%", AchievementFamily.MILESTONE, "%", 15.0),

    INTAKE_BELOW_BURN_STREAK("摄入低于消耗 7 天", "连续 7 天总摄入低于总消耗", AchievementFamily.INTAKE_DISCIPLINE, "天", 7.0),
    INTAKE_BELOW_BURN_STREAK_21("三周热量缺口", "连续 21 天总摄入低于总消耗", AchievementFamily.INTAKE_DISCIPLINE, "天", 21.0),
    DEFICIT_7700_KCAL("攒出一公斤缺口", "累计热量缺口满 7700 kcal", AchievementFamily.INTAKE_DISCIPLINE, "kcal", 7700.0),

    EXERCISE_STREAK_5("连续 5 天有运动", "连续 5 天都记了运动", AchievementFamily.BURN_SURPLUS, "天", 5.0),
    EXERCISE_TARGET_STREAK("运动达标 7 天", "连续 7 天运动消耗达到每日推荐下限", AchievementFamily.BURN_SURPLUS, "天", 7.0),
    EXERCISE_MINUTES_600("累计运动 600 分钟", "累计记满 600 分钟运动", AchievementFamily.BURN_SURPLUS, "分钟", 600.0),
    EXERCISE_KCAL_5000("累计消耗 5000 kcal", "累计运动消耗满 5000 kcal", AchievementFamily.BURN_SURPLUS, "kcal", 5000.0),
}

data class AchievementProgress(
    val code: AchievementCode,
    val currentValue: Double,
    val targetValue: Double,
) {
    val unlocked: Boolean get() = currentValue >= targetValue
    val ratio: Double get() = if (targetValue <= 0.0) 1.0 else (currentValue / targetValue).coerceIn(0.0, 1.0)
    val remaining: Double get() = (targetValue - currentValue).coerceAtLeast(0.0)
}

object AchievementCatalog {
    val ALL: List<AchievementCode> = AchievementCode.entries.toList()
}

object AchievementEvaluator {

    /** days 必须按日期升序；缺记录的日子直接缺席，连串会被日期断档打断。 */
    fun evaluate(days: List<DayStats>): List<AchievementProgress> {
        val sorted = days.sortedBy { it.date }
        val weights = sorted.mapNotNull { it.weightKg }
        val baseline = weights.firstOrNull()
        val lowest = weights.minOrNull()
        // 里程碑按历史最低体重算，反弹不会把已经拿到手的成就打掉。
        val bestLossKg = if (baseline == null || lowest == null) 0.0 else (baseline - lowest).coerceAtLeast(0.0)
        val bestLossPercent = if (baseline == null || baseline <= 0.0) 0.0 else bestLossKg / baseline * 100.0

        val progress = mapOf(
            AchievementCode.FIRST_MEAL_LOG to sorted.count { it.mealCount > 0 }.toDouble(),
            AchievementCode.FIRST_ACTIVITY_LOG to sorted.count { it.activityCount > 0 }.toDouble(),
            AchievementCode.FIRST_WEIGHT_LOG to weights.size.toDouble(),
            AchievementCode.FIRST_DAY_LOGGED to sorted.count { it.logged }.toDouble(),

            AchievementCode.RECORD_STREAK_7 to longestStreak(sorted) { it.logged },
            AchievementCode.RECORD_STREAK_30 to longestStreak(sorted) { it.logged },
            AchievementCode.RECORD_DAYS_100 to sorted.count { it.logged }.toDouble(),
            AchievementCode.DIET_LOG_STREAK_7 to longestStreak(sorted) { it.mealCount > 0 },

            AchievementCode.WEIGHT_DROP_STREAK to longestWeightDropStreak(sorted),
            AchievementCode.WEIGHT_DROP_STREAK_14 to longestWeightDropStreak(sorted),
            AchievementCode.WEEKLY_LOSS_5_DAYS to longestWeightDropStreak(sorted),

            AchievementCode.LOSE_1_KG to bestLossKg,
            AchievementCode.LOSE_5_PERCENT to bestLossPercent,
            AchievementCode.LOSE_10_PERCENT to bestLossPercent,
            AchievementCode.LOSE_5_KG to bestLossKg,
            AchievementCode.LOSE_15_PERCENT to bestLossPercent,

            AchievementCode.INTAKE_BELOW_BURN_STREAK to longestStreak(sorted) { it.netKcal > 0.0 },
            AchievementCode.INTAKE_BELOW_BURN_STREAK_21 to longestStreak(sorted) { it.netKcal > 0.0 },
            AchievementCode.DEFICIT_7700_KCAL to sorted.sumOf { if (it.netKcal > 0.0) it.netKcal else 0.0 },

            AchievementCode.EXERCISE_STREAK_5 to longestStreak(sorted) { it.activityCount > 0 },
            AchievementCode.EXERCISE_TARGET_STREAK to longestStreak(sorted) {
                it.exerciseTargetKcal > 0.0 && it.exerciseKcal >= it.exerciseTargetKcal
            },
            AchievementCode.EXERCISE_MINUTES_600 to sorted.sumOf { it.exerciseMinutes }.toDouble(),
            AchievementCode.EXERCISE_KCAL_5000 to sorted.sumOf { it.exerciseKcal },
        )

        return AchievementCatalog.ALL.map { code ->
            AchievementProgress(
                code = code,
                currentValue = progress.getValue(code),
                targetValue = code.targetValue,
            )
        }
    }

    fun newlyUnlocked(
        progress: List<AchievementProgress>,
        alreadyUnlocked: Set<AchievementCode>,
    ): List<AchievementProgress> = progress.filter { it.unlocked && it.code !in alreadyUnlocked }

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
