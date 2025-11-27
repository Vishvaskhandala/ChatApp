package com.example.chatapp.data.model

/**
 * Represents a single chat message, either text or image.
 */
data class Message(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = 0L,
    val delivered: Boolean = false,
    val seen: Boolean = false,
    val type: String = "text"
)