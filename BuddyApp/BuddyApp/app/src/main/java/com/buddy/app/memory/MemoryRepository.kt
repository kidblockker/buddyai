package com.buddy.app.memory

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MemoryRepository(context: Context) {

    private val db = MemoryDatabase.getDatabase(context)
    private val interactionDao = db.interactionDao()
    private val profileDao = db.userProfileDao()
    private val patternDao = db.behaviorPatternDao()

    // Profile keys
    companion object {
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_AGE = "user_age"
        const val KEY_USER_OCCUPATION = "user_occupation"
        const val KEY_USER_CITY = "user_city"
        const val KEY_LAST_ACTIVE = "last_active"
        const val KEY_SESSION_COUNT = "session_count"
        const val KEY_PREFERRED_LANGUAGE = "preferred_language"
        const val KEY_PERSONALITY_NOTES = "personality_notes"
    }

    suspend fun saveInteraction(userMsg: String, buddyResponse: String) {
        withContext(Dispatchers.IO) {
            interactionDao.insert(
                InteractionEntity(
                    userMessage = userMsg,
                    buddyResponse = buddyResponse
                )
            )
            // Keep only last 500 interactions
            val count = interactionDao.getCount()
            if (count > 500) {
                interactionDao.deleteOldest(count - 500)
            }
            setProfile(KEY_LAST_ACTIVE, System.currentTimeMillis().toString())
        }
    }

    suspend fun getMemoryContextString(): String {
        return withContext(Dispatchers.IO) {
            val profile = profileDao.getAll()
            val recentInteractions = interactionDao.getLastN(10)
            val patterns = patternDao.getRecent()

            val sb = StringBuilder()

            if (profile.isNotEmpty()) {
                sb.append("USER PROFILE:\n")
                profile.forEach { p ->
                    val label = p.key.replace("_", " ").replaceFirstChar { it.uppercase() }
                    sb.append("- $label: ${p.value}\n")
                }
                sb.append("\n")
            }

            if (patterns.isNotEmpty()) {
                sb.append("OBSERVED PATTERNS:\n")
                patterns.forEach { p ->
                    sb.append("- [${p.type}] ${p.observation} (observed ${p.count}x)\n")
                }
                sb.append("\n")
            }

            if (recentInteractions.isNotEmpty()) {
                sb.append("RECENT CONVERSATION CONTEXT:\n")
                recentInteractions.takeLast(5).forEach { i ->
                    val date = SimpleDateFormat("MMM dd HH:mm", Locale.getDefault())
                        .format(Date(i.timestamp))
                    sb.append("[$date] User: ${i.userMessage.take(80)}\n")
                }
            }

            if (sb.isEmpty()) {
                "No memory yet. This appears to be a new user."
            } else {
                sb.toString()
            }
        }
    }

    suspend fun setProfile(key: String, value: String) {
        withContext(Dispatchers.IO) {
            profileDao.set(UserProfileEntity(key = key, value = value))
        }
    }

    suspend fun getProfile(key: String): String? {
        return withContext(Dispatchers.IO) {
            profileDao.get(key)?.value
        }
    }

    suspend fun recordPattern(type: String, observation: String) {
        withContext(Dispatchers.IO) {
            try {
                patternDao.incrementPattern(type, observation)
            } catch (e: Exception) {
                patternDao.insert(BehaviorPatternEntity(type = type, observation = observation))
            }
        }
    }

    suspend fun incrementSessionCount() {
        withContext(Dispatchers.IO) {
            val current = getProfile(KEY_SESSION_COUNT)?.toIntOrNull() ?: 0
            setProfile(KEY_SESSION_COUNT, (current + 1).toString())
        }
    }
}
