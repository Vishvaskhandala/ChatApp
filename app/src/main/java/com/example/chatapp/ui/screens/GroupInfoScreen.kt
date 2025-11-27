package com.example.chatapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.chatapp.data.model.User
import com.example.chatapp.ui.components.UserListItem
import com.example.chatapp.viewmodel.GroupInfoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    navController: NavController,
    groupId: String,
    viewModel: GroupInfoViewModel = viewModel()
) {
    val group by viewModel.group.collectAsState()
    val members by viewModel.members.collectAsState()
    var showRemoveMemberDialog by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(Unit) {
        viewModel.getGroupDetails(groupId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group?.name ?: "Group Info") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (viewModel.isCurrentUserAdmin()) {
                        IconButton(onClick = { navController.navigate("addMember/$groupId") }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Add Member")
                        }
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
                items(members) { member ->
                    UserListItem(
                        user = member,
                        trailingContent = {
                            if (viewModel.isCurrentUserAdmin() && member.uid != group?.admin) {
                                TextButton(onClick = { showRemoveMemberDialog = member }) {
                                    Text("Remove")
                                }
                            }
                        }
                    ) {}
                }
            }
        }
    }

    showRemoveMemberDialog?.let { member ->
        AlertDialog(
            onDismissRequest = { showRemoveMemberDialog = null },
            title = { Text("Remove Member") },
            text = { Text("Are you sure you want to remove ${member.name} from the group?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeMember(groupId, member.uid)
                        showRemoveMemberDialog = null
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveMemberDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}