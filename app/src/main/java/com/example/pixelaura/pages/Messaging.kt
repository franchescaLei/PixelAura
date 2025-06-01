package com.example.pixelaura.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.pixelaura.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@Composable
fun MessagesPage(navController: NavHostController) {
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    val context = LocalContext.current
    val messages = remember { mutableStateListOf<ConversationPreview>() }
    var showComposeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentUid) {
        currentUid?.let { uid ->
            FirebaseFirestore.getInstance()
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val relevant = snapshot.documents.filter { doc ->
                            val sender = doc.getString("sender")
                            val receiver = doc.getString("receiver")
                            sender == uid || receiver == uid
                        }

                        val conversationMap = mutableMapOf<String, ConversationPreview>()

                        relevant.forEach { doc ->
                            val sender = doc.getString("sender") ?: return@forEach
                            val receiver = doc.getString("receiver") ?: return@forEach
                            val contentRaw = doc.getString("content") ?: ""
                            val timestamp = doc.getTimestamp("timestamp")

                            // create convo id
                            val participants = listOf(sender, receiver).sorted()
                            val convoId = participants.joinToString("_")

                            // Determine which one is the other user
                            val otherUid = if (sender == uid) receiver else sender
                            val readBy = doc.get("readBy") as? List<String> ?: emptyList()
                            val isUnread = sender == otherUid && receiver == uid && !readBy.contains(uid)


                            val previewText = if (sender == uid) {
                                "You sent: $contentRaw"
                            } else {
                                contentRaw
                            }

                            // Always keep only the latest message per conversation
                            val existing = conversationMap[convoId]
                            if (existing == null || (timestamp != null && (existing.timestamp == null || timestamp > existing.timestamp))) {
                                conversationMap[convoId] = ConversationPreview(
                                    uid = otherUid,
                                    lastMessage = previewText,
                                    timestamp = timestamp,
                                    isUnread = isUnread
                                )
                            }
                        }

                        messages.clear()
                        messages.addAll(conversationMap.values.sortedByDescending { it.timestamp })

                    }
                }
        }
    }


    Scaffold(
        topBar = {
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                HeaderWithTitle(navController, "Messages")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // new message button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { showComposeDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF780C28))
                ) {
                    Text(
                        "Compose New Message",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontFamily = poppinsFont
                    )
                }
            }

            // Messages List or Empty State
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No messages to display",
                        color = Color(0xFF780C28),
                        fontSize = 18.sp,
                        fontFamily = poppinsFont
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(messages) { preview ->
                        var username by remember { mutableStateOf("") }
                        var handle by remember { mutableStateOf("") }
                        var profileUrl by remember { mutableStateOf<String?>(null) }

                        LaunchedEffect(preview.uid) {
                            FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(preview.uid)
                                .get()
                                .addOnSuccessListener {
                                    username = it.getString("username") ?: ""
                                    handle = it.getString("handle") ?: ""
                                    profileUrl = it.getString("profile_picture")
                                }
                        }

                        val bgColor = if (preview.isUnread) Color(0xFFFFEBEE) else Color.Transparent

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .background(bgColor, shape = RoundedCornerShape(12.dp)) // ✅ round the tint
                                .clickable {
                                    navController.navigate("conversationPage/${preview.uid}")
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = profileUrl ?: R.drawable.profile,
                                    contentDescription = "Profile Image",
                                    modifier = Modifier
                                        .size(45.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, Color(0xFF780C28), CircleShape),
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = username,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = poppinsFont,
                                        color = Color(0xFF780C28)
                                    )
                                    Text(
                                        text = handle,
                                        fontSize = 14.sp,
                                        color = Color.Gray,
                                        fontFamily = poppinsFont
                                    )
                                    Text(
                                        text = preview.lastMessage,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = Color.DarkGray,
                                        fontFamily = poppinsFont
                                    )
                                }

                                preview.timestamp?.let {
                                    Text(
                                        text = android.text.format.DateFormat.format("h:mm a", it.toDate()).toString(),
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
        if (showComposeDialog) {
            ComposeMessageDialog(
                onDismiss = { showComposeDialog = false },
                onSend = { toHandle, message ->
                    showComposeDialog = false
                }
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeMessageDialog(
    onDismiss: () -> Unit,
    onSend: (String, String) -> Unit
) {
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    var toHandle by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val following = remember { mutableStateListOf<String>() }
    var expanded by remember { mutableStateOf(false) }
    val filteredSuggestions = remember(toHandle, following) {
        following.filter { it.contains(toHandle, ignoreCase = true) }
    }


    LaunchedEffect(Unit) {
        db.collection("users").document(currentUid).get()
            .addOnSuccessListener { doc ->
                val followingUids = doc.get("following") as? List<String> ?: emptyList()
                following.clear()

                // fetch handles of followed users
                followingUids.forEach { uid ->
                    db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
                        val handle = userDoc.getString("handle")
                        if (!handle.isNullOrBlank()) {
                            following.add(handle)
                        }
                    }
                }

            }
    }

    AlertDialog(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .heightIn(min = 450.dp),
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {},
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            {
                Text(
                    text = "Compose New Message",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF780C28),
                    fontFamily = poppinsFont
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box {
                    OutlinedTextField(
                        value = toHandle,
                        onValueChange = {
                            toHandle = it
                            expanded = true
                        },
                        label = { Text("To:", fontFamily = poppinsFont) },
                        singleLine = true,
                        isError = errorMessage != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF780C28),
                            unfocusedBorderColor = Color(0xFF780C28),
                            cursorColor = Color(0xFF780C28)
                        )
                    )

                    DropdownMenu(
                        expanded = expanded && filteredSuggestions.isNotEmpty(),
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        filteredSuggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion, fontFamily = poppinsFont) },
                                onClick = {
                                    toHandle = suggestion
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content:", fontFamily = poppinsFont) },
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(vertical = 6.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF780C28),
                        unfocusedBorderColor = Color(0xFF780C28),
                        cursorColor = Color(0xFF780C28)
                    )
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color.Red,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp),
                        fontFamily = poppinsFont
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { onDismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                    ) {
                        Text("Cancel", color = Color.Black, fontFamily = poppinsFont)
                    }

                    Button(
                        onClick = {
                            val trimmedHandle = toHandle.trim()
                            val db = FirebaseFirestore.getInstance()
                            val senderUid = FirebaseAuth.getInstance().currentUser?.uid ?: return@Button

                            db.collection("users")
                                .whereEqualTo("handle", trimmedHandle)
                                .get()
                                .addOnSuccessListener { snap ->
                                    val targetDoc = snap.documents.firstOrNull()
                                    val receiverUid = targetDoc?.id

                                    if (receiverUid != null) {
                                        // Create message doc
                                        val messageData = hashMapOf(
                                            "sender" to senderUid,
                                            "receiver" to receiverUid,
                                            "content" to content,
                                            "timestamp" to com.google.firebase.Timestamp.now(),
                                            "readBy" to listOf(senderUid)
                                        )

                                        db.collection("messages")
                                            .add(messageData)
                                            .addOnSuccessListener {
                                                onSend(receiverUid, content)
                                            }
                                            .addOnFailureListener {
                                                errorMessage = "Failed to send message."
                                            }
                                    } else {
                                        errorMessage = "User not found."
                                    }
                                }
                                .addOnFailureListener {
                                    errorMessage = "Something went wrong."
                                }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF780C28))
                    ) {
                        Text("Send", color = Color.White, fontFamily = poppinsFont)
                    }
                }
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
fun HeaderWithTitle(navController: NavHostController, title: String) {
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    var profileUrl by remember { mutableStateOf<String?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }

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
        IconButton(
            onClick = { navController.navigate("ownProfilePage") },
            modifier = Modifier.padding(start = 3.dp)
        ) {
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
            text = title,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = poppinsFont,
            color = Color(0xFF780C28)
        )

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

data class ConversationPreview(
    val uid: String,
    val lastMessage: String,
    val timestamp: Timestamp?,
    val isUnread: Boolean = false

)
