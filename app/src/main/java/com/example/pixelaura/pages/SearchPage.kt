package com.example.pixelaura.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.pixelaura.R
import com.example.pixelaura.logic.BackendManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchPage(navController: NavHostController) {
    val context = LocalContext.current
    val backendManager = remember { BackendManager(context) }
    val allPosts = remember { mutableStateListOf<Post>() }
    var searchQuery by remember { mutableStateOf("") }
    val filteredPosts = remember {
        derivedStateOf {
            if (searchQuery.isBlank()) allPosts
            else allPosts.filter {
                it.username.contains(searchQuery, ignoreCase = true) ||
                        it.handle.contains(searchQuery, ignoreCase = true) ||
                        it.text?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }
    val userHandle = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        backendManager.fetchAllPosts { posts ->
            allPosts.clear()
            allPosts.addAll(posts)
        }
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                userHandle.value = doc.getString("handle")
            }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                SearchHeader(navController = navController)
                Divider(color = Color(0xFF780C28), thickness = 1.dp)
            }
        },
        bottomBar = {
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
                Divider(color = Color(0xFF780C28), thickness = 1.dp)
                BottomNav(navController)
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = {
                    Text(
                        "Search posts or users",
                        color = Color(0xFF780C28),
                        fontFamily = poppinsFont
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF780C28),
                    unfocusedBorderColor = Color(0xFF780C28),
                    cursorColor = Color(0xFF780C28)
                )
            )


            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                if (filteredPosts.value.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No posts found",
                                color = Color(0xFF780C28),
                                fontSize = 18.sp,
                                fontFamily = poppinsFont
                            )
                        }
                    }
                } else {
                    itemsIndexed(filteredPosts.value) { index, post ->
                        PostItem(
                            post = post,
                            index = index,
                            posts = allPosts,
                            currentUserHandle = userHandle.value,
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchHeader(navController: NavHostController) {
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    var profileUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { document ->
                    profileUrl = document.getString("profile_picture")
                }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { navController.navigate("ownProfilePage") }, modifier = Modifier.padding(start = 3.dp)) {
            androidx.compose.foundation.Image(
                painter = coil.compose.rememberAsyncImagePainter(
                    model = profileUrl ?: com.example.pixelaura.R.drawable.profile
                ),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(35.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
            )
        }
        Text(
            text = "Search",
            fontSize = 25.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            fontFamily = com.example.pixelaura.pages.poppinsFont,
            color = Color(0xFF780C28)
        )
        var showLogoutDialog by remember { mutableStateOf(false) }
        IconButton(
            onClick = { showLogoutDialog = true },
            modifier = Modifier.padding(3.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.logout),
                contentDescription = "Logout",
                tint = Color(0xFF780C28),
                modifier = Modifier.size(35.dp)
            )
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = {
                    Text("Logout", fontWeight = FontWeight.Bold, color = Color(0xFF780C28), fontFamily = poppinsFont)
                },
                text = {
                    Text("Are you sure you want to logout?", color = Color(0xFF780C28), fontFamily = poppinsFont)
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                            FirebaseAuth.getInstance().signOut()

                            // Optionally clear any locally stored UID/token if used
                            navController.navigate("loginPage") {
                                popUpTo("homePage") { inclusive = true }
                            }
                        }
                    ) {
                        Text("Yes", color = Color(0xFF780C28), fontFamily = poppinsFont)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("No", color = Color(0xFF780C28), fontFamily = poppinsFont)
                    }
                },
                containerColor = Color.White
            )
        }
    }
}
