package com.health.tracker.ui.achievement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.health.tracker.domain.AchievementCode
import com.health.tracker.domain.AchievementFamily
import com.health.tracker.domain.AchievementProgress
import com.health.tracker.ui.containerFactory
import com.health.tracker.ui.components.EmptyHint
import com.health.tracker.ui.components.GradientCard
import com.health.tracker.ui.components.SectionCard
import com.health.tracker.ui.countText
import com.health.tracker.ui.displayText
import com.health.tracker.ui.kcalText
import com.health.tracker.ui.kgNumberText
import com.health.tracker.ui.kgText
import com.health.tracker.ui.percentText
import com.health.tracker.ui.theme.HealthTheme
import java.time.LocalDate

@Composable
fun AchievementScreen(
    modifier: Modifier = Modifier,
    viewModel: AchievementViewModel = viewModel(
        factory = containerFactory {
            AchievementViewModel(
                it.profileRepository,
                it.weightRepository,
                it.ledgerRepository,
                it.achievementRepository,
            )
        },
    ),
) {
    val state by viewModel.state.collectAsState()
    AchievementContent(state = state, modifier = modifier)
}

@Composable
fun AchievementContent(
    state: AchievementUiState,
    modifier: Modifier = Modifier,
) {
    val grouped = state.progress.groupBy { it.code.family }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AchievementHero(state)

        if (state.daysConsidered == 0) {
            SectionCard(title = "还没有可计算的数据") {
                EmptyHint("先去今日页记几笔饮食或运动，或者在体重页称一次重，成就就会开始累计。")
            }
        }

        AchievementFamily.entries.forEach { family ->
            val items = grouped[family]
            if (!items.isNullOrEmpty()) {
                SectionCard(title = "${family.label} ${state.unlockedIn(family)}/${state.totalIn(family)}") {
                    state.ordered(items).forEach { progress ->
                        AchievementRow(
                            progress = progress,
                            unlocked = state.isUnlocked(progress.code),
                            unlockedDate = state.unlockedAt[progress.code],
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementHero(state: AchievementUiState) {
    GradientCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(color = Color.White.copy(alpha = 0.24f), shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(19.dp),
                )
            }
            Text(
                text = "成就墙",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
        }

        Text(
            text = "${state.unlockedCount} / ${state.total}",
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
        )
        Text(
            text = "已拿下 ${state.completionPercent.percentText()}，累计有记录 ${state.daysConsidered} 天",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.88f),
        )
        LinearProgressIndicator(
            progress = { state.completionRatio.toFloat() },
            modifier = Modifier.fillMaxWidth().height(10.dp),
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.26f),
            strokeCap = StrokeCap.Round,
        )

        state.latestUnlock?.let { moment ->
            UnlockBanner(
                text = "最近解锁 · ${moment.progress.code.title} · ${moment.date.displayText()}",
                background = Color.White.copy(alpha = 0.24f),
            )
        }
        state.closestLocked?.let { closest ->
            UnlockBanner(
                text = "还差 ${closest.remainingText()} 就能拿下「${closest.code.title}」",
                background = Color.White.copy(alpha = 0.14f),
            )
        }
    }
}

@Composable
private fun UnlockBanner(text: String, background: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .background(color = background, shape = MaterialTheme.shapes.medium)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun AchievementRow(
    progress: AchievementProgress,
    unlocked: Boolean,
    unlockedDate: LocalDate?,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(
                        color = if (unlocked) scheme.primaryContainer else scheme.surfaceVariant,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (unlocked) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                    contentDescription = if (unlocked) "已解锁" else "未解锁",
                    tint = if (unlocked) scheme.primary else scheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = progress.code.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (unlocked) scheme.onSurface else scheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (unlocked) "已解锁" else progress.progressText(),
                style = MaterialTheme.typography.labelLarge,
                color = if (unlocked) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                modifier = Modifier
                    .background(
                        color = if (unlocked) scheme.primaryContainer else scheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Text(
            text = progress.code.description,
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { progress.ratio.toFloat() },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = if (unlocked) scheme.primary else scheme.secondary,
            trackColor = scheme.primaryContainer,
            strokeCap = StrokeCap.Round,
        )
        Text(
            text = when {
                unlockedDate != null -> "解锁于 ${unlockedDate.displayText()}"
                unlocked -> "刚刚达成"
                else -> "还差 ${progress.remainingText()}"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (unlocked) scheme.primary else scheme.onSurfaceVariant,
        )
    }
}

private fun AchievementUiState.ordered(items: List<AchievementProgress>): List<AchievementProgress> =
    items.sortedWith(
        compareByDescending<AchievementProgress> { isUnlocked(it.code) }
            .thenByDescending { it.ratio }
            .thenBy { it.code.ordinal },
    )

private fun AchievementProgress.progressText(): String {
    val unit = code.unit
    return when {
        unit == "%" -> "${currentValue.percentText()} / ${targetValue.toInt()}%"
        unit == "kg" -> "${currentValue.kgNumberText()} / ${targetValue.toInt()} kg"
        unit == "kcal" -> "${currentValue.toInt()} / ${targetValue.toInt()} kcal"
        else -> "${currentValue.toInt()} / ${targetValue.toInt()} $unit"
    }
}

private fun Double.formatAs(unit: String): String = when (unit) {
    "%" -> percentText()
    "kg" -> kgText()
    "kcal" -> kcalText()
    else -> countText(unit)
}

private fun AchievementProgress.remainingText(): String = remaining.formatAs(code.unit)

private fun previewProgress(code: AchievementCode, value: Double) =
    AchievementProgress(code = code, currentValue = value, targetValue = code.targetValue)

private fun previewAchievementState() = AchievementUiState(
    progress = listOf(
        previewProgress(AchievementCode.FIRST_MEAL_LOG, 1.0),
        previewProgress(AchievementCode.FIRST_ACTIVITY_LOG, 1.0),
        previewProgress(AchievementCode.FIRST_WEIGHT_LOG, 41.0),
        previewProgress(AchievementCode.FIRST_DAY_LOGGED, 41.0),
        previewProgress(AchievementCode.RECORD_STREAK_7, 12.0),
        previewProgress(AchievementCode.RECORD_STREAK_30, 12.0),
        previewProgress(AchievementCode.RECORD_DAYS_100, 41.0),
        previewProgress(AchievementCode.DIET_LOG_STREAK_7, 7.0),
        previewProgress(AchievementCode.WEIGHT_DROP_STREAK, 8.0),
        previewProgress(AchievementCode.WEIGHT_DROP_STREAK_14, 8.0),
        previewProgress(AchievementCode.WEEKLY_LOSS_5_DAYS, 8.0),
        previewProgress(AchievementCode.LOSE_1_KG, 4.3),
        previewProgress(AchievementCode.LOSE_5_PERCENT, 5.5),
        previewProgress(AchievementCode.LOSE_10_PERCENT, 5.5),
        previewProgress(AchievementCode.LOSE_5_KG, 4.3),
        previewProgress(AchievementCode.LOSE_15_PERCENT, 5.5),
        previewProgress(AchievementCode.INTAKE_BELOW_BURN_STREAK, 7.0),
        previewProgress(AchievementCode.INTAKE_BELOW_BURN_STREAK_21, 7.0),
        previewProgress(AchievementCode.DEFICIT_7700_KCAL, 5200.0),
        previewProgress(AchievementCode.EXERCISE_STREAK_5, 5.0),
        previewProgress(AchievementCode.EXERCISE_TARGET_STREAK, 6.0),
        previewProgress(AchievementCode.EXERCISE_MINUTES_600, 480.0),
        previewProgress(AchievementCode.EXERCISE_KCAL_5000, 2100.0),
    ),
    unlocked = setOf(
        AchievementCode.FIRST_MEAL_LOG,
        AchievementCode.FIRST_ACTIVITY_LOG,
        AchievementCode.FIRST_WEIGHT_LOG,
        AchievementCode.FIRST_DAY_LOGGED,
        AchievementCode.RECORD_STREAK_7,
        AchievementCode.RECORD_DAYS_100,
        AchievementCode.DIET_LOG_STREAK_7,
        AchievementCode.WEIGHT_DROP_STREAK,
        AchievementCode.WEEKLY_LOSS_5_DAYS,
        AchievementCode.LOSE_1_KG,
        AchievementCode.LOSE_5_PERCENT,
        AchievementCode.INTAKE_BELOW_BURN_STREAK,
        AchievementCode.EXERCISE_STREAK_5,
    ),
    unlockedAt = mapOf(
        AchievementCode.FIRST_WEIGHT_LOG to LocalDate.of(2026, 5, 2),
        AchievementCode.RECORD_STREAK_7 to LocalDate.of(2026, 6, 9),
        AchievementCode.LOSE_1_KG to LocalDate.of(2026, 7, 14),
        AchievementCode.WEIGHT_DROP_STREAK to LocalDate.of(2026, 10, 18),
        AchievementCode.LOSE_5_PERCENT to LocalDate.of(2026, 10, 20),
    ),
    daysConsidered = 41,
)

@Preview(showBackground = true, widthDp = 380, heightDp = 1400)
@Composable
private fun AchievementContentPreview() {
    HealthTheme {
        AchievementContent(state = previewAchievementState(), modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun AchievementEmptyPreview() {
    HealthTheme {
        AchievementContent(state = AchievementUiState(), modifier = Modifier.padding(16.dp))
    }
}
