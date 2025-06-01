package com.example.pixelaura.pages

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.firebase.firestore.FieldPath

@Composable
fun OtherProfilePage(navController: NavHostController, uid: String) {
    val context = LocalContext.current
    val backendManager = remember { BackendManager(context) }
    var user by remember { mutableStateOf<User?>(null) }
    val posts = remember { mutableStateListOf<Post>() }
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    var isFollowing by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid.isBlank()) return@LaunchedEffect
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                user = doc.toObject(User::class.java)

                if (user != null) {
                    posts.clear()

                    backendManager.fetchPosts { allPosts ->
                        val originalPosts = allPosts.filter { it.uid == uid }

                        firestore.collection("reposts")
                            .whereEqualTo("repostedBy", uid)
                            .get()
                            .addOnSuccessListener { repostDocs ->
                                val repostDataMap = repostDocs.associateBy { it.getString("originalPostId") }
                                val originalPostIds = repostDataMap.keys.filterNotNull()

                                if (originalPostIds.isNotEmpty()) {
                                    firestore.collection("posts")
                                        .whereIn(FieldPath.documentId(), originalPostIds)
                                        .get()
                                        .addOnSuccessListener { postDocs ->
                                            val repostedPosts = postDocs.map { doc ->
                                                val originalPostId = doc.id
                                                val repostMeta = repostDataMap[originalPostId]
                                                val originalAuthorUid = repostMeta?.getString("originalAuthorUid")
                                                val repostTimestamp = repostMeta?.getTimestamp("timestamp")

                                                Post(
                                                    id = doc.id,
                                                    username = doc.getString("username") ?: "",
                                                    handle = doc.getString("handle") ?: "",
                                                    profileImageUrl = doc.getString("profileImageUrl") ?: "",
                                                    imageUrl = doc.getString("imageUrl"),
                                                    text = doc.getString("text"),
                                                    likes = doc.getLong("likes")?.toInt() ?: 0,
                                                    likedBy = doc.get("likedBy") as? List<String> ?: emptyList(),
                                                    reposts = doc.getLong("reposts")?.toInt() ?: 0,
                                                    repostedBy = doc.get("repostedBy") as? List<String> ?: emptyList(),
                                                    isRepost = true,
                                                    downloads = doc.getLong("downloads")?.toInt() ?: 0,
                                                    uid = doc.getString("uid") ?: "",
                                                    originalAuthorUid = originalAuthorUid,
                                                    timestamp = repostTimestamp
                                                )
                                            }


                                            val sortedPosts = (originalPosts + repostedPosts).sortedByDescending { it.timestamp }
                                            posts.addAll(sortedPosts)
                                        }
                                } else {
                                    // if no reposts, just show original posts sorted
                                    val sortedPosts = originalPosts.sortedByDescending { it.timestamp }
                                    posts.addAll(sortedPosts)
                                }
                            }
                    }

                    firestore.collection("users").document(currentUid)
                        .get()
                        .addOnSuccessListener { currentUserDoc ->
                            val followingList = currentUserDoc.get("following") as? List<*> ?: emptyList<String>()
                            isFollowing = followingList.contains(uid)
                        }
                }
            }
    }



    Scaffold(
        bottomBar = {
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
                Divider(color = Color(0xFF780C28), thickness = 1.dp)
                BottomNav(navController)
            }
        }
    ) { paddingValues ->
        val isRefreshing = remember { mutableStateOf(false) }

        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing.value),
            onRefresh = {
                isRefreshing.value = true
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener { doc ->
                        user = doc.toObject(User::class.java)

                        if (user != null) {
                            backendManager.fetchPosts { allPosts ->
                                val originalPosts = allPosts.filter { it.uid == uid }

                                FirebaseFirestore.getInstance()
                                    .collection("reposts")
                                    .whereEqualTo("repostedBy", uid)
                                    .get()
                                    .addOnSuccessListener { repostDocs ->
                                        val repostDataMap = repostDocs.associateBy { it.getString("originalPostId") }
                                        val originalPostIds = repostDataMap.keys.filterNotNull()

                                        if (originalPostIds.isNotEmpty()) {
                                            FirebaseFirestore.getInstance()
                                                .collection("posts")
                                                .whereIn(FieldPath.documentId(), originalPostIds)
                                                .get()
                                                .addOnSuccessListener { postDocs ->
                                                    val repostedPosts = postDocs.map { doc ->
                                                        val originalPostId = doc.id
                                                        val repostMeta = repostDataMap[originalPostId]
                                                        val originalAuthorUid = repostMeta?.getString("originalAuthorUid")
                                                        val repostTimestamp = repostMeta?.getTimestamp("timestamp")

                                                        Post(
                                                            id = doc.id,
                                                            username = doc.getString("username") ?: "",
                                                            handle = doc.getString("handle") ?: "",
                                                            profileImageUrl = doc.getString("profileImageUrl") ?: "",
                                                            imageUrl = doc.getString("imageUrl"),
                                                            text = doc.getString("text"),
                                                            likes = doc.getLong("likes")?.toInt() ?: 0,
                                                            likedBy = doc.get("likedBy") as? List<String> ?: emptyList(),
                                                            reposts = doc.getLong("reposts")?.toInt() ?: 0,
                                                            repostedBy = doc.get("repostedBy") as? List<String> ?: emptyList(),
                                                            isRepost = true,
                                                            downloads = doc.getLong("downloads")?.toInt() ?: 0,
                                                            uid = doc.getString("uid") ?: "",
                                                            originalAuthorUid = originalAuthorUid,
                                                            timestamp = repostTimestamp
                                                        )
                                                    }

                                                    val combined = (originalPosts + repostedPosts).sortedByDescending { it.timestamp }
                                                    posts.clear()
                                                    posts.addAll(combined)
                                                    isRefreshing.value = false
                                                }
                                        } else {
                                            // No reposts, just original posts
                                            val sorted = originalPosts.sortedByDescending { it.timestamp }
                                            posts.clear()
                                            posts.addAll(sorted)
                                            isRefreshing.value = false
                                        }
                                    }
                            }
                        } else {
                            isRefreshing.value = false
                        }
                    }
            }
        ) {
        LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    user?.let { profile ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(profile.header.ifBlank { R.drawable.sample2 })
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Header",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .background(Color.White)
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                ) {
                                    Box(modifier = Modifier.offset(y = (-60).dp)) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(profile.profile_picture.ifBlank { R.drawable.profile })
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Profile Picture",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(100.dp)
                                                .clip(CircleShape)
                                                .border(2.dp, Color.White, CircleShape)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .offset(y = (-40).dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(profile.username, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color(0xFF780C28))
                                            Text(profile.handle, color = Color.Gray, fontSize = 18.sp)
                                        }
                                        Row {
                                            IconButton(
                                                onClick = { navController.navigate("conversationPage/$uid")},
                                                modifier = Modifier
                                                    .size(45.dp)
                                                    .clip(CircleShape)
                                                    .border(1.dp, Color(0xFF780C28), CircleShape)
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.messages),
                                                    contentDescription = "Message",
                                                    modifier = Modifier.size(24.dp),
                                                    tint = Color.Unspecified
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = {
                                                    if (currentUid != null && currentUid != uid) {
                                                        BackendManager(context).toggleFollow(currentUid, uid) { nowFollowing ->
                                                            isFollowing = nowFollowing

                                                            if (nowFollowing) {
                                                                //Send notification when following
                                                                BackendManager(context).sendNotificationToUser(
                                                                    toUid = uid,
                                                                    contextType = "follow",
                                                                    senderUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                                                                )
                                                            }

                                                            Toast.makeText(
                                                                context,
                                                                if (nowFollowing) "Followed" else "Unfollowed",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                },
                                                colors = if (isFollowing)
                                                    ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                                                else
                                                    ButtonDefaults.buttonColors(containerColor = Color(0xFF780C28)),
                                                shape = RoundedCornerShape(35.dp),
                                                border = if (isFollowing) BorderStroke(1.dp, Color(0xFF780C28)) else null
                                            ) {
                                                Text(
                                                    text = if (isFollowing) "Unfollow" else "Follow",
                                                    color = if (isFollowing) Color(0xFF780C28) else Color.White,
                                                    fontFamily = poppinsFont
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = if (profile.bio.isNotBlank()) profile.bio else "No Bio",
                                        fontSize = 14.sp,
                                        color = Color(0xFF780C28),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    ) {
                                        Text("${profile.followers_count} Followers", color = Color(0xFF780C28), fontSize = 14.sp)
                                        Text("${profile.following_count} Following", color = Color(0xFF780C28), fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                        Divider(color = Color(0xFF780C28), thickness = 1.dp)
                    }
                }

                itemsIndexed(posts) { index, post ->
                    PostItem(
                        post = post,
                        index = index,
                        posts = posts,
                        currentUserHandle = FirebaseAuth.getInstance().currentUser?.uid,
                        navController = navController
                    )
                }
            }
        }
    }
}