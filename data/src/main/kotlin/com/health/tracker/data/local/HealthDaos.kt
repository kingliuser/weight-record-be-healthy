package com.health.tracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Upsert
    suspend fun upsert(profile: ProfileEntity)

    @Query("SELECT * FROM profile WHERE id = 1 LIMIT 1")
    fun observe(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 1 LIMIT 1")
    suspend fun get(): ProfileEntity?
}

@Dao
interface WeightLogDao {
    @Insert
    suspend fun insert(entry: WeightLogEntity): Long

    @Query("DELETE FROM weight_log WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** 每天取最后一次称重作为当日体重。 */
    @Query(
        """
        SELECT w.* FROM weight_log w
        WHERE w.id = (
            SELECT o.id FROM weight_log o
            WHERE o.dateEpochDay = w.dateEpochDay
            ORDER BY o.dateEpochDay DESC, o.id DESC LIMIT 1
        )
        ORDER BY w.dateEpochDay ASC
        """
    )
    fun observeDailyLatest(): Flow<List<WeightLogEntity>>

    @Query(
        "SELECT * FROM weight_log WHERE dateEpochDay BETWEEN :fromEpochDay AND :toEpochDay " +
            "ORDER BY dateEpochDay ASC"
    )
    fun observeBetween(fromEpochDay: Long, toEpochDay: Long): Flow<List<WeightLogEntity>>

    /** 区间内所有称重原始行，按天取最新一条由仓储层做。 */
    @Query(
        "SELECT * FROM weight_log WHERE dateEpochDay BETWEEN :from AND :to " +
            "ORDER BY dateEpochDay ASC, id ASC"
    )
    fun observeRawBetween(from: Long, to: Long): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_log ORDER BY dateEpochDay DESC, id DESC LIMIT 1")
    suspend fun latest(): WeightLogEntity?
}

@Dao
interface MealEntryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: MealEntryEntity): Long

    @Query("DELETE FROM meal_entry WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM meal_entry WHERE dateEpochDay = :epochDay ORDER BY occurredAtMillis ASC")
    fun observeForDay(epochDay: Long): Flow<List<MealEntryEntity>>

    @Query("SELECT COALESCE(SUM(kcal), 0.0) FROM meal_entry WHERE dateEpochDay = :epochDay")
    fun observeDayTotalKcal(epochDay: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(kcal), 0.0) FROM meal_entry WHERE dateEpochDay BETWEEN :from AND :to")
    fun observeRangeTotalKcal(from: Long, to: Long): Flow<Double>

    @Query(
        "SELECT dateEpochDay AS epochDay, COALESCE(SUM(kcal), 0.0) AS kcal FROM meal_entry " +
            "WHERE dateEpochDay BETWEEN :from AND :to GROUP BY dateEpochDay ORDER BY dateEpochDay ASC"
    )
    fun observeDailyTotals(from: Long, to: Long): Flow<List<DayKcalRow>>
}

@Dao
interface ActivityEntryDao {
    @Insert
    suspend fun insert(entry: ActivityEntryEntity): Long

    @Query("DELETE FROM activity_entry WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM activity_entry WHERE dateEpochDay = :epochDay ORDER BY id ASC")
    fun observeForDay(epochDay: Long): Flow<List<ActivityEntryEntity>>

    @Query("SELECT COALESCE(SUM(kcal), 0.0) FROM activity_entry WHERE dateEpochDay = :epochDay")
    fun observeDayTotalKcal(epochDay: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(kcal), 0.0) FROM activity_entry WHERE dateEpochDay BETWEEN :from AND :to")
    fun observeRangeTotalKcal(from: Long, to: Long): Flow<Double>

    @Query(
        "SELECT dateEpochDay AS epochDay, COALESCE(SUM(kcal), 0.0) AS kcal FROM activity_entry " +
            "WHERE dateEpochDay BETWEEN :from AND :to GROUP BY dateEpochDay ORDER BY dateEpochDay ASC"
    )
    fun observeDailyTotals(from: Long, to: Long): Flow<List<DayKcalRow>>
}

@Dao
interface AchievementUnlockDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: AchievementUnlockEntity): Long

    @Query("SELECT * FROM achievement_unlock")
    fun observeAll(): Flow<List<AchievementUnlockEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM achievement_unlock WHERE code = :code)")
    suspend fun isUnlocked(code: String): Boolean
}
