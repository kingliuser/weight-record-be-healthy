package com.health.tracker.domain

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** 一天四张表的聚合视图，月度报表和成就评估都吃这个。 */
data class DayTotals(
    val date: LocalDate,
    val intakeKcal: Double,
    val exerciseKcal: Double,
    val weightKg: Double?,
)

interface ProfileRepository {
    fun observe(): Flow<Profile?>
    suspend fun current(): Profile?
    suspend fun save(profile: Profile)
}

interface WeightRepository {
    /** 每个自然日取最后一次称重，按日期升序。 */
    fun observeDaily(): Flow<List<WeightEntry>>
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<WeightEntry>>
    suspend fun latest(): WeightEntry?
    suspend fun record(date: LocalDate, weightKg: Double)
    suspend fun delete(id: Long)
}

interface FoodRepository {
    fun observeForDay(date: LocalDate): Flow<List<FoodEntry>>
    suspend fun add(date: LocalDate, name: String, kcal: Double, mealType: MealType)
    suspend fun delete(id: Long)
}

interface ExerciseRepository {
    fun observeForDay(date: LocalDate): Flow<List<ExerciseEntry>>
    suspend fun add(date: LocalDate, name: String, kcal: Double, minutes: Int)
    suspend fun delete(id: Long)
}

interface LedgerRepository {
    fun observeDayTotals(from: LocalDate, to: LocalDate): Flow<List<DayTotals>>
}

interface AchievementRepository {
    fun observeUnlocked(): Flow<Set<AchievementCode>>
    suspend fun unlock(code: AchievementCode, progressValue: Double, atMillis: Long)
}
