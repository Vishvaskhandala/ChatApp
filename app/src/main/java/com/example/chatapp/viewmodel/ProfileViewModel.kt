package com.example.chatapp.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference

    init {
        fetchUser()
    }

    private fun fetchUser() {
        _isLoading.value = true
        val userId = auth.currentUser?.uid ?: return
        database.child("users").child(userId).get().addOnSuccessListener {
            _user.value = it.getValue(User::class.java)
            _isLoading.value = false
        }.addOnFailureListener {
            _isLoading.value = false
            // Handle error
        }
    }

    fun updateProfile(name: String, phone: String, status: String) {
        _isLoading.value = true
        val userId = auth.currentUser?.uid ?: return
        val updates = mapOf(
            "name" to name,
            "phone" to phone,
            "status" to status
        )
        database.child("users").child(userId).updateChildren(updates).addOnCompleteListener {
            _isLoading.value = false
            fetchUser() // Refresh user data
        }
    }

    fun updateProfileImage(imageUri: Uri) {
        _isLoading.value = true
        val userId = auth.currentUser?.uid ?: return
        val imageRef = storage.child("profileImages/$userId.jpg")

        imageRef.putFile(imageUri).continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            imageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                database.child("users").child(userId).child("profileImage").setValue(downloadUri.toString())
                    .addOnCompleteListener {
                        _isLoading.value = false
                        fetchUser() // Refresh user data
                    }
            } else {
                _isLoading.value = false
                // Handle failure
            }
        }
    }

    fun logout() {
        auth.signOut()
    }

    fun deleteAccount() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            auth.currentUser?.delete()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    database.child("users").child(userId).removeValue()
                }
            }
        }
    }
}