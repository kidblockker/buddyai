package com.buddy.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID

class VoiceEngine(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsReady = false
    private var currentUtteranceId: String? = null

    // Callbacks
    var onSpeechStarted: (() -> Unit)? = null
    var onSpeechFinished: (() -> Unit)? = null
    var onRecognitionResult: ((String) -> Unit)? = null
    var onRecognitionError: ((String) -> Unit)? = null
    var onPartialResult: ((String) -> Unit)? = null

    init {
        initTTS()
        initSTT()
    }

    private fun initTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                // Deep, calm, slightly slow voice settings
                tts?.setPitch(0.72f)       // Lower pitch = deeper voice
                tts?.setSpeechRate(0.88f)  // Slightly slower = more deliberate
                isTtsReady = true

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        onSpeechStarted?.invoke()
                    }
                    override fun onDone(utteranceId: String?) {
                        onSpeechFinished?.invoke()
                    }
                    override fun onError(utteranceId: String?) {
                        onSpeechFinished?.invoke()
                    }
                })
            }
        }
    }

    private fun initSTT() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    if (text.isNotBlank()) {
                        onRecognitionResult?.invoke(text)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = partial?.firstOrNull() ?: ""
                    if (text.isNotBlank()) {
                        onPartialResult?.invoke(text)
                    }
                }

                override fun onError(error: Int) {
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "No microphone permission"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error — voice needs internet"
                        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                        else -> "Unknown error $error"
                    }
                    onRecognitionError?.invoke(msg)
                }

                override fun onBeginningOfSpeech() {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onRmsChanged(rmsdB: Float) {}
            })
        }
    }

    fun speak(text: String, language: String = "en") {
        if (!isTtsReady) return

        // Set language based on detected language
        val locale = when {
            language.startsWith("hi") -> Locale("hi", "IN")
            language.startsWith("bn") -> Locale("bn", "IN")
            else -> Locale.US
        }
        tts?.language = locale

        stopSpeaking()
        val utteranceId = UUID.randomUUID().toString()
        currentUtteranceId = utteranceId
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun isSpeaking() = tts?.isSpeaking == true

    fun startListening(language: String = "en-IN") {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000)
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("VoiceEngine", "Start listening failed: ${e.message}")
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    fun detectLanguage(text: String): String {
        return when {
            text.any { it.code in 0x0900..0x097F } -> "hi"  // Devanagari (Hindi)
            text.any { it.code in 0x0980..0x09FF } -> "bn"  // Bengali
            else -> "en"
        }
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}
