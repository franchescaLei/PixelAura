package com.example.pixelaura.pages

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pixelaura.ui.theme.AuthViewModel

@Composable
fun PixelAuraNavigation(modifier:Modifier = Modifier, authViewModel: AuthViewModel){
    val navController = rememberNavController()


    NavHost(
        navController = navController,
        startDestination = "loading",
        modifier = androidx.compose.ui.Modifier.padding()
    ) {
        composable("loading") {
            LoadingScreen(navController)
            LaunchedEffect(Unit) {
                Toast.makeText(navController.context, "Loading", Toast.LENGTH_SHORT).show()
            }
        }
        composable("loginPage") {
            LogInScreen(navController = navController, modifier = androidx.compose.ui.Modifier.padding(),authViewModel)
            LaunchedEffect(Unit) {
                Toast.makeText(navController.context, "Please Login or Signup to continue", Toast.LENGTH_SHORT).show()
            }
        }
        composable("signupPage") {
            SignUpScreen(navController = navController, modifier = androidx.compose.ui.Modifier.padding(), authViewModel)
        }
        composable("homePage") {
            HomePage(navController = navController, modifier = Modifier.padding())
        }
        composable("forgotPasswordPage") {
            ResetPasswordPage(navController = navController, modifier = Modifier.padding(), authViewModel = authViewModel)
        }
        composable("newPostPage") {
            NewPostActivity(navController = navController)
        }
        composable("ownProfilePage") {
            OwnProfilePage(navController = navController)
        }
        composable("otherProfilePage/{uid}") { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            OtherProfilePage(navController, uid)
        }
        composable("searchPage") {
            SearchPage(navController = navController)
        }
        composable("notificationsPage") {
            NotificationsPage(navController)
        }
        composable("peersPage") {
            PeersPage(navController)
        }
        composable("messagesPage") {
            MessagesPage(navController)
        }
        composable("conversationPage/{uid}") { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            ConversationPage(navController, uid)
        }
    }
}