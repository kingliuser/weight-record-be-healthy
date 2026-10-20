package com.health.tracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProfileEntity::class,
        WeightLogEntity::class,
        MealEntryEntity::class,
        ActivityEntryEntity::class,
        AchievementUnlockEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun weightLogDao(): WeightLogDao
    abstract fun mealEntryDao(): MealEntryDao
    abstract fun activityEntryDao(): ActivityEntryDao
    abstract fun achievementUnlockDao(): AchievementUnlockDao
}
