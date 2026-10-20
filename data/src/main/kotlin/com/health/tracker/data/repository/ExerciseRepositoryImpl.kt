package com.health.tracker.data.repository

import com.health.tracker.data.local.ActivityEntryDao
import com.health.tracker.data.local.ActivityEntryEntity
import com.health.tracker.domain.ExerciseEntry
import com.health.tracker.domain.ExerciseRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExerciseRepositoryImpl(private val dao: ActivityEntryDao) : ExerciseRepository {

    override fun observeForDay(date: LocalDate): Flow<List<ExerciseEntry>> =
        dao.observeForDay(date.toEpochDay()).map { rows -> rows.map { it.toDomain() } }

    override suspend fun add(date: LocalDate, name: String, kcal: Double, minutes: Int) {
        dao.insert(
            ActivityEntryEntity(
                dateEpochDay = date.toEpochDay(),
                name = name,
                kcal = kcal,
                minutes = minutes,
            ),
        )
    }

    override suspend fun delete(id: Long) = dao.deleteById(id)
}
