package com.buddy.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.buddy.app.BuddyApplication
import com.buddy.app.ai.BuddyAI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class BuddyUiState {
    object Idle : BuddyUiState()
    object Listening : BuddyUiState()
    object Thinking : BuddyUiState()
    object Speaking : BuddyUiState()
}

class BuddyViewModel(application: Application) : AndroidViewModel(application) {
    private val memoryRepository = (application as BuddyApplication).memoryRepository

    private val _uiState = MutableStateFlow<BuddyUiState>(BuddyUiState.Idle)
    val uiState: StateFlow<BuddyUiState> = _uiState

    private val _messages = MutableStateFlow<List<BuddyMessage>>(emptyList())
    val messages: StateFlow<List<BuddyMessage>> = _messages

    private var buddyAI: BuddyAI? = null

    fun initAI(apiKey: String) {
        buddyAI = BuddyAI(memoryRepository, apiKey)
    }

    fun updateApiKey(key: String) {
        buddyAI?.updateApiKey(key)
    }

    fun setState(state: BuddyUiState) {
        _uiState.value = state
    }

    fun addMessage(message: BuddyMessage) {
        _messages.value = _messages.value + message
    }

    fun clearMessages() {
        _messages.value = emptyList()
        buddyAI?.clearHistory()
    }

    suspend fun sendToAI(text: String, imageBase64: String? = null): String {
        return buddyAI?.chat(text, imageBase64) ?: "I need an API key to think. Set it up in Settings."
    }
}
