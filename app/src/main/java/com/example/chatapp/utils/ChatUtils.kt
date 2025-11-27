package com.example.chatapp.utils

/**
 * A utility object for chat-related helper functions.
 */
object ChatUtils {

    /**
     * Creates a consistent, unique chat ID for a one-to-one chat between two users.
     *
     * @param userId1 The ID of the first user.
     * @param userId2 The ID of the second user.
     * @return A unique string ID for the chat, created by sorting the user IDs alphabetically and joining them with an underscore.
     */
    fun getChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
    }
}
