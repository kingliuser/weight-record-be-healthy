package com.health.tracker.di

import android.app.Application
import androidx.room.Room
import com.health.tracker.data.local.HealthDatabase
import com.health.tracker.data.repository.AchievementRepositoryImpl
import com.health.tracker.data.repository.ExerciseRepositoryImpl
import com.health.tracker.data.repository.FoodRepositoryImpl
import com.health.tracker.data.repository.LedgerRepositoryImpl
import com.health.tracker.data.repository.ProfileRepositoryImpl
import com.health.tracker.data.repository.WeightRepositoryImpl
import com.health.tracker.domain.AchievementRepository
import com.health.tracker.domain.ExerciseRepository
import com.health.tracker.domain.FoodRepository
import com.health.tracker.domain.LedgerRepository
import com.health.tracker.domain.ProfileRepository
import com.health.tracker.domain.WeightRepository

/** 手写容器：UI 层只认 domain 仓储接口，Room 细节留在 :data。 */
class AppContainer(private val application: Application) {

    private val database: HealthDatabase by lazy {
        Room.databaseBuilder(application, HealthDatabase::class.java, "health.db").build()
    }

    val profileRepository: ProfileRepository by lazy { ProfileRepositoryImpl(database.profileDao()) }
    val weightRepository: WeightRepository by lazy { WeightRepositoryImpl(database.weightLogDao()) }
    val foodRepository: FoodRepository by lazy { FoodRepositoryImpl(database.mealEntryDao()) }
    val exerciseRepository: ExerciseRepository by lazy { ExerciseRepositoryImpl(database.activityEntryDao()) }
    val ledgerRepository: LedgerRepository by lazy {
        LedgerRepositoryImpl(database.mealEntryDao(), database.activityEntryDao(), database.weightLogDao())
    }
    val achievementRepository: AchievementRepository by lazy {
        AchievementRepositoryImpl(database.achievementUnlockDao())
    }
}

fun Application.appContainer(): AppContainer =
    (this as HealthApplication).container

class HealthApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
