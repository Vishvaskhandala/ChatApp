package com.example.chatapp.viewmodel

import androidx.lifecycle.ViewModel
import com.example.chatapp.data.model.Group
import com.example.chatapp.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GroupInfoViewModel : ViewModel() {

    private val _group = MutableStateFlow<Group?>(null)
    val group: StateFlow<Group?> = _group

    private val _members = MutableStateFlow<List<User>>(emptyList())
    val members: StateFlow<List<User>> = _members

    private val _nonMembers = MutableStateFlow<List<User>>(emptyList())
    val nonMembers: StateFlow<List<User>> = _nonMembers

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    fun getGroupDetails(groupId: String) {
        database.child("groups").child(groupId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val group = snapshot.getValue(Group::class.java)
                _group.value = group
                group?.members?.keys?.let { fetchMembers(it.toList()) }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun fetchMembers(memberIds: List<String>) {
        val users = mutableListOf<User>()
        memberIds.forEach { userId ->
            database.child("users").child(userId).get().addOnSuccessListener {
                it.getValue(User::class.java)?.let { user ->
                    users.add(user)
                    _members.value = users
                }
            }
        }
    }

    fun removeMember(groupId: String, userId: String) {
        if (isCurrentUserAdmin() && userId != _group.value?.admin) {
            database.child("groups").child(groupId).child("members").child(userId).removeValue()
        }
    }

    fun getNonMembers(groupId: String) {
        database.child("groups").child(groupId).get().addOnSuccessListener { groupSnapshot ->
            val group = groupSnapshot.getValue(Group::class.java)
            val groupMemberIds = group?.members?.keys ?: emptySet()

            database.child("users").get().addOnSuccessListener { usersSnapshot ->
                val allUsers = usersSnapshot.children.mapNotNull { it.getValue(User::class.java) }
                val nonMembers = allUsers.filter { it.uid !in groupMemberIds }
                _nonMembers.value = nonMembers
            }
        }
    }

    fun addMember(groupId: String, userId: String) {
        database.child("groups").child(groupId).child("members").child(userId).setValue(true)
    }

    fun isCurrentUserAdmin(): Boolean {
        val currentUserId = auth.currentUser?.uid
        return currentUserId != null && currentUserId == _group.value?.admin
    }
}