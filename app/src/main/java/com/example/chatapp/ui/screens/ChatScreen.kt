package com.example.chatapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.chatapp.MyFirebaseMessagingService
import com.example.chatapp.data.model.Message
import com.example.chatapp.ui.components.FullScreenImage
import com.example.chatapp.ui.components.MessageBubble
import com.example.chatapp.ui.components.TypingIndicator
import com.example.chatapp.utils.ChatUtils
import com.example.chatapp.utils.ComposeFileProvider
import com.example.chatapp.viewmodel.ChatViewModel
import com.example.chatapp.viewmodel.UploadState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    viewModel: ChatViewModel = viewModel(),
    chatId: String
) {
    val messages by viewModel.messages.collectAsState()
    val typingStatus by viewModel.typingStatus.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()
    val receiverUser by viewModel.receiverUser.collectAsState()
    val error by viewModel.error.collectAsState()

    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showImageDialog by remember { mutableStateOf(false) }
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Use the centralized utility to get the peer ID
    val receiverId = remember(chatId, currentUserId) {
        ChatUtils.getPeerId(chatId, currentUserId)
    }

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

    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            uri?.let { viewModel.uploadImage(context, chatId, it) }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                imageUri?.let { viewModel.uploadImage(context, chatId, it) }
            }
        }
    )

    // Show snackbar on error
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
            viewModel.resetError() // Reset error after showing
        }
    }

    DisposableEffect(chatId) {
        MyFirebaseMessagingService.isChatScreenOpen = true
        MyFirebaseMessagingService.currentChatId = chatId
        onDispose {
            MyFirebaseMessagingService.isChatScreenOpen = false
            MyFirebaseMessagingService.currentChatId = null
        }
    }

    LaunchedEffect(chatId, receiverId) {
        viewModel.getReceiverUser(receiverId)
        viewModel.getMessages(chatId)
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
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = rememberAsyncImagePainter(receiverUser?.profileImage),
                                contentDescription = "Profile Picture",
                                modifier = Modifier.size(40.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = receiverUser?.name ?: "")
                                AnimatedVisibility(visible = typingStatus) {
                                    TypingIndicator()
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            bottomBar = {
                Column {
                    if (uploadState is UploadState.InProgress) {
                        val progressValue = (uploadState as UploadState.InProgress).progress
                        LinearProgressIndicator(
                            progress = progressValue,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Row(
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
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
                                viewModel.sendMessage(chatId, text)
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
                modifier = Modifier.padding(paddingValues).fillMaxSize(),
                reverseLayout = true
            ) {
                items(messages.reversed(), key = { it.id }) { message ->
                    MessageBubble(message = message, isSender = message.senderId == currentUserId) {
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
                                val newImageUri = ComposeFileProvider.getImageUri(context)
                                imageUri = newImageUri
                                newImageUri?.let { cameraLauncher.launch(it) }
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
