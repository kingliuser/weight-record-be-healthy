package com.health.tracker.ui.today

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.health.tracker.domain.DailyTarget
import com.health.tracker.domain.ExerciseEntry
import com.health.tracker.domain.FoodEntry
import com.health.tracker.domain.MealType
import com.health.tracker.ui.containerFactory
import com.health.tracker.ui.components.ChoiceChipRow
import com.health.tracker.ui.components.DeletableRow
import com.health.tracker.ui.components.EmptyHint
import com.health.tracker.ui.components.GradientCard
import com.health.tracker.ui.components.NumberField
import com.health.tracker.ui.components.PrimaryButton
import com.health.tracker.ui.components.SectionCard
import com.health.tracker.ui.components.StatProgress
import com.health.tracker.ui.displayText
import com.health.tracker.ui.kcalText
import com.health.tracker.ui.kgText
import com.health.tracker.ui.theme.HealthTheme
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TodayScreen(
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = viewModel(
        factory = containerFactory {
            TodayViewModel(it.profileRepository, it.weightRepository, it.foodRepository, it.exerciseRepository)
        },
    ),
) {
    val state by viewModel.state.collectAsState()
    TodayContent(
        state = state,
        onAddFood = viewModel::addFood,
        onAddExercise = viewModel::addExercise,
        onDeleteFood = viewModel::deleteFood,
        onDeleteExercise = viewModel::deleteExercise,
        modifier = modifier,
    )
}

@Composable
fun TodayContent(
    state: TodayUiState,
    onAddFood: (String, String, MealType) -> Unit,
    onAddExercise: (String, String, String) -> Unit,
    onDeleteFood: (Long) -> Unit,
    onDeleteExercise: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var foodName by remember { mutableStateOf("") }
    var foodKcal by remember { mutableStateOf("") }
    var mealType by remember { mutableStateOf(MealType.LUNCH) }
    var exerciseName by remember { mutableStateOf("") }
    var exerciseKcal by remember { mutableStateOf("") }
    var exerciseMinutes by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        HeroSummary(state)

        SectionCard(title = "记一笔饮食") {
            NumberField(label = "吃了什么", value = foodName, onValueChange = { foodName = it })
            NumberField(label = "热量", value = foodKcal, onValueChange = { foodKcal = it }, suffix = "kcal")
            ChoiceChipRow(
                options = MealType.entries.toList(),
                selected = mealType,
                labelOf = { it.label },
                onSelect = { mealType = it },
            )
            PrimaryButton(
                text = "加入今日饮食",
                onClick = {
                    onAddFood(foodName, foodKcal, mealType)
                    foodName = ""
                    foodKcal = ""
                },
            )
        }

        SectionCard(title = "今日饮食") {
            if (state.foods.isEmpty()) {
                EmptyHint("今天还没有吃的东西要记。")
            }
            state.foods.forEach { entry ->
                DeletableRow(
                    label = "${entry.name} · ${entry.mealType.label}",
                    value = entry.kcal.kcalText(),
                    onDelete = { onDeleteFood(entry.id) },
                )
            }
        }

        SectionCard(title = "记一笔运动") {
            NumberField(label = "做了什么", value = exerciseName, onValueChange = { exerciseName = it })
            NumberField(label = "消耗", value = exerciseKcal, onValueChange = { exerciseKcal = it }, suffix = "kcal")
            NumberField(label = "时长", value = exerciseMinutes, onValueChange = { exerciseMinutes = it }, suffix = "分钟")
            PrimaryButton(
                text = "加入今日消耗",
                onClick = {
                    onAddExercise(exerciseName, exerciseKcal, exerciseMinutes)
                    exerciseName = ""
                    exerciseKcal = ""
                    exerciseMinutes = ""
                },
            )
        }

        SectionCard(title = "今日运动") {
            if (state.exercises.isEmpty()) {
                EmptyHint("还没动过，记一笔看看。")
            }
            state.exercises.forEach { entry ->
                DeletableRow(
                    label = "${entry.name} · ${entry.minutes} 分钟",
                    value = entry.kcal.kcalText(),
                    onDelete = { onDeleteExercise(entry.id) },
                )
            }
        }
    }
}

@Composable
private fun HeroSummary(state: TodayUiState) {
    val target = state.target
    GradientCard {
        Text(
            text = "${state.today.displayText()} · 目标执行情况",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.75f),
        )
        if (target == null) {
            Text(
                text = if (state.loading) "正在读取数据…" else "先在设置里建档，才能算出今日目标。",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
            )
            return@GradientCard
        }
        val overBudget = state.intakeRemainingKcal < 0
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = if (overBudget) "今日已超出" else "今天还能吃",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
            Text(
                text = abs(state.intakeRemainingKcal).roundToInt().toString(),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
            )
            Text(
                text = "kcal · 摄入上限 ${target.intakeCapKcal.kcalText()}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
        HeroBar(
            label = "摄入",
            note = "${state.intakeKcal.kcalText()} / ${target.intakeCapKcal.kcalText()}",
            ratio = if (target.intakeCapKcal <= 0.0) 0f else (state.intakeKcal / target.intakeCapKcal).toFloat(),
        )
        HeroBar(
            label = "运动",
            note = "${state.exerciseKcal.kcalText()} / 下限 ${target.exerciseFloorKcal.kcalText()}",
            ratio = if (target.exerciseFloorKcal <= 0.0) 1f else (state.exerciseKcal / target.exerciseFloorKcal).toFloat(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "当前体重",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = state.weightKg?.kgText() ?: "今天还没称重",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun HeroBar(label: String, note: String, ratio: Float) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.weight(1f),
            )
            Text(text = note, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
        }
        LinearProgressIndicator(
            progress = { ratio.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.25f),
            strokeCap = StrokeCap.Round,
        )
    }
}

private fun previewTodayState() = TodayUiState(
    loading = false,
    target = DailyTarget(
        bmrKcal = 1400.0,
        tdeeKcal = 1925.0,
        intakeCapKcal = 1540.0,
        exerciseFloorKcal = 165.0,
        plannedWeeklyLossKg = 0.5,
        bmi = 27.0,
        warnings = emptySet(),
    ),
    weightKg = 77.9,
    foods = listOf(
        FoodEntry(id = 1, date = LocalDate.of(2026, 10, 20), name = "牛肉拉面", kcal = 620.0, mealType = MealType.LUNCH),
        FoodEntry(id = 2, date = LocalDate.of(2026, 10, 20), name = "酸奶", kcal = 140.0, mealType = MealType.SNACK),
    ),
    exercises = listOf(
        ExerciseEntry(id = 1, date = LocalDate.of(2026, 10, 20), name = "快走", kcal = 200.0, minutes = 40),
    ),
    today = LocalDate.of(2026, 10, 20),
)

@Preview(showBackground = true, widthDp = 380, heightDp = 900)
@Composable
private fun TodayContentPreview() {
    HealthTheme {
        TodayContent(
            state = previewTodayState(),
            onAddFood = { _, _, _ -> },
            onAddExercise = { _, _, _ -> },
            onDeleteFood = {},
            onDeleteExercise = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun TodayProgressLinePreview() {
    HealthTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            StatProgress(label = "摄入 1400 kcal / 上限 1540 kcal", ratio = 0.91f, footnote = "还能吃 140 kcal")
            StatProgress(
                label = "摄入 1720 kcal / 上限 1540 kcal",
                ratio = 1f,
                footnote = "已超出 180 kcal",
                over = true,
            )
        }
    }
}
