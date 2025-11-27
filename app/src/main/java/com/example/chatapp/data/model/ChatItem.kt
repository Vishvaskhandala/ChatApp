package com.example.chatapp.data.model

/**
 * A data class to represent an item in the chat list, which can be either a one-to-one chat or a group chat.
 */
data class ChatItem(
    val id: String,
    val name: String,
    val profileImage: String,
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int,
    val isOnline: Boolean,
    val isGroup: Boolean
)
