package com.example.chatapp.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.model.Message
import com.example.chatapp.data.model.User
import com.example.chatapp.utils.ChatUtils
import com.example.chatapp.utils.ImageUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    // Nested the UploadState sealed class inside ChatViewModel
    sealed class UploadState {
        object Idle : UploadState()
        data class InProgress(val progress: Float) : UploadState()
        object Success : UploadState()
        data class Failure(val error: Throwable) : UploadState()
    }

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _typingStatus = MutableStateFlow(false)
    val typingStatus: StateFlow<Boolean> = _typingStatus

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState

    private val _receiverUser = MutableStateFlow<User?>(null)
    val receiverUser: StateFlow<User?> = _receiverUser

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference

    private var messagesListener: ValueEventListener? = null
    private var currentChatId: String? = null

    /**
     * Generate a consistent chat ID from two user IDs.
     * Always orders them alphabetically to ensure consistency.
     */
    fun generateChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) {
            "$userId1-$userId2"
        } else {
            "$userId2-$userId1"
        }
    }

    /**
     * Get the peer's user ID from a chat ID
     */
    fun getPeerIdFromChatId(chatId: String, currentUserId: String): String {
        val parts = chatId.split("-")
        return if (parts.size == 2) {
            if (parts[0] == currentUserId) parts[1] else parts[0]
        } else {
            ""
        }
    }

    fun getReceiverUser(receiverId: String) {
        if (receiverId.isBlank()) {
            _error.value = "Invalid receiver ID."
            return
        }
        database.child("users").child(receiverId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _receiverUser.value = snapshot.getValue(User::class.java)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to get receiver user: ", error.toException())
                _error.value = "Failed to load user details."
            }
        })
    }

    fun getMessages(chatId: String) {
        if (chatId.isBlank()) {
            _error.value = "Invalid chat ID."
            return
        }

        // Remove previous listener if exists
        currentChatId?.let { oldChatId ->
            messagesListener?.let { listener ->
                database.child("chats").child(oldChatId).removeEventListener(listener)
            }
        }

        currentChatId = chatId
        messagesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messageList = snapshot.children.mapNotNull {
                    it.getValue(Message::class.java)
                }.sortedBy { it.timestamp }
                _messages.value = messageList
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Message listener cancelled: ", error.toException())
                _error.value = "You do not have permission to view this chat."
            }
        }

        database.child("chats").child(chatId).addValueEventListener(messagesListener!!)
    }

    /**
     * Send a text message and update both users' chat lists
     */
    fun sendMessage(chatId: String, text: String) {
        val senderId = auth.currentUser?.uid
        if (senderId == null) {
            _error.value = "You must be logged in to send a message."
            return
        }

        if (text.isBlank()) {
            _error.value = "Message cannot be empty."
            return
        }

        _isSending.value = true

        // Get receiver ID from chat ID
        val receiverId = getPeerIdFromChatId(chatId, senderId)

        if (receiverId.isBlank()) {
            _error.value = "Invalid chat ID format."
            _isSending.value = false
            return
        }

        val messageId = database.child("chats").child(chatId).push().key
        if (messageId.isNullOrEmpty()) {
            _error.value = "Failed to create message ID."
            _isSending.value = false
            return
        }

        val timestamp = System.currentTimeMillis()

        // Create message data as a Map for multi-path update
        val messageData = mapOf(
            "id" to messageId,
            "senderId" to senderId,
            "receiverId" to receiverId,
            // Uses 'message' key to satisfy Firebase rules
            "message" to text,
            "timestamp" to timestamp,
            "type" to "text",
            "read" to false
        )

        // Create userChats update data for current user
        val senderChatUpdate = mapOf(
            "lastMessage" to text,
            "timestamp" to timestamp,
            "userId" to receiverId,
            "chatId" to chatId,
            "unreadCount" to 0
        )

        // Create userChats update data for receiver
        val receiverChatUpdate = mapOf(
            "lastMessage" to text,
            "timestamp" to timestamp,
            "userId" to senderId,
            "chatId" to chatId
        )

        // Build multi-path update
        val updates = hashMapOf<String, Any>(
            // Add message to chat
            "/chats/$chatId/$messageId" to messageData,
            // Update sender's chat list
            "/userChats/$senderId/$chatId" to senderChatUpdate,
            // Update receiver's chat list
            "/userChats/$receiverId/$chatId" to receiverChatUpdate
        )

        // Execute atomic multi-path update
        database.updateChildren(updates)
            .addOnSuccessListener {
                Log.d(TAG, "Message sent successfully")
                _isSending.value = false

                // Increment unread count for receiver separately
                incrementUnreadCount(receiverId, chatId)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to send message: ", exception)
                _error.value = "Message failed to send. Check your connection or permissions."
                _isSending.value = false
            }
    }

    /**
     * Increment unread count for receiver
     */
    private fun incrementUnreadCount(receiverId: String, chatId: String) {
        database.child("userChats").child(receiverId).child(chatId).child("unreadCount")
            .get()
            .addOnSuccessListener { snapshot ->
                val currentCount = snapshot.getValue(Int::class.java) ?: 0
                database.child("userChats").child(receiverId).child(chatId).child("unreadCount")
                    .setValue(currentCount + 1)
            }
            .addOnFailureListener {
                // If permission is denied here, it confirms the rules need adjustment (though we already updated them)
                Log.w(TAG, "Failed to increment unread count for $receiverId", it)
            }
    }

    /**
     * Reset unread count when user opens chat
     */
    fun resetUnreadCount(chatId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        database.child("userChats").child(currentUserId).child(chatId).child("unreadCount")
            .setValue(0)
    }

    /**
     * Upload image and send as message
     */
    fun uploadImage(context: Context, chatId: String, imageUri: Uri) {
        val senderId = auth.currentUser?.uid
        if (senderId == null) {
            _error.value = "You must be logged in to upload an image."
            return
        }

        val messageId = database.child("chats").child(chatId).push().key
        if (messageId.isNullOrEmpty()) {
            _error.value = "Failed to create message ID."
            return
        }

        val receiverId = getPeerIdFromChatId(chatId, senderId)
        if (receiverId.isBlank()) {
            _error.value = "Invalid chat ID format."
            return
        }

        val imageRef = storage.child("chat_images/$chatId/$messageId.jpg")
        val compressedImage = ImageUtils.compressImage(context, imageUri)

        _uploadState.value = UploadState.InProgress(0f)

        imageRef.putBytes(compressedImage)
            .addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toFloat() / 100f
                _uploadState.value = UploadState.InProgress(progress)
            }
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                imageRef.downloadUrl
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    sendImageMessage(chatId, senderId, receiverId, messageId, downloadUri.toString())
                } else {
                    _uploadState.value = UploadState.Failure(task.exception ?: Exception("Upload failed"))
                }
            }
    }

    /**
     * Send image message with multi-path update
     */
    private fun sendImageMessage(
        chatId: String,
        senderId: String,
        receiverId: String,
        messageId: String,
        imageUrl: String
    ) {
        val timestamp = System.currentTimeMillis()

        val messageData = mapOf(
            "id" to messageId,
            "senderId" to senderId,
            "receiverId" to receiverId,
            "imageUrl" to imageUrl,
            "message" to "📷 Image", // Uses 'message' key
            "timestamp" to timestamp,
            "type" to "image",
            "read" to false
        )

        val senderChatUpdate = mapOf(
            "lastMessage" to "📷 Image",
            "timestamp" to timestamp,
            "userId" to receiverId,
            "chatId" to chatId,
            "unreadCount" to 0
        )

        val receiverChatUpdate = mapOf(
            "lastMessage" to "📷 Image",
            "timestamp" to timestamp,
            "userId" to senderId,
            "chatId" to chatId
        )

        val updates = hashMapOf<String, Any>(
            "/chats/$chatId/$messageId" to messageData,
            "/userChats/$senderId/$chatId" to senderChatUpdate,
            "/userChats/$receiverId/$chatId" to receiverChatUpdate
        )

        database.updateChildren(updates)
            .addOnSuccessListener {
                _uploadState.value = UploadState.Success
                incrementUnreadCount(receiverId, chatId)
            }
            .addOnFailureListener {
                _uploadState.value = UploadState.Failure(it)
            }
    }

    /**
     * Update typing status
     */
    fun setTypingStatus(chatId: String, isTyping: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: return
        database.child("typing").child(chatId).child(currentUserId).setValue(isTyping)
    }

    /**
     * Listen for peer's typing status
     */
    fun listenForTypingStatus(chatId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val peerId = getPeerIdFromChatId(chatId, currentUserId)

        database.child("typing").child(chatId).child(peerId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    _typingStatus.value = snapshot.getValue(Boolean::class.java) ?: false
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Typing status listener cancelled", error.toException())
                }
            })
    }

    /**
     * Mark all messages in chat as read
     */
    fun markMessagesAsRead(chatId: String) {
        val currentUserId = auth.currentUser?.uid ?: return

        database.child("chats").child(chatId).get()
            .addOnSuccessListener { snapshot ->
                val updates = hashMapOf<String, Any>()

                snapshot.children.forEach { messageSnapshot ->
                    val senderId = messageSnapshot.child("senderId").getValue(String::class.java)
                    val isRead = messageSnapshot.child("read").getValue(Boolean::class.java) ?: false

                    // Mark as read only if message is from peer and not already read
                    if (senderId != currentUserId && !isRead) {
                        updates["/chats/$chatId/${messageSnapshot.key}/read"] = true
                    }
                }

                if (updates.isNotEmpty()) {
                    database.updateChildren(updates)
                }

                // Reset unread count
                resetUnreadCount(chatId)
            }
    }

    fun resetError() {
        _error.value = null
    }

    fun resetUploadState() {
        _uploadState.value = UploadState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up listener
        currentChatId?.let { chatId ->
            messagesListener?.let { listener ->
                database.child("chats").child(chatId).removeEventListener(listener)
            }
        }
    }
}