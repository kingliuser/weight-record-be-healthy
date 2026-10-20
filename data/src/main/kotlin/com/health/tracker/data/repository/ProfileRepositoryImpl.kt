package com.health.tracker.data.repository

import com.health.tracker.data.local.ProfileDao
import com.health.tracker.domain.Profile
import com.health.tracker.domain.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepositoryImpl(private val dao: ProfileDao) : ProfileRepository {

    override fun observe(): Flow<Profile?> = dao.observe().map { it?.toDomain() }

    override suspend fun current(): Profile? = dao.get()?.toDomain()

    override suspend fun save(profile: Profile) =
        dao.upsert(profile.toEntity(System.currentTimeMillis()))
}
