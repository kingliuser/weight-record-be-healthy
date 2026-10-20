package com.health.tracker.ui.monthly

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.health.tracker.domain.DayTotals
import com.health.tracker.ui.containerFactory
import com.health.tracker.ui.components.EmptyHint
import com.health.tracker.ui.components.GradientCard
import com.health.tracker.ui.components.MetricRow
import com.health.tracker.ui.components.SectionCard
import com.health.tracker.ui.displayText
import com.health.tracker.ui.kcalText
import com.health.tracker.ui.kgText
import com.health.tracker.ui.theme.HealthTheme
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun MonthlyScreen(
    modifier: Modifier = Modifier,
    viewModel: MonthlyViewModel = viewModel(
        factory = containerFactory {
            MonthlyViewModel(it.profileRepository, it.weightRepository, it.ledgerRepository)
        },
    ),
) {
    val state by viewModel.state.collectAsState()
    MonthlyContent(
        state = state,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        modifier = modifier,
    )
}

@Composable
fun MonthlyContent(
    state: MonthlyUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = state.current

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MonthSwitcher(state, onPreviousMonth, onNextMonth)

        if (summary == null) {
            SectionCard(title = "月度汇总") {
                EmptyHint("这个月还没有任何记录。")
            }
            return@Column
        }

        GradientCard {
            Text(
                text = "${state.selectedMonth.year} 年 ${state.selectedMonth.monthValue} 月净缺口",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
            Text(
                text = summary.netDeficitKcal.kcalText(),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
            )
            Text(
                text = "${summary.daysRecorded} 天有记录 · 摄入 ${summary.intakeKcal.kcalText()} · 运动 ${summary.exerciseKcal.kcalText()}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
            )
        }

        SectionCard(title = "月度汇总") {
            MetricRow("有记录天数", "${summary.daysRecorded} 天")
            MetricRow("累计摄入", summary.intakeKcal.kcalText())
            MetricRow("累计运动消耗", summary.exerciseKcal.kcalText())
            MetricRow("估算总消耗", summary.estimatedBurnKcal.kcalText())
            MetricRow("净缺口", summary.netDeficitKcal.kcalText(), emphasized = true)
        }

        SectionCard(title = "减重对照") {
            MetricRow("热量推算应减", summary.theoryLossKg.kgText())
            MetricRow("秤上实际", summary.actualLossKg?.kgText() ?: "至少两次称重才能算")
            summary.lossGapKg?.let { gap ->
                Text(
                    text = if (gap > 0.5) "实际比推算多减了 ${gap.kgText()}，可能有漏记或水分波动。"
                    else if (gap < -0.5) "实际比推算少减了 ${(-gap).kgText()}，热量记录可能偏少。"
                    else "热量账和体重账基本对得上。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            summary.firstWeightKg?.let { MetricRow("月初体重", it.kgText()) }
            summary.lastWeightKg?.let { MetricRow("月末体重", it.kgText()) }
        }

        SectionCard(title = "每日明细") {
            if (state.days.isEmpty()) {
                EmptyHint("这个月还没有逐日数据。")
            }
            state.days.forEach { day ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = day.date.displayText(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1.5f),
                    )
                    Text(
                        text = "吃 ${day.intakeKcal.kcalText()}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1.6f),
                    )
                    Text(
                        text = "练 ${day.exerciseKcal.kcalText()}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1.5f),
                    )
                    Text(
                        text = day.weightKg?.kgText() ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthSwitcher(
    state: MonthlyUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPreviousMonth, enabled = state.canGoPrevious) {
            Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "上一月")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${state.selectedMonth.year} 年 ${state.selectedMonth.monthValue} 月",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "左右切换月份",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onNextMonth, enabled = state.canGoNext) {
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "下一月")
        }
    }
}

private fun previewMonthlyState() = MonthlyUiState(
    selectedMonth = YearMonth.of(2026, 10),
    months = listOf(
        MonthSummary(
            month = YearMonth.of(2026, 10),
            daysRecorded = 12,
            intakeKcal = 16800.0,
            exerciseKcal = 1600.0,
            tdeeKcal = 1917.0,
            firstWeightKg = 82.0,
            lastWeightKg = 77.9,
        ),
    ),
    days = listOf(
        DayTotals(date = LocalDate.of(2026, 10, 18), intakeKcal = 1400.0, exerciseKcal = 200.0, weightKg = 78.2),
        DayTotals(date = LocalDate.of(2026, 10, 19), intakeKcal = 1500.0, exerciseKcal = 0.0, weightKg = 78.0),
        DayTotals(date = LocalDate.of(2026, 10, 20), intakeKcal = 1400.0, exerciseKcal = 200.0, weightKg = 77.9),
    ),
)

@Preview(showBackground = true, widthDp = 380, heightDp = 900)
@Composable
private fun MonthlyContentPreview() {
    HealthTheme {
        MonthlyContent(
            state = previewMonthlyState(),
            onPreviousMonth = {},
            onNextMonth = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun MonthlyEmptyPreview() {
    HealthTheme {
        MonthlyContent(
            state = MonthlyUiState(),
            onPreviousMonth = {},
            onNextMonth = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
