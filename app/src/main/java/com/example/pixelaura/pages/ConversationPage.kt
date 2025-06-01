package com.example.pixelaura.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.pixelaura.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch


data class ChatMessage(
    val sender: String = "",
    val receiver: String = "",
    val content: String = "",
    val timestamp: Timestamp? = null
)

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun keyboardAsState(): State<Boolean> {
    val ime = WindowInsets.ime
    val isVisible = ime.getBottom(LocalDensity.current) > 0
    return rememberUpdatedState(isVisible)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationPage(navController: NavHostController, otherUid: String) {
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var recipientHandle by remember { mutableStateOf("") }
    var recipientProfileUrl by remember { mutableStateOf<String?>(null) }
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()


    val keyboardVisible by keyboardAsState()

    //scrolls to the last message whenever keyboard is open
    LaunchedEffect(keyboardVisible) {
        if (keyboardVisible && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    //auto scroll to the bottom of the page when convo isopened
    LaunchedEffect(messages.size, keyboardVisible) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }


    // Real time listener for updating convo messages
    LaunchedEffect(otherUid) {
        FirebaseFirestore.getInstance()
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val history = snapshot.documents.mapNotNull { doc ->
                        val sender = doc.getString("sender") ?: return@mapNotNull null
                        val receiver = doc.getString("receiver") ?: return@mapNotNull null
                        val content = doc.getString("content") ?: return@mapNotNull null
                        val timestamp = doc.getTimestamp("timestamp")

                        if (
                            (sender == currentUid && receiver == otherUid) ||
                            (sender == otherUid && receiver == currentUid)
                        ) {
                            ChatMessage(sender, receiver, content, timestamp)
                        } else null
                    }

                    messages.clear()
                    messages.addAll(history)

                    val unreadMessages = snapshot.documents.filter { doc ->
                        val readBy = doc.get("readBy") as? List<String> ?: emptyList()
                        val sender = doc.getString("sender")
                        val receiver = doc.getString("receiver")
                        sender == otherUid && receiver == currentUid && !readBy.contains(currentUid)
                    }

                    unreadMessages.forEach { doc ->
                        val ref = doc.reference
                        ref.update("readBy", FieldValue.arrayUnion(currentUid))
                    }
                }
            }
    }

    //fetch recipient handle to display
    LaunchedEffect(otherUid) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(otherUid)
            .get()
            .addOnSuccessListener { doc ->
                recipientHandle = doc.getString("handle")?.removePrefix("@") ?: ""
                recipientProfileUrl = doc.getString("profile_picture")
            }

        // Real-time listener
        FirebaseFirestore.getInstance()
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val history = snapshot.documents.mapNotNull { doc ->
                        val sender = doc.getString("sender") ?: return@mapNotNull null
                        val receiver = doc.getString("receiver") ?: return@mapNotNull null
                        val content = doc.getString("content") ?: return@mapNotNull null
                        val timestamp = doc.getTimestamp("timestamp")

                        if (
                            (sender == currentUid && receiver == otherUid) ||
                            (sender == otherUid && receiver == currentUid)
                        ) {
                            ChatMessage(sender, receiver, content, timestamp)
                        } else null
                    }

                    messages.clear()
                    messages.addAll(history)
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            //top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.back),
                        contentDescription = "Back",
                        tint = Color(0xFF780C28)
                    )
                }
                Text(
                    text = "Convo with $recipientHandle",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = poppinsFont,
                    color = Color(0xFF780C28)
                )
            }

            Divider(color = Color(0xFF780C28), thickness = 1.dp)

            // Message List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
            ) {
                itemsIndexed(messages) { index, msg ->
                    val isMe = msg.sender == currentUid
                    val showTimestamp = index == messages.lastIndex
                    val time = msg.timestamp?.toDate()?.let {
                        android.text.format.DateFormat.format("h:mm a", it).toString()
                    } ?: ""

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        Column(
                            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                            modifier = Modifier.fillMaxWidth(0.75f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isMe) Color(0xFF780C28) else Color.LightGray,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = msg.content,
                                    color = if (isMe) Color.White else Color.Black,
                                    fontFamily = poppinsFont
                                )
                            }

                            if (showTimestamp) {
                                Text(
                                    text = time,
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontFamily = poppinsFont,
                                    modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Text input bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.White)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .clickable {
                        coroutineScope.launch {
                            listState.animateScrollToItem(messages.lastIndex)
                        }
                    },
                placeholder = { Text("Type a message...", fontFamily = poppinsFont) },
                shape = RoundedCornerShape(15.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF780C28),
                    unfocusedBorderColor = Color(0xFF780C28),
                    cursorColor = Color(0xFF780C28)
                )
            )

            Button(
                onClick = {
                    if (messageText.isNotBlank()) {
                        val messageData = hashMapOf(
                            "sender" to currentUid,
                            "receiver" to otherUid,
                            "content" to messageText.trim(),
                            "timestamp" to com.google.firebase.Timestamp.now()
                        )
                        FirebaseFirestore.getInstance().collection("messages").add(messageData)
                        messageText = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF780C28))
            ) {
                Text("Send", fontFamily = poppinsFont, color = Color.White)
            }
        }
    }
}