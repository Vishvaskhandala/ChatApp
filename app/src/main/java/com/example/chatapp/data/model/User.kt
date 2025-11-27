package com.example.chatapp.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val profileImage: String = "",
    val status: String = "",
    val online: Boolean = false,
    val lastSeen: Long = 0,
    val createdAt: Long = 0
)
