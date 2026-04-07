package com.buddy.app.voice

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.buddy.app.BuddyApplication
import com.buddy.app.MainActivity
import com.buddy.app.R
import kotlinx.coroutines.*

class WakeWordService : Service() {

    companion object {
        const val ACTION_WAKE_WORD_DETECTED = "com.buddy.app.WAKE_WORD_DETECTED"
        const val WAKE_WORDS = "hey buddy"
        private const val TAG = "WakeWordService"
        private const val NOTIF_ID = 101
        var isRunning = false
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isListening = false
    private var restartJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        startForeground(NOTIF_ID, buildNotification())
        initRecognizer()
    }

    private fun buildNotification(): Notification {
        val intent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, BuddyApplication.CHANNEL_WAKE_WORD)
            .setSmallIcon(R.drawable.ic_buddy_logo)
            .setContentTitle("Buddy is listening")
            .setContentText("Say \"Hey Buddy\" to activate")
            .setContentIntent(intent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "Speech recognition not available")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.forEach { result ->
                    if (result.lowercase().contains(WAKE_WORDS)) {
                        Log.d(TAG, "Wake word detected: $result")
                        onWakeWordDetected()
                        return
                    }
                }
                // Restart listening
                scheduleRestart(500)
            }

            override fun onPartialResults(partial: Bundle?) {
                val matches = partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.forEach { result ->
                    if (result.lowercase().contains(WAKE_WORDS)) {
                        Log.d(TAG, "Wake word in partial: $result")
                        onWakeWordDetected()
                        return
                    }
                }
            }

            override fun onError(error: Int) {
                isListening = false
                val delay = when (error) {
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 2000L
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> 5000L
                    else -> 1000L
                }
                scheduleRestart(delay)
            }

            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onBeginningOfSpeech() { isListening = true }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        startRecognition()
    }

    private fun startRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
        }
        try {
            speechRecognizer?.startListening(intent)
            isListening = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start: ${e.message}")
            scheduleRestart(1000)
        }
    }

    private fun scheduleRestart(delayMs: Long) {
        restartJob?.cancel()
        restartJob = serviceScope.launch {
            delay(delayMs)
            if (isRunning) startRecognition()
        }
    }

    private fun onWakeWordDetected() {
        isListening = false
        speechRecognizer?.stopListening()

        // Send broadcast to MainActivity
        val intent = Intent(ACTION_WAKE_WORD_DETECTED).apply {
            setPackage(packageName)
        }
        sendBroadcast(intent)

        // Resume listening after 5 seconds
        scheduleRestart(5000)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning = false
        restartJob?.cancel()
        serviceScope.cancel()
        speechRecognizer?.destroy()
        super.onDestroy()
    }
}
