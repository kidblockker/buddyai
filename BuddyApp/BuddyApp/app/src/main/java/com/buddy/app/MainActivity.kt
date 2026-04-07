package com.buddy.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.buddy.app.ai.BuddyAI
import com.buddy.app.camera.CameraHelper
import com.buddy.app.databinding.ActivityMainBinding
import com.buddy.app.memory.MemoryRepository
import com.buddy.app.service.AutonomousThinkingService
import com.buddy.app.ui.BuddyMessage
import com.buddy.app.ui.MessageAdapter
import com.buddy.app.ui.WaveformView
import com.buddy.app.voice.VoiceEngine
import com.buddy.app.voice.WakeWordService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var voiceEngine: VoiceEngine
    private lateinit var buddyAI: BuddyAI
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var memoryRepository: MemoryRepository
    private lateinit var cameraHelper: CameraHelper

    private var isVoiceMode = false
    private var isCameraActive = false
    private var pendingCameraCapture = false
    private var capturedImageBase64: String? = null

    private val wakeWordReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == WakeWordService.ACTION_WAKE_WORD_DETECTED) {
                onWakeWordDetected()
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true

        if (audioGranted) {
            startWakeWordService()
        }
        if (!audioGranted) {
            Toast.makeText(this, "Microphone permission needed for voice features", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        memoryRepository = (application as BuddyApplication).memoryRepository
        cameraHelper = CameraHelper(this)

        initBuddyAI()
        initVoiceEngine()
        initUI()
        requestPermissions()
        checkFirstLaunch()
        handleIncomingIntent(intent)

        // Start autonomous thinking service
        startService(Intent(this, AutonomousThinkingService::class.java))

        // Session tracking
        lifecycleScope.launch { memoryRepository.incrementSessionCount() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val message = intent?.getStringExtra(AutonomousThinkingService.EXTRA_MESSAGE)
        if (!message.isNullOrBlank()) {
            // Show Buddy's proactive message
            addBuddyMessage(message)
            voiceEngine.speak(message)
        }
    }

    private fun initBuddyAI() {
        val apiKey = getSharedPreferences("buddy_prefs", MODE_PRIVATE)
            .getString("api_key", "") ?: ""
        buddyAI = BuddyAI(memoryRepository, apiKey)
    }

    private fun initVoiceEngine() {
        voiceEngine = VoiceEngine(this)

        voiceEngine.onSpeechStarted = {
            runOnUiThread {
                binding.waveform.setState(WaveformView.State.SPEAKING)
                binding.tvStatus.text = "Speaking..."
            }
        }

        voiceEngine.onSpeechFinished = {
            runOnUiThread {
                binding.waveform.setState(WaveformView.State.IDLE)
                binding.tvStatus.text = "Say 'Hey Buddy' or tap mic"
                if (isVoiceMode) {
                    // Ready for next input
                    isVoiceMode = false
                }
            }
        }

        voiceEngine.onRecognitionResult = { text ->
            runOnUiThread {
                binding.waveform.setState(WaveformView.State.IDLE)
                binding.etInput.setText("")
                if (text.isNotBlank()) {
                    sendMessage(text, capturedImageBase64)
                    capturedImageBase64 = null
                }
                isVoiceMode = false
            }
        }

        voiceEngine.onPartialResult = { partial ->
            runOnUiThread {
                binding.etInput.hint = partial
            }
        }

        voiceEngine.onRecognitionError = { error ->
            runOnUiThread {
                binding.waveform.setState(WaveformView.State.IDLE)
                binding.tvStatus.text = "Say 'Hey Buddy' or tap mic"
                if (error != "Didn't catch that" && error != "No speech detected") {
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                }
                isVoiceMode = false
            }
        }
    }

    private fun initUI() {
        // RecyclerView
        messageAdapter = MessageAdapter()
        binding.rvMessages.apply {
            adapter = messageAdapter
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
        }

        // Waveform initial state
        binding.waveform.setState(WaveformView.State.IDLE)

        // Mic button
        binding.btnMic.setOnClickListener {
            if (isVoiceMode) {
                stopListening()
            } else {
                startListening()
            }
        }

        // Send button
        binding.btnSend.setOnClickListener {
            val text = binding.etInput.text.toString().trim()
            if (text.isNotBlank()) {
                binding.etInput.setText("")
                sendMessage(text, capturedImageBase64)
                capturedImageBase64 = null
            }
        }

        // Camera button
        binding.btnCamera.setOnClickListener {
            toggleCamera()
        }

        // Capture photo button
        binding.btnCapture.setOnClickListener {
            captureAndDescribe()
        }

        // Settings
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Input text watcher
        binding.etInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                binding.btnSend.isEnabled = s?.isNotBlank() == true
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Long press on waveform to clear conversation
        binding.waveform.setOnLongClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear conversation?")
                .setMessage("This will clear the current chat history. Memory is preserved.")
                .setPositiveButton("Clear") { _, _ ->
                    messageAdapter.clear()
                    buddyAI.clearHistory()
                    binding.tvStatus.text = "Fresh start. What's on your mind?"
                }
                .setNegativeButton("Never mind", null)
                .show()
            true
        }
    }

    private fun startListening() {
        if (!isVoiceMode) {
            isVoiceMode = true
            binding.waveform.setState(WaveformView.State.LISTENING)
            binding.tvStatus.text = "Listening..."
            binding.btnMic.setImageResource(android.R.drawable.ic_media_pause)
            vibrate(50)
            voiceEngine.stopSpeaking()
            voiceEngine.startListening()
        }
    }

    private fun stopListening() {
        isVoiceMode = false
        voiceEngine.stopListening()
        binding.waveform.setState(WaveformView.State.IDLE)
        binding.tvStatus.text = "Say 'Hey Buddy' or tap mic"
        binding.btnMic.setImageResource(android.R.drawable.ic_btn_speak_now)
    }

    private fun onWakeWordDetected() {
        runOnUiThread {
            vibrate(100)
            Toast.makeText(this, "Hey!", Toast.LENGTH_SHORT).show()
            startListening()
        }
    }

    private fun sendMessage(text: String, imageBase64: String? = null) {
        // Add user message to chat
        messageAdapter.addMessage(BuddyMessage(text = text, isUser = true, hasImage = imageBase64 != null))
        scrollToBottom()

        binding.waveform.setState(WaveformView.State.THINKING)
        binding.tvStatus.text = "Thinking..."
        binding.btnSend.isEnabled = false

        // Get API key (may have been updated in settings)
        val apiKey = getSharedPreferences("buddy_prefs", MODE_PRIVATE)
            .getString("api_key", "") ?: ""
        buddyAI.updateApiKey(apiKey)

        lifecycleScope.launch {
            val response = withContext(Dispatchers.IO) {
                buddyAI.chat(text, imageBase64)
            }

            withContext(Dispatchers.Main) {
                addBuddyMessage(response)

                // Detect language and speak
                val lang = voiceEngine.detectLanguage(text)
                voiceEngine.speak(response, lang)

                binding.btnSend.isEnabled = true
            }
        }
    }

    private fun addBuddyMessage(text: String) {
        messageAdapter.addMessage(BuddyMessage(text = text, isUser = false))
        scrollToBottom()
        binding.waveform.setState(WaveformView.State.SPEAKING)
        binding.tvStatus.text = "Buddy"
    }

    private fun scrollToBottom() {
        binding.rvMessages.post {
            if (messageAdapter.itemCount > 0) {
                binding.rvMessages.smoothScrollToPosition(messageAdapter.itemCount - 1)
            }
        }
    }

    private fun toggleCamera() {
        if (isCameraActive) {
            binding.cameraPreview.visibility = View.GONE
            binding.btnCapture.visibility = View.GONE
            binding.capturedIndicator.visibility = View.GONE
            cameraHelper.stopCamera()
            isCameraActive = false
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                binding.cameraPreview.visibility = View.VISIBLE
                binding.btnCapture.visibility = View.VISIBLE
                isCameraActive = true
                cameraHelper.startCamera(this, binding.cameraPreview) {
                    // Camera ready
                }
            } else {
                Toast.makeText(this, "Camera permission needed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun captureAndDescribe() {
        cameraHelper.capturePhoto(
            onCaptured = { base64 ->
                runOnUiThread {
                    capturedImageBase64 = base64
                    binding.capturedIndicator.visibility = View.VISIBLE
                    binding.capturedIndicator.text = "📷 Image captured — ask Buddy what he sees"
                    binding.etInput.hint = "What do you want to know about this?"
                    // Default question
                    sendMessage("Buddy, look at this and tell me what you see. Describe it like a human, not a machine.", base64)
                    capturedImageBase64 = null
                    toggleCamera()
                }
            },
            onError = { error ->
                runOnUiThread { Toast.makeText(this, error, Toast.LENGTH_SHORT).show() }
            }
        )
    }

    private fun checkFirstLaunch() {
        val prefs = getSharedPreferences("buddy_prefs", MODE_PRIVATE)
        val isFirst = prefs.getBoolean("first_launch", true)
        if (isFirst) {
            prefs.edit().putBoolean("first_launch", false).apply()
            showWelcomeDialog()
        } else {
            val apiKey = prefs.getString("api_key", "") ?: ""
            if (apiKey.isBlank()) {
                showApiKeyDialog()
            }
        }
    }

    private fun showWelcomeDialog() {
        AlertDialog.Builder(this)
            .setTitle("I'm Buddy.")
            .setMessage("Not your typical AI assistant.\n\nI'll be straight with you, challenge you when needed, and remember what matters. But first — I need your Anthropic API key to think properly.")
            .setPositiveButton("Set up now") { _, _ ->
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            .setNegativeButton("Later", null)
            .setCancelable(false)
            .show()
    }

    private fun showApiKeyDialog() {
        AlertDialog.Builder(this)
            .setTitle("API Key Missing")
            .setMessage("I need an Anthropic API key to function. Get one at console.anthropic.com, then add it in Settings.")
            .setPositiveButton("Settings") { _, _ ->
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        } else {
            startWakeWordService()
        }
    }

    private fun startWakeWordService() {
        if (!WakeWordService.isRunning) {
            val intent = Intent(this, WakeWordService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    private fun vibrate(ms: Long) {
        val vibrator = getSystemService(Vibrator::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(ms)
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(WakeWordService.ACTION_WAKE_WORD_DETECTED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wakeWordReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(wakeWordReceiver, filter)
        }

        // Refresh API key in case settings were updated
        val apiKey = getSharedPreferences("buddy_prefs", MODE_PRIVATE)
            .getString("api_key", "") ?: ""
        buddyAI.updateApiKey(apiKey)
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(wakeWordReceiver) } catch (e: Exception) { /* ignore */ }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceEngine.destroy()
        cameraHelper.destroy()
    }
}
