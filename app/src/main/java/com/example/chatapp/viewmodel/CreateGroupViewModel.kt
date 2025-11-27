package com.example.chatapp.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.model.Group
import com.example.chatapp.data.model.User
import com.example.chatapp.utils.ImageUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CreateGroupViewModel : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _selectedMembers = MutableStateFlow<List<String>>(emptyList())
    val selectedMembers: StateFlow<List<String>> = _selectedMembers

    private val _groupCreated = MutableStateFlow(false)
    val groupCreated: StateFlow<Boolean> = _groupCreated

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference

    fun fetchUsers() {
        val currentUserId = auth.currentUser?.uid
        database.child("users").get().addOnSuccessListener {
            val userList = it.children.mapNotNull { it.getValue(User::class.java) }
                .filter { it.uid != currentUserId }
            _users.value = userList
        }
    }

    fun toggleMemberSelection(userId: String) {
        val currentSelection = _selectedMembers.value.toMutableList()
        if (currentSelection.contains(userId)) {
            currentSelection.remove(userId)
        } else {
            currentSelection.add(userId)
        }
        _selectedMembers.value = currentSelection
    }

    fun createGroup(context: Context, name: String, imageUri: Uri?) {
        viewModelScope.launch {
            val currentUser = auth.currentUser ?: return@launch
            val groupId = database.child("groups").push().key ?: return@launch

            val memberIds = _selectedMembers.value + currentUser.uid
            val membersMap = memberIds.associateWith { true }

            if (imageUri != null) {
                val compressedImage = ImageUtils.compressImage(context, imageUri)
                val imageRef = storage.child("group_icons/$groupId.jpg")
                imageRef.putBytes(compressedImage).continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    imageRef.downloadUrl
                }.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val downloadUri = task.result
                        val group = Group(
                            groupId = groupId,
                            name = name,
                            profileImageUrl = downloadUri.toString(),
                            members = membersMap,
                            admin = currentUser.uid,
                            createdAt = System.currentTimeMillis()
                        )
                        database.child("groups").child(groupId).setValue(group)
                        memberIds.forEach { database.child("user-groups").child(it).child(groupId).setValue(true) }
                        _groupCreated.value = true
                    }
                }
            } else {
                val group = Group(
                    groupId = groupId,
                    name = name,
                    members = membersMap,
                    admin = currentUser.uid,
                    createdAt = System.currentTimeMillis()
                )
                database.child("groups").child(groupId).setValue(group)
                memberIds.forEach { database.child("user-groups").child(it).child(groupId).setValue(true) }
                _groupCreated.value = true
            }
        }
    }
}