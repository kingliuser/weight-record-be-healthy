package com.health.tracker.data.repository

import com.health.tracker.data.local.WeightLogDao
import com.health.tracker.data.local.WeightLogEntity
import com.health.tracker.domain.WeightEntry
import com.health.tracker.domain.WeightRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WeightRepositoryImpl(private val dao: WeightLogDao) : WeightRepository {

    override fun observeDaily(): Flow<List<WeightEntry>> =
        dao.observeDailyLatest().map { rows -> rows.map { it.toDomain() } }

    override fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<WeightEntry>> =
        dao.observeRawBetween(from.toEpochDay(), to.toEpochDay())
            .map { rows -> rows.dailyLatest().map { it.toDomain() } }

    /** 同一天重复称重保留历史行，读取时以最后一条为当日值。 */
    override suspend fun latest(): WeightEntry? = dao.latest()?.toDomain()

    override suspend fun record(date: LocalDate, weightKg: Double) {
        dao.insert(WeightLogEntity(dateEpochDay = date.toEpochDay(), weightKg = weightKg))
    }

    override suspend fun delete(id: Long) = dao.deleteById(id)
}

internal fun List<WeightLogEntity>.dailyLatest(): List<WeightLogEntity> =
    groupBy { it.dateEpochDay }
        .mapNotNull { (_, rows) -> rows.maxByOrNull { it.id } }
        .sortedBy { it.dateEpochDay }
