package com.health.tracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Long = SINGLE_PROFILE_ID,
    val heightCm: Double,
    val sex: String,
    val birthYear: Int,
    val activityLevel: String,
    val targetWeeklyLossKg: Double,
    val goalWeightKg: Double?,
    val updatedAtMillis: Long,
)

@Entity(tableName = "weight_log")
data class WeightLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val weightKg: Double,
    val note: String? = null,
)

@Entity(tableName = "meal_entry")
data class MealEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val occurredAtMillis: Long,
    val name: String,
    val kcal: Double,
    val mealType: String,
)

@Entity(tableName = "activity_entry")
data class ActivityEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val name: String,
    val kcal: Double,
    val minutes: Int,
)

/** 只持久化解锁状态；成就阈值定义在 :domain，改规则不需要迁移数据库。 */
@Entity(tableName = "achievement_unlock")
data class AchievementUnlockEntity(
    @PrimaryKey val code: String,
    val unlockedAtMillis: Long,
    val progressSnapshot: Double,
)

/** 聚合查询的投影行，不是表。 */
data class DayKcalRow(val epochDay: Long, val kcal: Double)

const val SINGLE_PROFILE_ID = 1L
