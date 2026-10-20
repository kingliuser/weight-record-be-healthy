package com.health.tracker.data.repository

import com.health.tracker.data.local.MealEntryDao
import com.health.tracker.data.local.MealEntryEntity
import com.health.tracker.domain.FoodEntry
import com.health.tracker.domain.FoodRepository
import com.health.tracker.domain.MealType
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FoodRepositoryImpl(private val dao: MealEntryDao) : FoodRepository {

    override fun observeForDay(date: LocalDate): Flow<List<FoodEntry>> =
        dao.observeForDay(date.toEpochDay()).map { rows -> rows.map { it.toDomain() } }

    override suspend fun add(date: LocalDate, name: String, kcal: Double, mealType: MealType) {
        dao.insert(
            MealEntryEntity(
                dateEpochDay = date.toEpochDay(),
                occurredAtMillis = System.currentTimeMillis(),
                name = name,
                kcal = kcal,
                mealType = mealType.name,
            ),
        )
    }

    override suspend fun delete(id: Long) = dao.deleteById(id)
}
