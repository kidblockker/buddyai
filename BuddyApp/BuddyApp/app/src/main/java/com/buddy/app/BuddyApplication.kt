package com.buddy.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.buddy.app.memory.MemoryRepository

class BuddyApplication : Application() {

    companion object {
        const val CHANNEL_WAKE_WORD = "buddy_wake_word"
        const val CHANNEL_AUTONOMOUS = "buddy_autonomous"
        const val CHANNEL_NOTIFICATION = "buddy_notifications"
        lateinit var instance: BuddyApplication
            private set
    }

    lateinit var memoryRepository: MemoryRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        memoryRepository = MemoryRepository(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_WAKE_WORD,
                    "Buddy Listening",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows when Buddy is listening for wake word"
                    setShowBadge(false)
                }
            )

            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_AUTONOMOUS,
                    "Buddy Thinking",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Buddy autonomous background processing"
                    setShowBadge(false)
                }
            )

            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_NOTIFICATION,
                    "Buddy Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Proactive suggestions from Buddy"
                }
            )
        }
    }
}
