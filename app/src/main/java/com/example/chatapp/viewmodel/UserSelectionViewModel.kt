package com.example.chatapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.model.User
import com.example.chatapp.utils.ChatUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserSelectionViewModel : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    private val _searchQuery = MutableStateFlow("")

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val users: StateFlow<List<User>> = _searchQuery
        .debounce(300)
        .combine(_users) { query, users ->
            if (query.isBlank()) {
                users
            } else {
                users.filter { it.name.contains(query, ignoreCase = true) }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    init {
        fetchUsers()
    }

    private fun fetchUsers() {
        _isLoading.value = true
        val currentUserId = auth.currentUser?.uid
        database.child("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userList = snapshot.children.mapNotNull {
                    it.getValue(User::class.java)
                }.filter { it.uid.isNotEmpty() && it.uid != currentUserId }
                _users.value = userList
                _isLoading.value = false
            }

            override fun onCancelled(error: DatabaseError) {
                _isLoading.value = false
                // Handle error
            }
        })
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun createChat(receiverId: String, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val senderId = auth.currentUser?.uid ?: return@launch
            val chatId = ChatUtils.createChatId(senderId, receiverId)

            val chatSnapshot = database.child("chats").child(chatId).get().await()
            if (chatSnapshot.exists()) {
                onComplete(chatId)
                return@launch
            }

            val senderUser = database.child("users").child(senderId).get().await().getValue(User::class.java)
            val receiverUser = database.child("users").child(receiverId).get().await().getValue(User::class.java)

            val userChatsRef = database.child("userChats")
            val currentUserChat = mapOf("userId" to receiverId, "name" to (receiverUser?.name ?: ""), "timestamp" to System.currentTimeMillis())
            val receiverUserChat = mapOf("userId" to senderId, "name" to (senderUser?.name ?: ""), "timestamp" to System.currentTimeMillis())

            userChatsRef.child(senderId).child(receiverId).setValue(currentUserChat)
            userChatsRef.child(receiverId).child(senderId).setValue(receiverUserChat)

            onComplete(chatId)
        }
    }
}