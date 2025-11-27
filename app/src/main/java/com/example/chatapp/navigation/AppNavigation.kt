package com.example.chatapp.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.chatapp.ui.screens.*
import com.example.chatapp.viewmodel.LoginViewModel
import com.example.chatapp.viewmodel.SignupViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val loginViewModel: LoginViewModel = viewModel()

    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
        "chatList"
    } else {
        "login"
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(navController = navController, viewModel = loginViewModel)
        }
        composable("signup") {
            val signupViewModel: SignupViewModel = viewModel()
            SignupScreen(
                onSignupClick = { username, email, password ->
                    signupViewModel.signup(username, email, password)
                },
                onLoginClick = { navController.popBackStack() },
                viewModel = signupViewModel
            )
        }
        composable("chatList") {
            ChatListScreen(navController = navController)
        }
        composable("userSelection") {
            UserSelectionScreen(navController = navController)
        }
        composable("createGroup") {
            CreateGroupScreen(navController = navController)
        }
        composable("profile") {
            ProfileScreen(navController = navController)
        }
        composable(
            "addMember/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) {
            val groupId = it.arguments?.getString("groupId") ?: ""
            AddMemberScreen(navController = navController, groupId = groupId)
        }
        composable(
            "chat/{receiverId}",
            arguments = listOf(navArgument("receiverId") { type = NavType.StringType })
        ) {
            val receiverId = it.arguments?.getString("receiverId") ?: ""
            ChatScreen(navController = navController, receiverId = receiverId)
        }
        composable(
            "groupChat/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) {
            val groupId = it.arguments?.getString("groupId") ?: ""
            GroupChatScreen(navController = navController, groupId = groupId)
        }
        composable(
            "groupInfo/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) {
            val groupId = it.arguments?.getString("groupId") ?: ""
            GroupInfoScreen(navController = navController, groupId = groupId)
        }
    }
}
