package com.example.chatapp.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    val uid: String = "",
    val username: String = "",
    val name: String = "",
    val email: String = "",
    val profileImage: String = "",   // <‑ URL of PNG/JPG
    val online: Boolean = false,
    val status: String = "Hey there! I'm using ChatApp",
    val lastSeen: Long = 0L,
    val createdAt: Long = 0L
) {
    constructor() : this(
        uid = "",
        username = "",
        name = "",
        email = "",
        profileImage = "",
        online = false,
        status = "Hey there! I'm using ChatApp",
        lastSeen = 0L,
        createdAt = 0L
    )
}