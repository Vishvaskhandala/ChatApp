package com.example.chatapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.model.ChatItem
import com.example.chatapp.data.model.Group
import com.example.chatapp.data.model.Message
import com.example.chatapp.data.model.User
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
import kotlinx.coroutines.tasks.await

class ChatListViewModel : ViewModel() {

    companion object {
        private const val TAG = "ChatListViewModel"
    }

    private val _allChats = MutableStateFlow<List<ChatItem>>(emptyList())
    private val _searchQuery = MutableStateFlow("")

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val chats: StateFlow<List<ChatItem>> = _searchQuery
        .debounce(300)
        .combine(_allChats) { query, chats ->
            if (query.isBlank()) {
                chats.sortedByDescending { it.timestamp }
            } else {
                chats.filter { it.name.contains(query, ignoreCase = true) }
                    .sortedByDescending { it.timestamp }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    // Store listeners for cleanup
    private var userChatsListener: ValueEventListener? = null
    private var userGroupsListener: ValueEventListener? = null

    init {
        getChats()
    }

    fun fetchChats() {
        getChats()
    }

    fun getChats() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _error.value = "User not logged in"
            return
        }

        _isRefreshing.value = true

        // Remove previous listeners
        removeListeners()

        // Listen for individual chats
        userChatsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch {
                    try {
                        val individualChats = mutableListOf<ChatItem>()

                        for (chatSnapshot in snapshot.children) {
                            val chatId = chatSnapshot.key ?: continue
                            val lastMessage = chatSnapshot.child("lastMessage").getValue(String::class.java) ?: ""
                            val timestamp = chatSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                            val peerId = chatSnapshot.child("userId").getValue(String::class.java) ?: ""
                            val unreadCount = chatSnapshot.child("unreadCount").getValue(Int::class.java) ?: 0

                            if (peerId.isNotEmpty()) {
                                // Fetch peer user details
                                try {
                                    val userSnapshot = database.child("users").child(peerId).get().await()
                                    val peerUser = userSnapshot.getValue(User::class.java)

                                    val chatItem = ChatItem(
                                        id = chatId,
                                        // chatId for navigation // peerId for reference
                                        name = peerUser?.name ?: peerUser?.username ?: "Unknown User",
                                        profileImage = peerUser?.profileImage ?: "",
                                        lastMessage = lastMessage,
                                        timestamp = timestamp,
                                        unreadCount = unreadCount,
                                        isOnline = peerUser?.online ?: false,
                                        isGroup = false,
                                        chatId = chatId,
                                        type = "personal"
                                    )
                                    individualChats.add(chatItem)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error fetching user $peerId: ", e)
                                }
                            }
                        }

                        // Combine with existing group chats
                        val currentGroupChats = _allChats.value.filter { it.isGroup }
                        _allChats.value = (individualChats + currentGroupChats).sortedByDescending { it.timestamp }
                        _isRefreshing.value = false

                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing chats: ", e)
                        _error.value = "Failed to load chats"
                        _isRefreshing.value = false
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "userChats listener cancelled: ", error.toException())
                _error.value = "Failed to load chats"
                _isRefreshing.value = false
            }
        }

        // Listen for group chats
        userGroupsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch {
                    try {
                        val groupChats = mutableListOf<ChatItem>()
                        val groupIds = snapshot.children.mapNotNull { it.key }

                        for (groupId in groupIds) {
                            try {
                                // Fetch group details
                                val groupSnapshot = database.child("groups").child(groupId).get().await()
                                val group = groupSnapshot.getValue(Group::class.java) ?: continue

                                // Fetch last message
                                val messageSnapshot = database.child("groupMessages")
                                    .child(groupId)
                                    .limitToLast(1)
                                    .get()
                                    .await()

                                val lastMessage = messageSnapshot.children.firstOrNull()
                                    ?.getValue(Message::class.java)

                                val chatItem = ChatItem(
                                    id = groupId,
                                    userId = "",
                                    name = group.name,
                                    profileImage = group.profileImageUrl,
                                    lastMessage = lastMessage?.message ?: "",
                                    timestamp = lastMessage?.timestamp ?: group.createdAt,
                                    unreadCount = 0, // TODO: Implement group unread count
                                    isOnline = false,
                                    isGroup = true,
                                    chatId = groupId,
                                    type = "group"
                                )
                                groupChats.add(chatItem)

                            } catch (e: Exception) {
                                Log.e(TAG, "Error fetching group $groupId: ", e)
                            }
                        }

                        // Combine with existing individual chats
                        val currentIndividualChats = _allChats.value.filter { !it.isGroup }
                        _allChats.value = (currentIndividualChats + groupChats).sortedByDescending { it.timestamp }

                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing groups: ", e)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "userGroups listener cancelled: ", error.toException())
            }
        }

        // Attach listeners
        database.child("userChats").child(userId).addValueEventListener(userChatsListener!!)
        database.child("user-groups").child(userId).addValueEventListener(userGroupsListener!!)
    }

    private fun removeListeners() {
        val userId = auth.currentUser?.uid ?: return

        userChatsListener?.let {
            database.child("userChats").child(userId).removeEventListener(it)
        }
        userGroupsListener?.let {
            database.child("user-groups").child(userId).removeEventListener(it)
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun deleteChat(chatId: String, isGroup: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: return

        if (isGroup) {
            // Remove user from group
            database.child("groups").child(chatId).child("members").child(currentUserId).removeValue()
            database.child("user-groups").child(currentUserId).child(chatId).removeValue()
        } else {
            // Remove chat from userChats
            database.child("userChats").child(currentUserId).child(chatId).removeValue()
                .addOnSuccessListener {
                    Log.d(TAG, "Chat deleted successfully")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to delete chat: ", e)
                    _error.value = "Failed to delete chat"
                }
        }
    }

    fun setOnlineStatus(isOnline: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        val updates = mutableMapOf<String, Any>(
            "online" to isOnline
        )
        if (!isOnline) {
            updates["lastSeen"] = System.currentTimeMillis()
        }
        database.child("users").child(userId).updateChildren(updates)
    }

    fun logout() {
        setOnlineStatus(false)
        removeListeners()
        _allChats.value = emptyList()
        auth.signOut()
    }

    fun resetError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        removeListeners()
    }
}