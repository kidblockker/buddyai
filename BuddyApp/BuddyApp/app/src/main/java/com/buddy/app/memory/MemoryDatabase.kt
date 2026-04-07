package com.buddy.app.memory

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        InteractionEntity::class,
        UserProfileEntity::class,
        BehaviorPatternEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MemoryDatabase : RoomDatabase() {
    abstract fun interactionDao(): InteractionDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun behaviorPatternDao(): BehaviorPatternDao

    companion object {
        @Volatile private var INSTANCE: MemoryDatabase? = null

        fun getDatabase(context: Context): MemoryDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    MemoryDatabase::class.java,
                    "buddy_memory.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
