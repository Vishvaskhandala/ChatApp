package com.example.chatapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.chatapp.R
import com.example.chatapp.utils.ComposeFileProvider
import com.example.chatapp.viewmodel.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel()
) {
    val user by viewModel.user.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

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
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            uri?.let { viewModel.updateProfileImage(it) }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                imageUri?.let { viewModel.updateProfileImage(it) }
            }
        }
    )

    LaunchedEffect(user) {
        user?.let {
            name = it.name
            phone = it.phone
            status = it.status
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isEditing = !isEditing }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                user?.let { currentUser ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.clickable { showImageSourceDialog = true }) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = currentUser.profileImage,
                                    placeholder = painterResource(id = R.drawable.ic_launcher_background),
                                    error = painterResource(id = R.drawable.ic_launcher_background)
                                ),
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(150.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(value = name, onValueChange = {name = it}, label = {Text("Name")}, enabled = isEditing)
                        OutlinedTextField(value = phone, onValueChange = {phone = it}, label = {Text("Phone")}, enabled = isEditing)
                        OutlinedTextField(value = status, onValueChange = {status = it}, label = {Text("Status")}, enabled = isEditing)

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Email: ${currentUser.email}")
                        Text("Member since: ${SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(currentUser.createdAt))}")

                        Spacer(modifier = Modifier.height(32.dp))

                        if (isEditing) {
                            Button(onClick = { viewModel.updateProfile(name, phone, status) }) {
                                Text("Save")
                            }
                        }
                        
                        Button(onClick = { showLogoutDialog = true }) {
                            Text("Logout")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(onClick = { showDeleteDialog = true }) {
                            Text("Delete Account", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            if (showImageSourceDialog) {
                AlertDialog(
                    onDismissRequest = { showImageSourceDialog = false },
                    title = { Text("Update Profile Picture") },
                    text = { Text("Choose an option") },
                    confirmButton = {
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
                            TextButton(
                                onClick = { 
                                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    showImageSourceDialog = false 
                                }
                            ) {
                                Text("Gallery")
                            }
                            TextButton(
                                onClick = {
                                    if (hasCamPermission) {
                                        imageUri = ComposeFileProvider.getImageUri(context)
                                        imageUri?.let { cameraLauncher.launch(it) }
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                    showImageSourceDialog = false
                                }
                            ) {
                                Text("Camera")
                            }                            
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showImageSourceDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showLogoutDialog) {
                AlertDialog(
                    onDismissRequest = { showLogoutDialog = false },
                    title = { Text("Logout") },
                    text = { Text("Are you sure you want to logout?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.logout()
                                showLogoutDialog = false
                                navController.navigate("login") {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                }
                            }
                        ) {
                            Text("Logout")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Delete Account") },
                    text = { Text("This action is irreversible. Are you sure you want to delete your account?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteAccount()
                                showDeleteDialog = false
                                navController.navigate("login") {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                }
                            }
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}