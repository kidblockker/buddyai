package com.buddy.app.memory

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InteractionDao {
    @Insert
    suspend fun insert(interaction: InteractionEntity): Long

    @Query("SELECT * FROM interactions ORDER BY timestamp DESC LIMIT 50")
    suspend fun getRecentInteractions(): List<InteractionEntity>

    @Query("SELECT * FROM interactions ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLastN(limit: Int): List<InteractionEntity>

    @Query("DELETE FROM interactions WHERE id IN (SELECT id FROM interactions ORDER BY timestamp ASC LIMIT :count)")
    suspend fun deleteOldest(count: Int)

    @Query("SELECT COUNT(*) FROM interactions")
    suspend fun getCount(): Int
}

@Dao
interface UserProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profile WHERE `key` = :key")
    suspend fun get(key: String): UserProfileEntity?

    @Query("SELECT * FROM user_profile")
    suspend fun getAll(): List<UserProfileEntity>
}

@Dao
interface BehaviorPatternDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pattern: BehaviorPatternEntity)

    @Query("SELECT * FROM behavior_patterns WHERE type = :type ORDER BY lastSeen DESC LIMIT 10")
    suspend fun getByType(type: String): List<BehaviorPatternEntity>

    @Query("SELECT * FROM behavior_patterns ORDER BY lastSeen DESC LIMIT 20")
    suspend fun getRecent(): List<BehaviorPatternEntity>

    @Query("UPDATE behavior_patterns SET count = count + 1, lastSeen = :now WHERE type = :type AND observation = :observation")
    suspend fun incrementPattern(type: String, observation: String, now: Long = System.currentTimeMillis())
}
