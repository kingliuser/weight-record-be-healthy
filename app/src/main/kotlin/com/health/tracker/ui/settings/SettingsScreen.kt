package com.health.tracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.health.tracker.domain.ActivityLevel
import com.health.tracker.domain.Sex
import com.health.tracker.ui.containerFactory
import com.health.tracker.ui.bmiText
import com.health.tracker.ui.components.ChoiceChips
import com.health.tracker.ui.components.ErrorBanner
import com.health.tracker.ui.components.MetricRow
import com.health.tracker.ui.components.NumberField
import com.health.tracker.ui.components.PrimaryButton
import com.health.tracker.ui.components.SectionCard
import com.health.tracker.ui.kcalText
import com.health.tracker.ui.theme.HealthTheme

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    firstRun: Boolean = true,
    onDone: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(
        factory = containerFactory {
            SettingsViewModel(it.profileRepository, it.weightRepository)
        },
    ),
) {
    val state by viewModel.state.collectAsState()
    SettingsContent(
        state,
        firstRun,
        onHeightChange = viewModel::onHeightChange,
        onWeightChange = viewModel::onWeightChange,
        onGoalWeightChange = viewModel::onGoalWeightChange,
        onBirthYearChange = viewModel::onBirthYearChange,
        onSexChange = viewModel::onSexChange,
        onActivityLevelChange = viewModel::onActivityLevelChange,
        onWeeklyLossChange = viewModel::onWeeklyLossChange,
        onSave = viewModel::save,
        onDismissError = viewModel::dismissError,
        onDone = onDone,
        modifier = modifier,
    )
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    firstRun: Boolean,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onGoalWeightChange: (String) -> Unit,
    onBirthYearChange: (String) -> Unit,
    onSexChange: (Sex) -> Unit,
    onActivityLevelChange: (ActivityLevel) -> Unit,
    onWeeklyLossChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismissError: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = if (firstRun) "先建档，目标由 App 反推，你只需要填真实身体数据。" else "改完点保存，今日目标会按新数据重算。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        state.error?.let { ErrorBanner(message = it, onDismiss = onDismissError) }

        SectionCard(title = "身体数据") {
            NumberField("身高", state.heightCm, onHeightChange, suffix = "cm")
            NumberField("当前体重", state.weightKg, onWeightChange, suffix = "kg")
            NumberField("目标体重（可不填）", state.goalWeightKg, onGoalWeightChange, suffix = "kg")
            NumberField("出生年份", state.birthYear, onBirthYearChange, suffix = "如 1990")
        }

        SectionCard(title = "性别") {
            ChoiceChips(
                options = Sex.entries.toList(),
                selected = state.sex,
                labelOf = { if (it == Sex.MALE) "男" else "女" },
                onSelect = onSexChange,
            )
        }

        SectionCard(title = "日常活动量") {
            ChoiceChips(
                options = ActivityLevel.entries.toList(),
                selected = state.activityLevel,
                labelOf = { it.label },
                onSelect = onActivityLevelChange,
            )
        }

        SectionCard(title = "减重节奏") {
            NumberField("计划每周减重", state.weeklyLossKg, onWeeklyLossChange, suffix = "kg")
            Text(
                text = "推荐 0.3-0.5 kg/周，超过 1 kg/周 会被自动压回上限。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val target = state.previewTarget
        if (target == null) {
            SectionCard(title = "推荐目标") {
                Text(
                    text = "把身高、当前体重、出生年份、每周减重填成数字，这里会实时给出推荐。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            SectionCard(title = "推荐目标") {
                MetricRow("基础代谢 BMR", target.bmrKcal.kcalText())
                MetricRow("每日总消耗 TDEE", target.tdeeKcal.kcalText())
                MetricRow("每日摄入上限", target.intakeCapKcal.kcalText(), emphasized = true)
                MetricRow("每日运动消耗下限", target.exerciseFloorKcal.kcalText(), emphasized = true)
                MetricRow("BMI", target.bmi.bmiText())
                target.warnings.forEach { warning ->
                    Text(
                        text = warning.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        PrimaryButton(
            text = if (state.saved) "已保存" else if (firstRun) "保存并开始记录" else "保存修改",
            onClick = onSave,
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private val previewSettingsState = SettingsUiState(
    heightCm = "170",
    weightKg = "77.9",
    goalWeightKg = "72",
    birthYear = "1990",
    weeklyLossKg = "0.5",
    loading = false,
)

@Preview(showBackground = true, widthDp = 380, heightDp = 900)
@Composable
private fun SettingsContentPreview() {
    HealthTheme {
        SettingsContent(
            state = previewSettingsState,
            firstRun = true,
            onHeightChange = {},
            onWeightChange = {},
            onGoalWeightChange = {},
            onBirthYearChange = {},
            onSexChange = {},
            onActivityLevelChange = {},
            onWeeklyLossChange = {},
            onSave = {},
            onDismissError = {},
            onDone = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 700)
@Composable
private fun SettingsEmptyPreview() {
    HealthTheme {
        SettingsContent(
            state = SettingsUiState(loading = false),
            firstRun = true,
            onHeightChange = {},
            onWeightChange = {},
            onGoalWeightChange = {},
            onBirthYearChange = {},
            onSexChange = {},
            onActivityLevelChange = {},
            onWeeklyLossChange = {},
            onSave = {},
            onDismissError = {},
            onDone = {},
        )
    }
}
