package com.buddy.app.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.buddy.app.BuddyApplication
import com.buddy.app.MainActivity
import com.buddy.app.R
import com.buddy.app.memory.MemoryRepository
import kotlinx.coroutines.*
import java.util.Calendar

class AutonomousThinkingService : Service() {

    companion object {
        private const val NOTIF_ID = 102
        private const val NOTIF_ALERT_ID = 103
        const val EXTRA_MESSAGE = "message"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var memoryRepository: MemoryRepository
    private var checkJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        memoryRepository = (application as BuddyApplication).memoryRepository
        startForeground(NOTIF_ID, buildForegroundNotification())
        startPeriodicChecks()
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, BuddyApplication.CHANNEL_AUTONOMOUS)
            .setSmallIcon(R.drawable.ic_buddy_logo)
            .setContentTitle("Buddy")
            .setContentText("Active in background")
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun startPeriodicChecks() {
        checkJob = serviceScope.launch {
            delay(60_000)
            while (isActive) {
                try { performAutonomousCheck() } catch (e: Exception) { /* silent */ }
                delay(45 * 60 * 1000L)
            }
        }
    }

    private suspend fun performAutonomousCheck() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val suggestion = when {
            hour in 6..8 -> listOf(
                "Morning. Before you dive into the phone — drink some water first.",
                "Good morning. What's the one thing you need to get done today?",
                "You're awake. Don't check social media for the first 30 minutes — your brain will thank you."
            ).random()
            hour in 12..13 -> listOf(
                "Lunch time. Step away from whatever you're doing — even if you think you can't.",
                "Midday check: eat something real. Your decision-making is declining from this morning."
            ).random()
            hour >= 22 -> listOf(
                "It's late. What's keeping you up — work or avoidance? Be honest.",
                "Past 10 PM. Whatever's on your mind can mostly wait 8 hours."
            ).random()
            hour < 5 -> "It's past midnight. Your body is running on borrowed time right now."
            dayOfWeek == Calendar.MONDAY && hour in 9..10 ->
                "Monday morning. What are your top 3 priorities this week? Lock them in before everything else takes over."
            Math.random() > 0.7 -> listOf(
                "Quick check-in: what have you actually accomplished in the last 2 hours?",
                "Is what you're doing right now your highest priority task?",
                "Your attention is your most valuable resource. What is it pointed at?"
            ).random()
            else -> return
        }
        showProactiveNotification(suggestion)
    }

    private fun showProactiveNotification(message: String) {
        val intent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_MESSAGE, message)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, BuddyApplication.CHANNEL_NOTIFICATION)
            .setSmallIcon(R.drawable.ic_buddy_logo)
            .setContentTitle("Buddy")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentText(message)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()

        getSystemService(NotificationManager::class.java).notify(NOTIF_ALERT_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { checkJob?.cancel(); serviceScope.cancel(); super.onDestroy() }
}
