package com.health.tracker.data.repository

import com.health.tracker.data.local.AchievementUnlockDao
import com.health.tracker.data.local.AchievementUnlockEntity
import com.health.tracker.domain.AchievementCode
import com.health.tracker.domain.AchievementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AchievementRepositoryImpl(private val dao: AchievementUnlockDao) : AchievementRepository {

    override fun observeUnlocked(): Flow<Set<AchievementCode>> =
        dao.observeAll().map { rows -> rows.map { AchievementCode.valueOf(it.code) }.toSet() }

    override suspend fun unlock(code: AchievementCode, progressValue: Double, atMillis: Long) {
        dao.insert(AchievementUnlockEntity(code.name, atMillis, progressValue))
    }
}
