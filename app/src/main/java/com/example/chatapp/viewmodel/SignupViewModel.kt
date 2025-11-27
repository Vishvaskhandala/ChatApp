package com.example.chatapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignupViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    private val _signupState = MutableStateFlow<SignupState>(SignupState.Idle)
    val signupState: StateFlow<SignupState> = _signupState

    fun signup(username: String, email: String, password: String) {
        viewModelScope.launch {
            _signupState.value = SignupState.Loading
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user
                if (firebaseUser != null) {
                    val user = User(
                        uid = firebaseUser.uid,
                        name = username,
                        email = email,
                        profileImage = "",
                        status = "online",
                        lastSeen = System.currentTimeMillis()
                    )
                    database.child("users").child(firebaseUser.uid).setValue(user).await()
                    _signupState.value = SignupState.Success
                } else {
                    _signupState.value = SignupState.Error("Signup failed. Please try again.")
                }
            } catch (e: Exception) {
                _signupState.value = SignupState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }

    fun resetState() {
        _signupState.value = SignupState.Idle
    }
}

sealed class SignupState {
    object Idle : SignupState()
    object Loading : SignupState()
    object Success : SignupState()
    data class Error(val message: String) : SignupState()
}