package com.example.pixelaura.pages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.pixelaura.R
import com.example.pixelaura.logic.BackendManager
import com.example.pixelaura.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnProfilePage(navController: NavHostController) {
    val userHandle = remember { mutableStateOf<String?>(null) }
    var user by remember { mutableStateOf<User?>(null) }
    val context = LocalContext.current
    val backendManager = remember { BackendManager(context) }
    val posts = remember { mutableStateListOf<Post>() }
    val showEditDialog = remember { mutableStateOf(false) }
    var headerUri by remember { mutableStateOf<Uri?>(null) }
    var profileUri by remember { mutableStateOf<Uri?>(null) }
    var bio by remember { mutableStateOf("") }


    val headerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        headerUri = uri
    }

    val profilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        profileUri = uri
    }
    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid
        if (uid != null) {
            // Get user profile
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    user = doc.toObject(User::class.java)

                    val handle = doc.getString("handle")
                    userHandle.value = handle

                    val fetchedBio = doc.getString("bio")
                    if (fetchedBio != null) {
                        bio = fetchedBio
                    }
                }

            // Get original posts by this user
            backendManager.fetchPosts { allPosts ->
                val originalPosts = allPosts.filter { it.uid == uid }

                // Get reposts
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

                                    val allCombined = (originalPosts + repostedPosts).sortedByDescending { it.timestamp }
                                    posts.clear()
                                    posts.addAll(allCombined)
                                }
                        } else {
                            // No reposts, just show original posts sorted
                            val sorted = originalPosts.sortedByDescending { it.timestamp }
                            posts.clear()
                            posts.addAll(sorted)
                        }
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
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    FirebaseFirestore.getInstance().collection("users").document(uid)
                        .get()
                        .addOnSuccessListener { doc ->
                            user = doc.toObject(User::class.java)
                            bio = doc.getString("bio") ?: ""
                            userHandle.value = doc.getString("handle")
                        }

                    backendManager.fetchPosts { allPosts ->
                        val originalPosts = allPosts.filter { it.uid == uid }

                        FirebaseFirestore.getInstance().collection("reposts")
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

                                            val allCombined = (originalPosts + repostedPosts).sortedByDescending { it.timestamp }
                                            posts.clear()
                                            posts.addAll(allCombined)
                                            isRefreshing.value = false
                                        }
                                } else {
                                    // No reposts
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
                                        val profilePictureModel = profile.profile_picture.takeIf { !it.isNullOrBlank() } ?: R.drawable.profile

                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(profilePictureModel)
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
                                        Button(
                                            onClick = { showEditDialog.value = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF780C28))
                                        ) {
                                            Text("Edit Profile", color = Color.White, fontSize = 14.sp)
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
                        post = post, index = index, posts = posts,
                        currentUserHandle = userHandle.value, navController
                    )
                }
            }
        }

    }



    if (showEditDialog.value) {
        var username by remember { mutableStateOf(user?.username ?: "") }
        var handle by remember { mutableStateOf(user?.handle ?: "") }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Box {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            "Edit Profile",
                            fontSize = 22.sp,
                            color = Color(0xFF780C28),
                            fontWeight = FontWeight.Bold,
                            fontFamily = poppinsFont
                        )

                        Text("Profile Picture", color = Color(0xFF780C28), fontFamily = poppinsFont)
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray)
                                .clickable { profilePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (profileUri != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(profileUri),
                                    contentDescription = "PFP Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text("Tap", color = Color(0xFF780C28), fontSize = 12.sp)
                            }
                        }

                        Text("Header", color = Color(0xFF780C28), fontFamily = poppinsFont)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.LightGray)
                                .clickable { headerPickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (headerUri != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(headerUri),
                                    contentDescription = "Header Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text("Tap to select header", color = Color.DarkGray, fontSize = 14.sp)
                            }
                        }

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color(0xFF780C28),
                                unfocusedBorderColor = Color(0xFF780C28)
                            )
                        )

                        OutlinedTextField(
                            value = handle,
                            onValueChange = { handle = it },
                            label = { Text("Handle") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color(0xFF780C28),
                                unfocusedBorderColor = Color(0xFF780C28)
                            )
                        )
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { newText: String -> bio = newText },
                            label = { Text("Bio") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            singleLine = false,
                            maxLines = 4,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color(0xFF780C28),
                                unfocusedBorderColor = Color(0xFF780C28)
                            )
                        )
                        Button(
                            onClick = {
                                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@Button
                                val userRef = FirebaseFirestore.getInstance().collection("users").document(uid)

                                val updates = mutableMapOf<String, Any>(
                                    "username" to username,
                                    "handle" to handle,
                                    "bio" to bio
                                )

                                CoroutineScope(Dispatchers.IO).launch {
                                    headerUri?.let {
                                        backendManager.uploadImageToImgur(it)?.let { url ->
                                            updates["header"] = url
                                        }
                                    }

                                    var newPfpUrl: String? = null
                                    profileUri?.let {
                                        backendManager.uploadImageToImgur(it)?.let { url ->
                                            updates["profile_picture"] = url
                                            newPfpUrl = url
                                        }
                                    }

                                    // Update user Firestore doc
                                    withContext(Dispatchers.Main) {
                                        userRef.update(updates)
                                            .addOnSuccessListener {
                                                // Also update all user's posts
                                                FirebaseFirestore.getInstance()
                                                    .collection("posts")
                                                    .whereEqualTo("uid", uid)
                                                    .get()
                                                    .addOnSuccessListener { snapshot ->
                                                        for (doc in snapshot.documents) {
                                                            val postUpdates = mutableMapOf<String, Any>(
                                                                "username" to username,
                                                                "handle" to handle
                                                            )
                                                            newPfpUrl?.let { postUpdates["profileImageUrl"] = it }

                                                            doc.reference.update(postUpdates)
                                                        }
                                                    }

                                                // Refresh UI
                                                backendManager.fetchPosts { allPosts ->
                                                    posts.clear()
                                                    posts.addAll(allPosts.filter { it.uid == uid })

                                                    // Fetch and append reposts
                                                    FirebaseFirestore.getInstance()
                                                        .collection("reposts")
                                                        .whereEqualTo("repostedBy", uid)
                                                        .get()
                                                        .addOnSuccessListener { repostDocs ->
                                                            val originalPostIds = repostDocs.mapNotNull { it.getString("originalPostId") }

                                                            if (originalPostIds.isNotEmpty()) {
                                                                FirebaseFirestore.getInstance()
                                                                    .collection("posts")
                                                                    .whereIn(FieldPath.documentId(), originalPostIds)
                                                                    .get()
                                                                    .addOnSuccessListener { postDocs ->
                                                                        val repostedPosts = postDocs.map { doc ->
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
                                                                                uid = doc.getString("uid") ?: ""
                                                                            )
                                                                        }

                                                                        posts.addAll(repostedPosts)
                                                                    }
                                                            }
                                                        }
                                                }

                                                showEditDialog.value = false
                                            }
                                    }
                                }
                            }
                            ,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF780C28)),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Save", color = Color.White)
                        }
                    }

                    IconButton(
                        onClick = { showEditDialog.value = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = "Close",
                            tint = Color.Unspecified
                        )
                    }
                }
            }
        }

    }

}

@Preview(showBackground = true)
@Composable
fun PreviewOwnProfilePage() {
    OwnProfilePage(navController = rememberNavController())
}
