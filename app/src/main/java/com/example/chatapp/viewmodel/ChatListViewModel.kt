package com.example.chatapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.model.ChatItem
import com.example.chatapp.data.model.Group
import com.example.chatapp.data.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatListViewModel : ViewModel() {

    private val _chats = MutableStateFlow<List<ChatItem>>(emptyList())
    private val _searchQuery = MutableStateFlow("")

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val chats: StateFlow<List<ChatItem>> = _searchQuery
        .debounce(300)
        .combine(_chats) { query, chats ->
            if (query.isBlank()) {
                chats
            } else {
                chats.filter { it.name.contains(query, ignoreCase = true) }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    init {
        getChats()
    }
    fun fetchChats() {
        getChats()
    }

    fun getChats() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val userId = auth.currentUser?.uid ?: return@launch

            // Fetch individual chats
            database.child("userChats").child(userId).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val individualChats = snapshot.children.mapNotNull {
                        it.getValue(ChatItem::class.java)?.copy(isGroup = false)
                    }

                    // Fetch group chats
                    database.child("user-groups").child(userId).addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(groupSnapshot: DataSnapshot) {
                            val groupIds = groupSnapshot.children.mapNotNull { it.key }
                            val groupChats = mutableListOf<ChatItem>()

                            groupIds.forEach { groupId ->
                                database.child("groups").child(groupId).get().addOnSuccessListener {
                                    val group = it.getValue(Group::class.java)
                                    database.child("groupMessages").child(groupId).limitToLast(1).get().addOnSuccessListener { messageSnapshot ->
                                        val lastMessage = messageSnapshot.children.firstOrNull()?.getValue(Message::class.java)
                                        group?.let {
                                            groupChats.add(
                                                ChatItem(
                                                    id = it.groupId,
                                                    name = it.name,
                                                    profileImage = it.profileImageUrl,
                                                    isGroup = true,
                                                    lastMessage = lastMessage?.text ?: lastMessage?.imageUrl ?: "",
                                                    timestamp = lastMessage?.timestamp ?: it.createdAt,
                                                    unreadCount = 0, // TODO: Implement unread count
                                                    isOnline = false
                                                )
                                            )
                                        }
                                        _chats.value = (individualChats + groupChats).sortedByDescending { it.timestamp }
                                    }
                                }
                            }
                            _isRefreshing.value = false
                        }

                        override fun onCancelled(error: DatabaseError) {
                            _isRefreshing.value = false
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    _isRefreshing.value = false
                }
            })
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun deleteChat(chatId: String, isGroup: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (isGroup) {
            database.child("groups").child(chatId).child("members").child(currentUserId).removeValue()
            database.child("user-groups").child(currentUserId).child(chatId).removeValue()
        } else {
            database.child("userChats").child(currentUserId).child(chatId).removeValue()
        }
    }

    fun setOnlineStatus(isOnline: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        database.child("users").child(userId).child("online").setValue(isOnline)
        if (!isOnline) {
            database.child("users").child(userId).child("lastSeen").setValue(System.currentTimeMillis())
        }
    }

    fun logout() {
        setOnlineStatus(false)
        auth.signOut()
    }
}