package com.example.chatapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.chatapp.ui.components.FullScreenImage
import com.example.chatapp.ui.components.MessageBubble
import com.example.chatapp.utils.ComposeFileProvider
import com.example.chatapp.viewmodel.GroupChatViewModel
import com.example.chatapp.viewmodel.UploadState
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    navController: NavController,
    groupId: String,
    viewModel: GroupChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()
    val group by viewModel.group.collectAsState()
    val senderNames by viewModel.senderNames.collectAsState()
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }
    var showImageDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var hasCamPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCamPermission = granted
        }
    )

    var imageUri: Uri? = null

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            uri?.let {
                viewModel.uploadGroupImage(context, groupId, it)
            }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                imageUri?.let { uri ->
                    viewModel.uploadGroupImage(context, groupId, uri)
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        viewModel.getGroupDetails(groupId)
        viewModel.getGroupMessages(groupId)
    }

    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (fullScreenImageUrl != null) {
        FullScreenImage(imageUrl = fullScreenImageUrl!!) {
            fullScreenImageUrl = null
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = rememberAsyncImagePainter(group?.profileImageUrl),
                                contentDescription = "Group Icon",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = group?.name ?: "Group Chat")
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate("groupInfo/$groupId") }) {
                            Icon(Icons.Default.Info, contentDescription = "Group Info")
                        }
                    }
                )
            },
            bottomBar = {
                Column {
                    if (uploadState is UploadState.InProgress) {
                        LinearProgressIndicator(
                            progress = { (uploadState as UploadState.InProgress).progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showImageDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Attach File")
                        }
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Message") }
                        )
                        IconButton(onClick = {
                            if (text.isNotBlank()) {
                                viewModel.sendGroupMessage(groupId, text)
                                text = ""
                            }
                        }) {
                            Icon(Icons.Default.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                reverseLayout = true
            ) {
                items(messages.reversed()) { message ->
                    val isSender = message.senderId == FirebaseAuth.getInstance().currentUser?.uid
                    MessageBubble(
                        message = message,
                        isSender = isSender,
                        senderName = if (isSender) null else senderNames[message.senderId]
                    ) {
                        if (message.imageUrl != null) {
                            fullScreenImageUrl = message.imageUrl
                        }
                    }
                }
            }
        }
    }

    if (showImageDialog) {
        AlertDialog(
            onDismissRequest = { showImageDialog = false },
            title = { Text("Choose Image Source") },
            text = {
                Column {
                    TextButton(onClick = {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        showImageDialog = false
                    }) {
                        Text("Gallery")
                    }
                    TextButton(
                        onClick = {
                            if (hasCamPermission) {
                                val uri = ComposeFileProvider.getImageUri(context)
                                imageUri = uri
                                cameraLauncher.launch(uri)
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                            showImageDialog = false
                        }
                    ) {
                        Text("Camera")
                    }
                }
            },
            confirmButton = {}
        )
    }
}
