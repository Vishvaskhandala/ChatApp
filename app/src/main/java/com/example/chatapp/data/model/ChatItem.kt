package com.example.chatapp.data.model

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

/**
 * A data class to represent an item in the chat list, which can be either a one-to-one chat or a group chat.
 */
@IgnoreExtraProperties
data class ChatItem(
    // Primary identifier - this will be chatId for individual chats, groupId for groups
    val id: String = "",

    // User/Group display name
    val name: String = "",

    // Profile image URL
    val profileImage: String = "",

    // Last message preview
    val lastMessage: String = "",

    // Timestamp of last message
    val timestamp: Long = 0L,

    // Number of unread messages
    val unreadCount: Int = 0,

    // Online status (for individual chats)
    val isOnline: Boolean = false,

    // Whether this is a group chat
    val isGroup: Boolean = false,

    // The peer's user ID (for individual chats)
    @get:PropertyName("userId")
    @set:PropertyName("userId")
    var userId: String = "",

    // The chat ID (same as id, kept for Firebase compatibility)
    val chatId: String = "",

    // Type of chat: "personal" or "group"
    val type: String = "personal"
) {
    // No-argument constructor required for Firebase deserialization
    constructor() : this(
        id = "",
        name = "",
        profileImage = "",
        lastMessage = "",
        timestamp = 0L,
        unreadCount = 0,
        isOnline = false,
        isGroup = false,
        userId = "",
        chatId = "",
        type = "personal"
    )
}