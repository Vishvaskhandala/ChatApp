package com.example.chatapp.data.model

import com.google.firebase.database.IgnoreExtraProperties

/**
 * Represents a single chat message, either text or image.
 */
@IgnoreExtraProperties
data class Message(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    // RENAME: Changed 'text' to 'message' to align with Firebase rules validation
    val message: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = 0L,
    val delivered: Boolean = false,
    val seen: Boolean = false,
    val type: String = "text"
) {
    // Required for Firebase deserialization
    constructor() : this(
        id = "",
        senderId = "",
        receiverId = "",
        message = "",
        timestamp = 0L,
        delivered = false,
        seen = false,
        type = "text"
    )
}