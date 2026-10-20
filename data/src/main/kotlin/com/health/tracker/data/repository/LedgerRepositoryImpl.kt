package com.health.tracker.data.repository

import com.health.tracker.data.local.ActivityEntryDao
import com.health.tracker.data.local.MealEntryDao
import com.health.tracker.data.local.WeightLogDao
import com.health.tracker.domain.DayTotals
import com.health.tracker.domain.LedgerRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class LedgerRepositoryImpl(
    private val mealDao: MealEntryDao,
    private val activityDao: ActivityEntryDao,
    private val weightDao: WeightLogDao,
) : LedgerRepository {

    override fun observeDayTotals(from: LocalDate, to: LocalDate): Flow<List<DayTotals>> =
        combine(
            mealDao.observeDailyTotals(from.toEpochDay(), to.toEpochDay()),
            activityDao.observeDailyTotals(from.toEpochDay(), to.toEpochDay()),
            weightDao.observeRawBetween(from.toEpochDay(), to.toEpochDay()),
        ) { meals, activities, weights ->
            val intake = meals.associateBy { it.epochDay }
            val exercise = activities.associateBy { it.epochDay }
            val weight = weights.dailyLatest().associate { it.dateEpochDay to it.weightKg }
            val days = (intake.keys + exercise.keys + weight.keys).sorted()
            days.map { day ->
                DayTotals(
                    date = LocalDate.ofEpochDay(day),
                    intakeKcal = intake[day]?.kcal ?: 0.0,
                    exerciseKcal = exercise[day]?.kcal ?: 0.0,
                    weightKg = weight[day],
                    mealCount = intake[day]?.entries ?: 0,
                    activityCount = exercise[day]?.entries ?: 0,
                    exerciseMinutes = exercise[day]?.minutes ?: 0,
                )
            }
        }
}
