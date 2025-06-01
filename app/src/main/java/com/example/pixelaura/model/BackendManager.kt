package com.example.pixelaura.logic

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import com.example.pixelaura.pages.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.io.InputStream


class BackendManager(
    private val context: Context,
    private val onSuccess: () -> Unit = {},
    private val onError: (String) -> Unit = {}
) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val client = OkHttpClient()

    //for image uploading
    private val imgurClientId = "dc2b464d8786825"



    fun uploadPost(text: String, imageUri: Uri?) {
        if (imageUri == null && text.isBlank()) {
            Toast.makeText(context, "Please enter text or select an image.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val userDoc = doc.data
                val username = userDoc?.get("username") as? String ?: "Anonymous"
                val handle = (userDoc?.get("handle") as? String)?.ifBlank { "@" + uid.take(5) }
                    ?: "@" + uid.take(6)
                val profileImageUrl =
                    (userDoc?.get("profile_picture") as? String).takeUnless { it.isNullOrBlank() }
                        ?: "https://placehold.co/100x100.png"

                if (imageUri != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val imageStream: InputStream? =
                                context.contentResolver.openInputStream(imageUri)
                            val imageBytes = imageStream?.readBytes()
                            val base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT)

                            val requestBody = MultipartBody.Builder()
                                .setType(MultipartBody.FORM)
                                .addFormDataPart("image", base64Image)
                                .build()

                            val request = Request.Builder()
                                .url("https://api.imgur.com/3/image")
                                .addHeader("Authorization", "Client-ID $imgurClientId")
                                .post(requestBody)
                                .build()

                            val response = client.newCall(request).execute()
                            val responseBody = response.body?.string()

                            if (response.isSuccessful && responseBody != null) {
                                val imageUrl =
                                    Regex(""""link":"(.*?)"""").find(responseBody)?.groups?.get(1)?.value?.replace(
                                        "\\/",
                                        "/"
                                    )

                                if (imageUrl != null) {
                                    savePostToFirestore(
                                        text,
                                        imageUrl
                                    )
                                } else {
                                    onError("Failed to extract image URL from Imgur response.")
                                }
                            } else {
                                onError("Imgur upload failed: ${response.message}")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            onError("Error uploading image: ${e.message}")
                        }
                    }
                } else {
                    savePostToFirestore(text, null)
                }
            }
            .addOnFailureListener {
                onError("Failed to fetch user data: ${it.message}")
            }
    }

    private fun savePostToFirestore(
        text: String,
        imageUrl: String?
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid ?: return

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                val username = userDoc.getString("username") ?: "anonymous"
                val handle = userDoc.getString("handle") ?: "@${uid.take(6)}"
                val profileImageUrl = userDoc.getString("profile_picture") ?: ""

                val post = hashMapOf(
                    "uid" to uid,
                    "username" to username,
                    "handle" to handle,
                    "profileImageUrl" to profileImageUrl,
                    "text" to text,
                    "imageUrl" to imageUrl,
                    "likes" to 0,
                    "likedBy" to emptyList<String>(),
                    "reposts" to 0,
                    "repostedBy" to emptyList<String>(),
                    "downloads" to 0,
                    "timestamp" to com.google.firebase.Timestamp.now()
                )


                firestore.collection("posts").add(post)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Post uploaded", Toast.LENGTH_SHORT).show()
                        onSuccess()
                    }
                    .addOnFailureListener {
                        onError("Failed to save post")
                    }
            }
            .addOnFailureListener {
                onError("Failed to retrieve user data")
            }
    }

    //fetch posts for repost
    fun fetchPosts(onPostsFetched: (List<Post>) -> Unit) {
        firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val posts = snapshot.documents.mapNotNull { doc ->
                    val id = doc.id
                    val username = doc.getString("username") ?: return@mapNotNull null
                    val handle = doc.getString("handle") ?: return@mapNotNull null
                    val text = doc.getString("text")
                    val profileImageUrl = doc.getString("profileImageUrl") ?: ""
                    val imageUrl = doc.getString("imageUrl")
                    val likes = doc.getLong("likes")?.toInt() ?: 0
                    val likedBy = doc.get("likedBy") as? List<String> ?: emptyList()
                    val reposts = doc.getLong("reposts")?.toInt() ?: 0
                    val repostedBy = doc.get("repostedBy") as? List<String> ?: emptyList()
                    val uid = doc.getString("uid") ?: return@mapNotNull null
                    val downloads = doc.getLong("downloads")?.toInt() ?: 0


                    Post(
                        id = id,
                        username = username,
                        handle = handle,
                        text = text,
                        profileImageUrl = profileImageUrl,
                        imageUrl = imageUrl,
                        likes = likes,
                        likedBy = likedBy,
                        reposts = reposts,
                        repostedBy = repostedBy,
                        uid = uid,
                        downloads = downloads,
                        originalAuthorUid = doc.getString("uid"),
                        timestamp = doc.getTimestamp("timestamp")
                    )
                }
                onPostsFetched(posts)
            }
            .addOnFailureListener {
                onError("Failed to fetch posts: ${it.message}")
            }
    }

    //fetch all posts
    fun fetchAllPosts(onPostsFetched: (List<Post>) -> Unit) {
        firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val posts = snapshot.documents.mapNotNull { doc ->
                    val id = doc.id
                    val username = doc.getString("username") ?: return@mapNotNull null
                    val handle = doc.getString("handle") ?: return@mapNotNull null
                    val text = doc.getString("text")
                    val profileImageUrl = doc.getString("profileImageUrl") ?: ""
                    val imageUrl = doc.getString("imageUrl")
                    val likes = doc.getLong("likes")?.toInt() ?: 0
                    val likedBy = doc.get("likedBy") as? List<String> ?: emptyList()
                    val reposts = doc.getLong("reposts")?.toInt() ?: 0
                    val repostedBy = doc.get("repostedBy") as? List<String> ?: emptyList()
                    val uid = doc.getString("uid") ?: ""
                    val downloads = doc.getLong("downloads")?.toInt() ?: 0

                    Post(
                        id = id,
                        username = username,
                        handle = handle,
                        text = text,
                        profileImageUrl = profileImageUrl,
                        imageUrl = imageUrl,
                        likes = likes,
                        likedBy = likedBy,
                        reposts = reposts,
                        repostedBy = repostedBy,
                        downloads = downloads,
                        uid = uid,
                        originalAuthorUid = doc.getString("uid"),
                        timestamp = doc.getTimestamp("timestamp")
                    )

                }
                onPostsFetched(posts)
            }
            .addOnFailureListener {
                onError("Failed to fetch posts: ${it.message}")
            }
    }

    suspend fun uploadImageToImgur(uri: Uri): String? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val bytes = inputStream.readBytes()
        val base64 = Base64.encodeToString(bytes, Base64.DEFAULT)

        val requestBody = FormBody.Builder()
            .add("image", base64)
            .build()

        val request = Request.Builder()
            .url("https://api.imgur.com/3/image")
            .addHeader("Authorization", "Client-ID $imgurClientId") // ✅ Fix here
            .post(requestBody)
            .build()

        val response = OkHttpClient().newCall(request).execute()
        val responseBody = response.body?.string()

        if (responseBody == null) {
            println("Imgur upload failed: empty response")
            return null
        }

        try {
            val json = JSONObject(responseBody)
            if (json.optBoolean("success", false)) {
                return json.getJSONObject("data").getString("link")
            } else {
                println("Imgur response unsuccessful: $responseBody")
            }
        } catch (e: Exception) {
            println("Error parsing Imgur response: $responseBody")
            e.printStackTrace()
        }

        return null
    }

    //follow users
    fun toggleFollow(currentUid: String, targetUid: String, onComplete: (Boolean) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        val currentUserRef = firestore.collection("users").document(currentUid)
        val targetUserRef = firestore.collection("users").document(targetUid)

        firestore.runTransaction { transaction ->
            val currentUserSnapshot = transaction.get(currentUserRef)
            val targetUserSnapshot = transaction.get(targetUserRef)

            val following = (currentUserSnapshot.get("following") as? MutableList<String>)?.toMutableList()
                ?: mutableListOf<String>().also {
                    transaction.update(currentUserRef, "following", it)
                }

            val followingCount = currentUserSnapshot.getLong("following_count")?.toInt() ?: 0
            val followersCount = targetUserSnapshot.getLong("followers_count")?.toInt() ?: 0

            val isFollowing = targetUid in following

            if (isFollowing) {
                following.remove(targetUid)
                transaction.update(currentUserRef, mapOf(
                    "following" to following,
                    "following_count" to followingCount - 1
                ))
                transaction.update(targetUserRef, "followers_count", followersCount - 1)
            } else {
                following.add(targetUid)
                transaction.update(currentUserRef, mapOf(
                    "following" to following,
                    "following_count" to followingCount + 1
                ))
                transaction.update(targetUserRef, "followers_count", followersCount + 1)
            }

            isFollowing
        }.addOnSuccessListener { wasFollowing ->
            onComplete(!wasFollowing)
        }.addOnFailureListener {
            onComplete(false)
        }
    }

    //notiffs
    fun sendNotificationToUser(
        toUid: String,
        contextType: String,
        senderUid: String?
    ) {
        if (toUid.isBlank() || senderUid.isNullOrBlank()) return

        val firestore = FirebaseFirestore.getInstance()
        val usersRef = firestore.collection("users")

        usersRef.document(senderUid).get().addOnSuccessListener { senderDoc ->
            val senderHandle = senderDoc.getString("handle") ?: "@user"
            val message = when (contextType) {
                "like" -> "$senderHandle liked your post"
                "repost" -> "$senderHandle reposted your post"
                "download" -> "$senderHandle downloaded your post"
                "follow" -> "$senderHandle followed you"
                "message" -> "$senderHandle sent you a message"
                else -> "$senderHandle did something"
            }

            val notifData = hashMapOf(
                "context" to contextType,
                "senderUid" to senderUid,
                "receiverUid" to toUid,
                "message" to message,
                "timestamp" to com.google.firebase.Timestamp.now()
            )

            firestore.collection("users")
                .document(toUid)
                .collection("notifications")
                .add(notifData)
        }
    }


}
