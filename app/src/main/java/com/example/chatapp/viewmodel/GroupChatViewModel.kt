package com.example.chatapp.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.chatapp.data.model.Group
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

class GroupChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState

    private val _group = MutableStateFlow<Group?>(null)
    val group: StateFlow<Group?> = _group

    private val _senderNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val senderNames: StateFlow<Map<String, String>> = _senderNames

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference

    fun getGroupDetails(groupId: String) {
        database.child("groups").child(groupId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _group.value = snapshot.getValue(Group::class.java)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    fun getGroupMessages(groupId: String) {
        database.child("groupMessages").child(groupId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messageList = snapshot.children.mapNotNull { it.getValue(Message::class.java) }
                _messages.value = messageList
                fetchSenderNames(messageList)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun fetchSenderNames(messages: List<Message>) {
        val senderIds = messages.map { it.senderId }.distinct()
        val names = mutableMapOf<String, String>()
        senderIds.forEach { senderId ->
            database.child("users").child(senderId).child("username").get().addOnSuccessListener {
                it.getValue(String::class.java)?.let { name ->
                    names[senderId] = name
                    _senderNames.value = names.toMap()
                }
            }
        }
    }

    fun sendGroupMessage(groupId: String, text: String) {
        val senderId = auth.currentUser?.uid ?: return
        val messageId = database.child("groupMessages").child(groupId).push().key ?: return

        val message = Message(
            id = messageId,
            senderId = senderId,
            receiverId = groupId, // For group messages, store groupId here
            text = text,
            timestamp = System.currentTimeMillis(),
            type = "text"
        )
        database.child("groupMessages").child(groupId).child(messageId).setValue(message)

        // Update group's last message
        updateGroupLastMessage(groupId, text, System.currentTimeMillis())
    }

    fun uploadGroupImage(context: Context, groupId: String, imageUri: Uri) {
        val senderId = auth.currentUser?.uid ?: return
        val messageId = database.child("groupMessages").child(groupId).push().key ?: return
        val imageRef = storage.child("group_images/$groupId/$messageId.jpg")
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
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    val message = Message(
                        id = messageId,
                        senderId = senderId,
                        receiverId = groupId, // For group messages, store groupId here
                        imageUrl = downloadUri.toString(),
                        timestamp = System.currentTimeMillis(),
                        type = "image"
                    )
                    database.child("groupMessages").child(groupId).child(messageId).setValue(message)
                    _uploadState.value = UploadState.Success

                    // Update group's last message
                    updateGroupLastMessage(groupId, "📷 Photo", System.currentTimeMillis())
                } else {
                    _uploadState.value = UploadState.Failure(task.exception!!)
                }
            }
    }

    private fun updateGroupLastMessage(groupId: String, message: String, timestamp: Long) {
        val updates = mapOf(
            "lastMessage" to message,
            "lastMessageTime" to timestamp
        )
        database.child("groups").child(groupId).updateChildren(updates)
    }
}