package com.example.chatapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.chatapp.data.model.User
import com.example.chatapp.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel()
) {
    val user by viewModel.user.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }

    // Gallery picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadProfileImage(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Image
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { imagePickerLauncher.launch("image/*") } // <-- tap to pick image
                    ) {
                        if (user?.profileImage?.isNotEmpty() == true) {
                            Image(
                                painter = rememberAsyncImagePainter(model = user?.profileImage),
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Default profile icon when no image set
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Default Profile",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Small overlay icon or loader
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .padding(6.dp)
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Change Picture",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // User Info
                    user?.let { currentUser ->
                        ProfileInfoCard(
                            title = "Name",
                            value = currentUser.name.ifEmpty { currentUser.username },
                            icon = Icons.Default.Person
                        )

                        ProfileInfoCard(
                            title = "Username",
                            value = currentUser.username,
                            icon = Icons.Default.AlternateEmail
                        )

                        ProfileInfoCard(
                            title = "Status",
                            value = currentUser.status,
                            icon = Icons.Default.Info
                        )

                        ProfileInfoCard(
                            title = "Email",
                            value = currentUser.email,
                            icon = Icons.Default.Email
                        )

                        ProfileInfoCard(
                            title = "Availability",
                            value = if (currentUser.online) "Online" else "Offline",
                            icon = Icons.Default.Circle,
                            tint = if (currentUser.online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            user = user,
            onDismiss = { showEditDialog = false },
            onSave = { name, username, status ->
                viewModel.updateProfile(name, username, status)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun ProfileInfoCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    user: User?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(user?.name ?: "") }
    var username by remember { mutableStateOf(user?.username ?: "") }
    var status by remember { mutableStateOf(user?.status ?: "Hey there! I'm using ChatApp") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = status,
                    onValueChange = { status = it },
                    label = { Text("Status") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, username, status) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}