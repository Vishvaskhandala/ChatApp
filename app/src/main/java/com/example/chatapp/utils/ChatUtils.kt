package com.example.chatapp.utils

object ChatUtils {

    /**
     * Creates a consistent, sorted chat ID from two user IDs.
     * @param user1 The first user's ID.
     * @param user2 The second user's ID.
     * @return A chat ID string in the format "higherId-lowerId".
     */
    fun createChatId(user1: String, user2: String): String {
        return if (user1 > user2) "$user1-$user2" else "$user2-$user1"
    }

    /**
     * Extracts the other user's (peer) ID from a chat ID.
     * @param chatId The chat ID (e.g., "higherId-lowerId").
     * @param currentUserId The ID of the current user.
     * @return The ID of the other user in the chat.
     */
    fun getPeerId(chatId: String, currentUserId: String): String {
        return chatId.replace(currentUserId, "").replace("-", "")
    }
}
