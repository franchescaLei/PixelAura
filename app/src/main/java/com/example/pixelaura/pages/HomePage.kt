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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.pixelaura.R
import com.example.pixelaura.logic.BackendManager
import com.example.pixelaura.util.downloadImage
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(navController: NavHostController, modifier: Modifier) {
    var selectedTab by remember { mutableStateOf("For You") }
    val context = LocalContext.current
    val backendManager = remember { BackendManager(context) }
    val posts = remember { mutableStateListOf<Post>() }
    val currentUser = FirebaseAuth.getInstance().currentUser
    val profileImageUrl = currentUser?.photoUrl?.toString()
    val userHandle = remember { mutableStateOf<String?>(null) }
    val followingList = remember { mutableStateListOf<String>() }
    var profileUrl by rememberSaveable { mutableStateOf<String?>(null) }

    BackHandler {
        Toast.makeText(context, "Use the logout button to exit.", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        FirebaseFirestore.getInstance().collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                userHandle.value = doc.getString("handle")
                profileUrl = doc.getString("profile_picture")
                followingList.clear()
                (doc.get("following") as? List<*>)?.filterIsInstance<String>()?.let {
                    followingList.addAll(it)
                }
            }
    }
    LaunchedEffect(selectedTab) {
        backendManager.fetchAllPosts { fetchedPosts ->
            posts.clear()
            posts.addAll(
                if (selectedTab == "Following")
                    fetchedPosts.filter { it.uid in followingList }
                else
                    fetchedPosts
            )
        }
    }



    Scaffold(
        topBar = {
            Column(
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            ) {
                HomeHeader(navController = navController, profileImageUrl = profileUrl)
                HorizontalDivider(color = Color(0xFF780C28), thickness = 1.dp)
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                HorizontalDivider(color = Color(0xFF780C28), thickness = 1.dp)
                BottomNav(navController)
            }
        },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        clip = false
                    )
                    .clickable { navController.navigate("newPostPage") }
            ) {
                Image(painter = painterResource(id = R.drawable.plus),
                    contentDescription = "New Tweet Icon",
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Tabs(selectedTab) { newTab -> selectedTab = newTab }

            val isRefreshing = remember { mutableStateOf(false) }

            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing.value),
                onRefresh = {
                    isRefreshing.value = true
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    FirebaseFirestore.getInstance().collection("users").document(uid ?: "").get()
                        .addOnSuccessListener { doc ->
                            val following = doc.get("following") as? List<String> ?: emptyList()
                            backendManager.fetchAllPosts { fetchedPosts ->
                                posts.clear()
                                posts.addAll(
                                    if (selectedTab == "Following")
                                        fetchedPosts.filter { it.uid in following }
                                    else
                                        fetchedPosts
                                )
                                isRefreshing.value = false
                            }
                        }
                }
            )
            {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(posts) { index, post ->
                        PostItem(post = post, index = index, posts = posts, currentUserHandle = userHandle.value, navController)
                    }
                }
            }
        }
    }
}

@Composable
fun HomeHeader(navController: NavHostController, profileImageUrl: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { navController.navigate("ownProfilePage")}, modifier = Modifier.padding(start = 3.dp)) {
            val imageModel = ImageRequest.Builder(LocalContext.current)
                .data(profileImageUrl ?: R.drawable.profile)
                .crossfade(true)
                .build()
            val fallback = profileImageUrl.isNullOrBlank()


            Box(
                modifier = Modifier
                    .padding(start = 3.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (fallback) Color(0xFFE0E0E0) else Color.Transparent)
                    .clickable { navController.navigate("ownProfilePage") }
            ) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

        }
        Text(
            text = "Home",
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

@Composable
fun Tabs(selectedTab: String, onTabSelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        TabItem("For You", selectedTab, onTabSelected)
        TabItem("Following", selectedTab, onTabSelected)
    }
}

@Composable
fun TabItem(tabName: String, selectedTab: String, onTabSelected: (String) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onTabSelected(tabName) }
    ) {
        Text(
            text = tabName,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = poppinsFont,
            color = Color(0xFF780C28)
        )
        if (selectedTab == tabName) {
            Box(
                modifier = Modifier
                    .height(1.dp)
                    .width(45.dp)
                    .background(Color(0xFF780C28))
            )
        }
    }
}
@Composable
fun PostItem(post: Post, index: Int, posts: SnapshotStateList<Post>, currentUserHandle: String?,  navController: NavHostController )
{

    if (post.isRepost) {
        Text(
            text = "Reposted",
            fontSize = 13.sp,
            color = Color.Gray,
            fontFamily = poppinsFont,
            modifier = Modifier.padding(start = 52.dp, bottom = 4.dp)
        )
    }

    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    val likeState = remember { mutableStateOf(post.likedBy.contains(currentUser?.uid)) }
    val likeCount = remember { mutableStateOf(post.likes) }

    val repostState = remember { mutableStateOf(post.repostedBy.contains(currentUser?.uid)) }
    val repostCount = remember { mutableStateOf(post.reposts) }

    val postRef = FirebaseFirestore.getInstance().collection("posts").document(post.id)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(
                        if (post.profileImageUrl.isNullOrBlank())
                            R.drawable.profile
                        else
                            post.profileImageUrl
                    )
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(45.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = post.username,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = poppinsFont,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF780C28)
                )
                Spacer(modifier = Modifier.width(15.dp))
                Text(
                    text = post.handle,
                    fontFamily = poppinsFont,
                    color = Color(0xFF780C28),
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            var expanded by remember { mutableStateOf(false) }

            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.more),
                        contentDescription = "More Options",
                        modifier = Modifier.size(23.dp),
                        tint = Color(0xFF780C28)
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    offset = DpOffset(0.dp, 8.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Go to Profile", fontFamily = poppinsFont) },
                        onClick = {
                            val targetUid = if (post.isRepost && !post.originalAuthorUid.isNullOrBlank()) {
                                post.originalAuthorUid
                            } else {
                                post.uid
                            }

                            if (targetUid == FirebaseAuth.getInstance().currentUser?.uid) {
                                navController.navigate("ownProfilePage")
                            } else {
                                navController.navigate("otherProfilePage/$targetUid")
                            }
                        }
                    )
                }
            }
        }

        post.text?.let {
            Text(
                text = it,
                modifier = Modifier.padding(start = 52.dp),
                fontFamily = poppinsFont,
                fontSize = 16.sp,
                color = Color(0xFF780C28)
            )
        }

        post.imageUrl?.let { url ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = "Post Image",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(start = 52.dp)
                    .clip(RoundedCornerShape(13.dp))
            )
        }

        // ACTION ROW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 65.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(50.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Repost
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (currentUser?.uid == post.uid || currentUser?.uid == post.originalAuthorUid) {
                        Toast.makeText(context, "You can't repost your own post.", Toast.LENGTH_SHORT).show()
                        return@IconButton
                    }

                    FirebaseFirestore.getInstance().runTransaction { transaction ->
                        if (currentUser == null) return@runTransaction

                        val snapshot = transaction.get(postRef)
                        val repostedBy = snapshot.get("repostedBy") as? MutableList<String> ?: mutableListOf()
                        val reposts = snapshot.getLong("reposts")?.toInt() ?: 0

                        if (currentUser.uid in repostedBy) {
                            repostedBy.remove(currentUser.uid)
                            transaction.update(postRef, mapOf("repostedBy" to repostedBy, "reposts" to reposts - 1))

                            // Delete from reposts collection
                            FirebaseFirestore.getInstance().collection("reposts")
                                .whereEqualTo("originalPostId", post.id)
                                .whereEqualTo("repostedBy", currentUser.uid)
                                .get()
                                .addOnSuccessListener { snapshot ->
                                    for (doc in snapshot.documents) {
                                        doc.reference.delete()
                                    }
                                }

                            repostState.value = false
                            repostCount.value = reposts - 1
                            posts[index] = posts[index].copy(
                                reposts = reposts - 1,
                                repostedBy = repostedBy.toList()
                            )

                        } else {
                            repostedBy.add(currentUser.uid)
                            transaction.update(postRef, mapOf("repostedBy" to repostedBy, "reposts" to reposts + 1))

                            // Add to reposts collection
                            val repostData = hashMapOf(
                                "originalPostId" to post.id,
                                "originalAuthorUid" to (post.originalAuthorUid ?: post.uid),
                                "repostedBy" to currentUser.uid,
                                "username" to post.username,
                                "handle" to post.handle,
                                "profileImageUrl" to post.profileImageUrl,
                                "text" to post.text,
                                "imageUrl" to post.imageUrl,
                                "timestamp" to com.google.firebase.Timestamp.now()
                            )

                            FirebaseFirestore.getInstance().collection("reposts").add(repostData)

                            // trigger Notification
                            BackendManager(context).sendNotificationToUser(
                                toUid = post.uid,
                                contextType = "repost",
                                senderUid = currentUser.uid
                            )

                            repostState.value = true
                            repostCount.value = reposts + 1
                            posts[index] = posts[index].copy(
                                reposts = reposts + 1,
                                repostedBy = repostedBy.toList()
                            )
                        }
                    }
                }) {
                    Icon(
                        painter = painterResource(
                            id = if (repostState.value) R.drawable.reposted else R.drawable.repost
                        ),
                        contentDescription = "Repost",
                        modifier = Modifier.size(28.dp),
                        tint = Color(0xFF780C28)
                    )
                }

                Text(
                    text = "${repostCount.value}",
                    fontSize = 14.sp,
                    fontFamily = poppinsFont,
                    color = Color(0xFF780C28)
                )
            }

            // Like
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    val currentUser = FirebaseAuth.getInstance().currentUser

                    // Prevent self-like
                    if (currentUser?.uid == post.uid) {
                        Toast.makeText(context, "You can't like your own post.", Toast.LENGTH_SHORT).show()
                        return@IconButton
                    }

                    FirebaseFirestore.getInstance().runTransaction { transaction ->
                        if (currentUser == null) return@runTransaction

                        val snapshot = transaction.get(postRef)
                        val likedBy = snapshot.get("likedBy") as? MutableList<String> ?: mutableListOf()
                        val likes = snapshot.getLong("likes")?.toInt() ?: 0

                        if (currentUser.uid in likedBy) {
                            likedBy.remove(currentUser.uid)
                            transaction.update(postRef, mapOf("likedBy" to likedBy, "likes" to likes - 1))
                            likeState.value = false
                            likeCount.value = likes - 1

                            posts[index] = posts[index].copy(
                                likes = likes - 1,
                                likedBy = likedBy.toList()
                            )
                        } else {
                            likedBy.add(currentUser.uid)
                            transaction.update(postRef, mapOf("likedBy" to likedBy, "likes" to likes + 1))
                            likeState.value = true
                            likeCount.value = likes + 1

                            posts[index] = posts[index].copy(
                                likes = likes + 1,
                                likedBy = likedBy.toList()
                            )

                            BackendManager(context).sendNotificationToUser(
                                toUid = post.uid,
                                contextType = "like",
                                senderUid = currentUser.uid
                            )
                        }
                    }
                }) {
                    Icon(
                        painter = painterResource(id = if (likeState.value) R.drawable.heart else R.drawable.unheart),
                        contentDescription = "Like",
                        modifier = Modifier.size(28.dp),
                        tint = Color(0xFF780C28)
                    )
                }

                Text(
                    "${likeCount.value}",
                    fontSize = 14.sp,
                    fontFamily = poppinsFont,
                    color = Color(0xFF780C28)
                )
            }

            // Download
            Row(verticalAlignment = Alignment.CenterVertically) {
                val downloadCount = remember { mutableStateOf(post.downloads) }
                IconButton(onClick = {
                    if (!post.imageUrl.isNullOrBlank()) {
                        downloadImage(context, post.imageUrl)

                        val postRef = FirebaseFirestore.getInstance().collection("posts").document(post.id)

                        FirebaseFirestore.getInstance().runTransaction { transaction ->
                            val snapshot = transaction.get(postRef)
                            val current = snapshot.getLong("downloads") ?: 0
                            val updated = current + 1

                            transaction.update(postRef, "downloads", updated)

                            updated
                        }.addOnSuccessListener { updatedCount ->
                            downloadCount.value = updatedCount.toInt()
                            BackendManager(context).sendNotificationToUser(
                                toUid = post.uid,
                                contextType = "download",
                                senderUid = FirebaseAuth.getInstance().currentUser?.uid
                            )
                        }
                    } else {
                        Toast.makeText(context, "No image to download.", Toast.LENGTH_SHORT).show()
                    }
                }){
                    Icon(
                        painter = painterResource(id = R.drawable.download),
                        contentDescription = "Download",
                        modifier = Modifier.size(28.dp),
                        tint = Color(0xFF780C28)
                    )
                }

                Text("${downloadCount.value}", fontSize = 14.sp, fontFamily = poppinsFont, color = Color(0xFF780C28))
            }

        }

        Divider(color = Color(0xFF780C28), thickness = 0.5.dp)
    }
}





@Composable
fun BottomNav(navController: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.White)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute == "homePage") {
                navController.popBackStack("homePage", inclusive = true)
                navController.navigate("homePage")
            } else {
                navController.navigate("homePage") {
                    launchSingleTop = true
                }
            }
        }, modifier = Modifier.padding(start = 3.dp)) {
            Icon(
                painter = painterResource(id = R.drawable.home),
                contentDescription = "Home",
                modifier = Modifier.size(33.dp),
                tint = Color(0xFF780C28)
            )
        }
        IconButton(onClick = { navController.navigate("searchPage") }) {
            Icon(
                painter = painterResource(id = R.drawable.search),
                contentDescription = "Search",
                modifier = Modifier.size(33.dp),
                tint = Color(0xFF780C28)
            )
        }
        IconButton(onClick = { navController.navigate("peersPage") }) {
            Icon(
                painter = painterResource(id = R.drawable.communities),
                contentDescription = "Chat",
                modifier = Modifier.size(33.dp),
                tint = Color(0xFF780C28)
            )
        }
        IconButton(onClick = { navController.navigate("notificationsPage") }) {
            Icon(
                painter = painterResource(id = R.drawable.notiff),
                contentDescription = "Notifications",
                modifier = Modifier.size(33.dp),
                tint = Color(0xFF780C28)
            )
        }
        IconButton(onClick = { navController.navigate("messagesPage")}, modifier = Modifier.padding(end = 3.dp)) {
            Icon(
                painter = painterResource(id = R.drawable.messages),
                contentDescription = "Menu",
                modifier = Modifier.size(33.dp),
                tint = Color(0xFF780C28)
            )
        }
    }
}

data class Post(
    val id: String = "",
    val username: String,
    val handle: String,
    val profileImage: Int? = null,
    val text: String? = null,
    val imageResId: Int? = null,
    val profileImageUrl: String,
    val imageUrl: String?,
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val reposts: Int = 0,
    val repostedBy: List<String> = emptyList(),
    val isRepost: Boolean = false,
    val timestamp: com.google.firebase.Timestamp? = null,
    val downloads: Int = 0,
    val uid: String = "",
    val originalAuthorUid: String? = null


)


@Preview(showBackground = true)
@Composable
fun PreviewHome() {
    HomePage(navController = rememberNavController(), modifier = Modifier.padding())
}
