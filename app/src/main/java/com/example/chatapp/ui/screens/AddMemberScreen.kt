package com.example.chatapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.chatapp.ui.components.UserListItem
import com.example.chatapp.viewmodel.GroupInfoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemberScreen(
    navController: NavController,
    groupId: String,
    viewModel: GroupInfoViewModel = viewModel()
) {
    val nonMembers by viewModel.nonMembers.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getNonMembers(groupId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Member") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn {
                items(nonMembers) { user ->
                    UserListItem(user = user) {
                        viewModel.addMember(groupId, user.uid)
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}