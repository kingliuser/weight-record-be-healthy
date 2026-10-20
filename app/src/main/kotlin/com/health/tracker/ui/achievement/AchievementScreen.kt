package com.health.tracker.ui.achievement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.health.tracker.domain.AchievementCode
import com.health.tracker.domain.AchievementFamily
import com.health.tracker.domain.AchievementProgress
import com.health.tracker.ui.containerFactory
import com.health.tracker.ui.components.EmptyHint
import com.health.tracker.ui.components.SectionCard
import com.health.tracker.ui.theme.HealthTheme

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
        if (state.daysConsidered == 0) {
            SectionCard(title = "还没有可计算的数据") {
                EmptyHint("先去今日页记几笔饮食或运动，或者在体重页称一次重，成就就会开始累计。")
            }
        }

        AchievementFamily.entries.forEach { family ->
            val items = grouped[family]
            if (!items.isNullOrEmpty()) {
                SectionCard(title = family.label) {
                    items.forEach { progress ->
                        AchievementRow(progress = progress, unlocked = progress.code in state.unlocked)
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementRow(progress: AchievementProgress, unlocked: Boolean) {
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
            Text(
                text = progress.code.title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (unlocked) "已解锁" else progress.valueText(),
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
    }
}

private fun AchievementProgress.valueText(): String = when (code) {
    AchievementCode.LOSE_5_PERCENT, AchievementCode.LOSE_10_PERCENT ->
        "${currentValue.toInt()}% / ${targetValue.toInt()}%"
    else -> "${currentValue.toInt()} 天 / ${targetValue.toInt()} 天"
}

private fun previewAchievementState() = AchievementUiState(
    progress = listOf(
        AchievementProgress(AchievementCode.WEIGHT_DROP_STREAK, 12.0, 7.0),
        AchievementProgress(AchievementCode.LOSE_5_PERCENT, 5.6, 5.0),
        AchievementProgress(AchievementCode.LOSE_10_PERCENT, 5.6, 10.0),
        AchievementProgress(AchievementCode.INTAKE_BELOW_BURN_STREAK, 12.0, 7.0),
        AchievementProgress(AchievementCode.EXERCISE_TARGET_STREAK, 8.0, 7.0),
    ),
    unlocked = setOf(
        AchievementCode.WEIGHT_DROP_STREAK,
        AchievementCode.LOSE_5_PERCENT,
        AchievementCode.INTAKE_BELOW_BURN_STREAK,
        AchievementCode.EXERCISE_TARGET_STREAK,
    ),
    daysConsidered = 12,
)

@Preview(showBackground = true, widthDp = 380, heightDp = 900)
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
