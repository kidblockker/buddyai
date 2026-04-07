package com.buddy.app.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "interactions")
data class InteractionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userMessage: String,
    val buddyResponse: String,
    val timestamp: Long = System.currentTimeMillis(),
    val emotion: String = "neutral"
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "behavior_patterns")
data class BehaviorPatternEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,        // "productivity", "health", "mood", "routine"
    val observation: String,
    val count: Int = 1,
    val lastSeen: Long = System.currentTimeMillis()
)
