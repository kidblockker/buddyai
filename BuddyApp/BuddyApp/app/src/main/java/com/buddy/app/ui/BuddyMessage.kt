package com.buddy.app.ui

data class BuddyMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val hasImage: Boolean = false
)
