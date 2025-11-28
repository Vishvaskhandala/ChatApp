package com.example.chatapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SignupViewModel : ViewModel() {

    sealed class SignupState {
        object Idle : SignupState()
        object Loading : SignupState()
        object Success : SignupState()
        data class Error(val message: String) : SignupState()
    }

    private val _signupState = MutableStateFlow<SignupState>(SignupState.Idle)
    val signupState: StateFlow<SignupState> = _signupState

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    fun signup(username: String, email: String, password: String) {
        viewModelScope.launch {
            _signupState.value = SignupState.Loading

            try {
                val result = auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val firebaseUser = task.result?.user
                            if (firebaseUser != null) {
                                val user = User(
                                    uid = firebaseUser.uid,
                                    username = username,
                                    name = username, // Initially same as username
                                    email = email,
                                    profileImage = "",
                                    online = true,
                                    lastSeen = System.currentTimeMillis(),
                                    createdAt = System.currentTimeMillis()
                                )

                                database.child("users").child(firebaseUser.uid).setValue(user)
                                    .addOnSuccessListener {
                                        _signupState.value = SignupState.Success
                                    }
                                    .addOnFailureListener { e ->
                                        _signupState.value = SignupState.Error(e.message ?: "Failed to save user data")
                                    }
                            } else {
                                _signupState.value = SignupState.Error("Failed to create user")
                            }
                        } else {
                            val exception = task.exception
                            val errorMessage = when (exception) {
                                is FirebaseAuthUserCollisionException -> "Email already exists"
                                else -> exception?.message ?: "Signup failed"
                            }
                            _signupState.value = SignupState.Error(errorMessage)
                        }
                    }
            } catch (e: Exception) {
                _signupState.value = SignupState.Error(e.message ?: "Signup failed")
            }
        }
    }

    fun resetState() {
        _signupState.value = SignupState.Idle
    }
}