package com.example.chatapp.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.chatapp.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        _isLoading.value = true

        database.child("users").child(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    _user.value = snapshot.getValue(User::class.java)
                    _isLoading.value = false
                }

                override fun onCancelled(error: DatabaseError) {
                    _isLoading.value = false
                }
            })
    }

    fun updateProfile(name: String, username: String, status: String) {
        val uid = auth.currentUser?.uid ?: return
        val updates = mapOf(
            "name" to name,
            "username" to username,
            "status" to status
        )
        database.child("users").child(uid).updateChildren(updates)
    }

    /**
     * Upload selected image (PNG/JPG/etc) to Firebase Storage and
     * save its download URL to users/{uid}/profileImage
     */
    fun uploadProfileImage(imageUri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        _isUploading.value = true

        // You can use .png or .jpg in the path; both work for any image type.
        val imageRef = storage.child("profile_images/$uid.png")

        imageRef.putFile(imageUri)
            .continueWithTask { task ->
                if (!task.isSuccessful) task.exception?.let { throw it }
                imageRef.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                database.child("users").child(uid)
                    .child("profileImage")
                    .setValue(downloadUri.toString())
                _isUploading.value = false
            }
            .addOnFailureListener {
                _isUploading.value = false
            }
    }
}