package com.example.pixelaura.pages

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.pixelaura.R
import com.example.pixelaura.logic.BackendManager
import com.example.pixelaura.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun PeersPage(navController: NavHostController) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    val followedUsers = remember { mutableStateListOf<UserWithUid>() }
    val suggestedUsers = remember { mutableStateListOf<UserWithUid>() }
    val backendManager = remember { BackendManager(context) }

    LaunchedEffect(currentUid) {
        currentUid?.let { uid ->
            // Real-time listener for following list changes
            firestore.collection("users").document(uid)
                .addSnapshotListener { docSnapshot, _ ->
                    val following = docSnapshot?.get("following") as? List<*> ?: emptyList<String>()
                    followedUsers.clear()
                    following.filterIsInstance<String>().forEach { followedUid ->
                        firestore.collection("users").document(followedUid).get()
                            .addOnSuccessListener { followedDoc ->
                                val user = followedDoc.toObject(User::class.java)
                                if (user != null) followedUsers.add(UserWithUid(user, followedUid))
                            }
                    }

                    // Suggestions: Load all users not in following
                    firestore.collection("users").get()
                        .addOnSuccessListener { allDocs ->
                            suggestedUsers.clear()
                            allDocs.forEach { doc ->
                                val otherUid = doc.id
                                if (otherUid != currentUid && !following.contains(otherUid)) {
                                    val user = doc.toObject(User::class.java)
                                    suggestedUsers.add(UserWithUid(user, otherUid))
                                }
                            }
                        }
                }
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                PeersHeader(navController)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(followedUsers) { userItem ->
                PeerItem(userItem, isFollowing = true) {
                    val currentUserRef = firestore.collection("users").document(currentUid!!)
                    val targetUserRef = firestore.collection("users").document(userItem.uid)

                    firestore.runBatch { batch ->
                        // Remove UID from following
                        batch.update(currentUserRef, "following", com.google.firebase.firestore.FieldValue.arrayRemove(userItem.uid))
                        // Decrement counts
                        batch.update(currentUserRef, "following_count", com.google.firebase.firestore.FieldValue.increment(-1))
                        batch.update(targetUserRef, "followers_count", com.google.firebase.firestore.FieldValue.increment(-1))
                    }.addOnSuccessListener {
                        Toast.makeText(context, "Unfollowed", Toast.LENGTH_SHORT).show()
                    }
                }

            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Suggestions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF780C28),
                    fontFamily = poppinsFont,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            items(suggestedUsers) { suggested ->
                PeerItem(suggested, isFollowing = false) {
                    val currentUserRef = firestore.collection("users").document(currentUid!!)
                    val targetUserRef = firestore.collection("users").document(suggested.uid)

                    firestore.runBatch { batch ->
                        // Add UID to following array
                        batch.update(currentUserRef, "following", com.google.firebase.firestore.FieldValue.arrayUnion(suggested.uid))
                        // Increment both counts
                        batch.update(currentUserRef, "following_count", com.google.firebase.firestore.FieldValue.increment(1))
                        batch.update(targetUserRef, "followers_count", com.google.firebase.firestore.FieldValue.increment(1))
                    }.addOnSuccessListener {
                        Toast.makeText(context, "Followed", Toast.LENGTH_SHORT).show()

                        // Send notification to followed user
                        backendManager.sendNotificationToUser(
                            toUid = suggested.uid,
                            contextType = "follow",
                            senderUid = currentUid
                        )
                    }
                }

            }
        }
    }
}

@Composable
fun PeerItem(userItem: UserWithUid, isFollowing: Boolean, onActionClick: () -> Unit) {
    val context = LocalContext.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(userItem.user.profile_picture.ifBlank { R.drawable.profile })
                .crossfade(true)
                .build(),
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(45.dp)
                .clip(CircleShape)
                .border(1.dp, Color(0xFF780C28), CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(userItem.user.username, fontFamily = poppinsFont, fontSize = 16.sp, color = Color(0xFF780C28))
            Text(userItem.user.handle, fontFamily = poppinsFont, fontSize = 14.sp, color = Color.Gray)
        }

        Button(
            onClick = { onActionClick() },
            modifier = Modifier.height(40.dp),
            colors = if (isFollowing)
                ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
            else
                ButtonDefaults.buttonColors(containerColor = Color(0xFF780C28)),
            border = if (isFollowing) BorderStroke(1.dp, Color(0xFF780C28)) else null,
            shape = CircleShape
        ) {
            Text(
                text = if (isFollowing) "Unfollow" else "Follow",
                color = if (isFollowing) Color(0xFF780C28) else Color.White,
                fontSize = 12.sp,
                fontFamily = poppinsFont
            )
        }
    }

    Divider(color = Color(0xFF780C28), thickness = 0.5.dp)
}

@Composable
fun PeersHeader(navController: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { navController.navigate("ownProfilePage") }, modifier = Modifier.padding(start = 3.dp)) {
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

            AsyncImage(
                model = profileUrl ?: R.drawable.profile,
                contentDescription = "Profile Picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(35.dp)
                    .clip(CircleShape)
            )
        }

        Text(
            text = "Peers",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = poppinsFont,
            color = Color(0xFF780C28)
        )
        var showLogoutDialog by remember { mutableStateOf(false) }
        IconButton(onClick = { showLogoutDialog = true }, modifier = Modifier.padding(3.dp)) {
            Icon(
                painter = painterResource(id = R.drawable.logout),
                contentDescription = "Settings",
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
                                popUpTo("homePage") { inclusive = true } // remove back stack
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


data class UserWithUid(val user: User, val uid: String)