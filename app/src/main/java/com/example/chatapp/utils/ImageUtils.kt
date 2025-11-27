package com.example.chatapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import java.io.ByteArrayOutputStream

object ImageUtils {

    fun compressImage(context: Context, imageUri: Uri): ByteArray {
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
        return outputStream.toByteArray()
    }
}