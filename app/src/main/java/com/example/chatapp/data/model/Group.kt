package com.example.chatapp.data.model

/**
 * Represents a chat group.
 */
data class Group(
    val groupId: String = "",
    val name: String = "",
    val profileImageUrl: String = "",
    val description: String = "",
    val admin: String = "",  // Admin user ID
    val createdAt: Long = 0L,
    val members: Map<String, Boolean> = emptyMap(), // userId -> isMember
    val lastMessage: String? = null,
    val lastMessageTime: Long = 0L
)
