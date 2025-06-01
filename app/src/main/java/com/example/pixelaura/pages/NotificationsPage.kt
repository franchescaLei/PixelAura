package com.example.pixelaura.pages

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
import com.example.pixelaura.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@Composable
fun NotificationsPage(navController: NavHostController) {
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    val notifications = remember { mutableStateListOf<NotificationItem>() }

    LaunchedEffect(currentUid) {
        currentUid?.let { uid ->
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        notifications.clear()
                        notifications.addAll(snapshot.documents.mapNotNull { doc ->
                            val context = doc.getString("context") ?: return@mapNotNull null
                            val senderUid = doc.getString("senderUid") ?: return@mapNotNull null
                            val message = doc.getString("message") ?: return@mapNotNull null
                            val timestamp = doc.getTimestamp("timestamp")
                            NotificationItem(context, senderUid, message, timestamp)
                        })
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                NotificationsHeader(navController)
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
        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            items(notifications) { notif ->
                var profileUrl by remember { mutableStateOf<String?>(null) }

                // Fetch profile image using senderUid
                LaunchedEffect(notif.senderUid) {
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(notif.senderUid)
                        .get()
                        .addOnSuccessListener {
                            profileUrl = it.getString("profile_picture")
                        }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        AsyncImage(
                            model = profileUrl ?: R.drawable.profile,
                            contentDescription = "Sender Profile Picture",
                            modifier = Modifier
                                .size(45.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color(0xFF780C28), CircleShape),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = notif.message,
                                fontFamily = poppinsFont,
                                fontSize = 16.sp,
                                color = Color(0xFF780C28)
                            )
                            notif.timestamp?.let { timestamp ->
                                Text(
                                    text = android.text.format.DateFormat.format("MMM dd, h:mm a", timestamp.toDate()).toString(),
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontFamily = poppinsFont
                                )
                            }
                        }
                    }

                    Divider(color = Color(0xFF780C28), thickness = 0.5.dp)
                }
            }

        }
    }
}

@Composable
fun NotificationsHeader(navController: NavHostController) {
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
            text = "Notifications",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = poppinsFont,
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

data class NotificationItem(
    val context: String,
    val senderUid: String,
    val message: String,
    val timestamp: com.google.firebase.Timestamp?
)

