package com.example.chatapp.viewmodel

/**
 * Represents the state of an upload operation (images, files, etc.)
 */
sealed class UploadState {
    object Idle : UploadState()
    data class InProgress(val progress: Float) : UploadState()
    object Success : UploadState()
    data class Failure(val error: Throwable) : UploadState()
}