package com.health.tracker.ui.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.health.tracker.domain.WeightEntry
import com.health.tracker.ui.containerFactory
import com.health.tracker.ui.components.DeletableRow
import com.health.tracker.ui.components.EmptyHint
import com.health.tracker.ui.components.GradientCard
import com.health.tracker.ui.components.MetricRow
import com.health.tracker.ui.components.NumberField
import com.health.tracker.ui.components.PrimaryButton
import com.health.tracker.ui.components.SectionCard
import com.health.tracker.ui.displayText
import com.health.tracker.ui.kgNumberText
import com.health.tracker.ui.kgText
import com.health.tracker.ui.signedKgText
import com.health.tracker.ui.theme.HealthTheme
import java.time.LocalDate

@Composable
fun WeightScreen(
    modifier: Modifier = Modifier,
    viewModel: WeightViewModel = viewModel(
        factory = containerFactory { WeightViewModel(it.profileRepository, it.weightRepository) },
    ),
) {
    val state by viewModel.state.collectAsState()
    WeightContent(
        state = state,
        onRecord = viewModel::recordToday,
        onDelete = viewModel::delete,
        modifier = modifier,
    )
}

@Composable
fun WeightContent(
    state: WeightUiState,
    onRecord: (String) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var weightText by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        GradientCard {
            Text(
                text = "当前体重",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = state.latestWeightKg?.kgNumberText() ?: "—",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                )
                Text(
                    text = "kg",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            if (state.entries.isNotEmpty()) {
                Text(
                    text = "从起始 ${state.startWeightKg?.kgText() ?: "—"} 到现在，累计 ${(-state.totalChangeKg).signedKgText()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }

        SectionCard(title = "记今天的体重") {
            NumberField(label = "体重", value = weightText, onValueChange = { weightText = it }, suffix = "kg")
            PrimaryButton(
                text = "保存到 ${state.today.displayText()}",
                onClick = {
                    onRecord(weightText)
                    weightText = ""
                },
            )
        }

        SectionCard(title = "总览") {
            if (state.entries.isEmpty()) {
                EmptyHint("还没有体重记录，先称一次。")
            } else {
                MetricRow("起始体重", state.startWeightKg?.kgText() ?: "—")
                MetricRow("当前体重", state.latestWeightKg?.kgText() ?: "—")
                MetricRow("累计变化", (-state.totalChangeKg).signedKgText(), emphasized = true)
                state.toGoalKg?.let { MetricRow("距目标体重", if (it <= 0.05) "已达成" else it.kgText()) }
            }
        }

        SectionCard(title = "历史") {
            if (state.entries.isEmpty()) {
                EmptyHint("每天称一次，趋势就从这里长出来。")
            }
            state.entries.reversed().forEachIndexed { reversedIndex, entry ->
                val index = state.entries.lastIndex - reversedIndex
                val delta = state.deltaOf(index)
                DeletableRow(
                    label = "${entry.date.displayText()}　${entry.weightKg.kgText()}",
                    value = delta?.let { "（${it.signedKgText()}）" } ?: "",
                    onDelete = { onDelete(entry.id) },
                )
            }
        }
    }
}

private fun previewWeightState() = WeightUiState(
    entries = listOf(
        WeightEntry(id = 1, date = LocalDate.of(2026, 10, 1), weightKg = 82.0),
        WeightEntry(id = 2, date = LocalDate.of(2026, 10, 2), weightKg = 81.6),
        WeightEntry(id = 3, date = LocalDate.of(2026, 10, 3), weightKg = 81.1),
        WeightEntry(id = 4, date = LocalDate.of(2026, 10, 20), weightKg = 77.9),
    ),
    goalWeightKg = 72.0,
    today = LocalDate.of(2026, 10, 20),
)

@Preview(showBackground = true, widthDp = 380, heightDp = 900)
@Composable
private fun WeightContentPreview() {
    HealthTheme {
        WeightContent(
            state = previewWeightState(),
            onRecord = {},
            onDelete = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun WeightEmptyPreview() {
    HealthTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionCard(title = "空状态") {
                EmptyHint("还没有体重记录，先称一次。")
            }
        }
    }
}
