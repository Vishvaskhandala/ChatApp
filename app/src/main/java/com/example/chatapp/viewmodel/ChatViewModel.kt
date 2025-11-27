package com.example.chatapp.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.chatapp.data.model.Message
import com.example.chatapp.data.model.User
import com.example.chatapp.utils.ImageUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _typingStatus = MutableStateFlow(false)
    val typingStatus: StateFlow<Boolean> = _typingStatus

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState

    private val _receiverUser = MutableStateFlow<User?>(null)
    val receiverUser: StateFlow<User?> = _receiverUser

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference

    fun getReceiverUser(receiverId: String) {
        database.child("users").child(receiverId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _receiverUser.value = snapshot.getValue(User::class.java)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    fun getMessages(receiverId: String) {
        val senderId = auth.currentUser?.uid ?: return
        val chatId = getChatId(senderId, receiverId)

        database.child("chats").child(chatId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messageList = snapshot.children.mapNotNull {
                    it.getValue(Message::class.java)
                }
                _messages.value = messageList
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    fun listenForTypingStatus(receiverId: String) {
        val senderId = auth.currentUser?.uid ?: return
        val chatId = getChatId(senderId, receiverId)
        database.child("typing").child(chatId).child(receiverId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _typingStatus.value = snapshot.getValue(Boolean::class.java) ?: false
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    fun sendMessage(receiverId: String, text: String) {
        val senderId = auth.currentUser?.uid ?: return
        val chatId = getChatId(senderId, receiverId)
        val messageId = database.child("chats").child(chatId).push().key ?: return

        val message = Message(
            id = messageId,
            senderId = senderId,
            receiverId = receiverId,
            text = text,
            timestamp = System.currentTimeMillis(),
            type = "text"
        )
        database.child("chats").child(chatId).child(messageId).setValue(message)
    }

    fun uploadImage(context: Context, receiverId: String, imageUri: Uri) {
        val senderId = auth.currentUser?.uid ?: return
        val chatId = getChatId(senderId, receiverId)
        val messageId = database.child("chats").child(chatId).push().key ?: return
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
                    task.exception?.let {
                        throw it
                    }
                }
                imageRef.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    val message = Message(
                        id = messageId,
                        senderId = senderId,
                        receiverId = receiverId,
                        imageUrl = downloadUri.toString(),
                        timestamp = System.currentTimeMillis(),
                        type = "image"
                    )
                    database.child("chats").child(chatId).child(messageId).setValue(message)
                    _uploadState.value = UploadState.Success
                } else {
                    _uploadState.value = UploadState.Failure(task.exception!!)
                }
            }
    }

    fun setTypingStatus(receiverId: String, isTyping: Boolean) {
        val senderId = auth.currentUser?.uid ?: return
        val chatId = getChatId(senderId, receiverId)
        database.child("typing").child(chatId).child(senderId).setValue(isTyping)
    }

    fun markMessagesAsSeen(receiverId: String) {
        val senderId = auth.currentUser?.uid ?: return
        val chatId = getChatId(senderId, receiverId)
        database.child("chats").child(chatId).orderByChild("receiverId").equalTo(senderId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach {
                        val message = it.getValue(Message::class.java)
                        if (message != null && !message.seen) {
                            it.ref.child("seen").setValue(true)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    private fun getChatId(user1: String, user2: String): String {
        return if (user1 > user2) "$user1-$user2" else "$user2-$user1"
    }
}