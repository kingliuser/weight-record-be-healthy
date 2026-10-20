package com.health.tracker.data.repository

import com.health.tracker.data.local.AchievementUnlockDao
import com.health.tracker.data.local.AchievementUnlockEntity
import com.health.tracker.domain.AchievementCode
import com.health.tracker.domain.AchievementRepository
import com.health.tracker.domain.AchievementUnlock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AchievementRepositoryImpl(private val dao: AchievementUnlockDao) : AchievementRepository {

    override fun observeUnlocks(): Flow<List<AchievementUnlock>> =
        dao.observeAll().map { rows ->
            rows.mapNotNull { row ->
                val code = CODES[row.code] ?: return@mapNotNull null
                AchievementUnlock(code, row.unlockedAtMillis)
            }.sortedBy { it.atMillis }
        }

    override suspend fun unlock(code: AchievementCode, progressValue: Double, atMillis: Long) {
        dao.insert(AchievementUnlockEntity(code.name, atMillis, progressValue))
    }

    private companion object {
        /** 改规则时可能留下对不上号的旧 code，跳过而不是让整页崩掉。 */
        val CODES: Map<String, AchievementCode> = AchievementCode.entries.associateBy { it.name }
    }
}
