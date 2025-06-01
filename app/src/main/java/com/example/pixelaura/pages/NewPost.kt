package com.example.pixelaura.pages

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.pixelaura.R
import com.example.pixelaura.logic.BackendManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPostActivity(navController: NavHostController) {
    val context = LocalContext.current

    var textContent by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var profileUrl by remember { mutableStateOf<String?>(null) }


    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(currentUid) {
        currentUid?.let { uid ->
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    profileUrl = doc.getString("profile_picture")
                }
        }
    }


    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            "New Post",
                            fontSize = 22.sp,
                            color = Color(0xFF780C28),
                            fontFamily = poppinsFont
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.back),
                                contentDescription = "Back",
                                tint = Color(0xFF780C28)
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            enabled = !uploading,
                            onClick = {
                                if (imageUri != null || textContent.isNotBlank()) {
                                    uploading = true
                                    val backend = BackendManager(
                                        context = context,
                                        onSuccess = {
                                            uploading = false
                                            navController.popBackStack()
                                        },
                                        onError = {
                                            uploading = false
                                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    backend.uploadPost(text = textContent, imageUri = imageUri)
                                } else {
                                    Toast.makeText(context, "Please enter text or select an image.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text(
                                "Post",
                                color = Color(0xFF780C28),
                                fontSize = 20.sp,
                                fontFamily = poppinsFont
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
                Divider(color = Color(0xFF780C28), thickness = 1.dp)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                val painter = rememberAsyncImagePainter(
                    model = profileUrl?.takeIf { it.isNotBlank() } ?: R.drawable.profile
                )

                Image(
                    painter = painter,
                    contentDescription = "User Profile",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(8.dp))

                TextField(
                    value = textContent,
                    onValueChange = { textContent = it },
                    placeholder = {
                        Text(
                            "What's on your mind?",
                            color = Color(0xFF780C28),
                            fontFamily = poppinsFont
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(150.dp),
                    colors = TextFieldDefaults.textFieldColors(
                        focusedTextColor = Color(0xFF780C28),
                        containerColor = Color.Transparent,
                        focusedIndicatorColor = Color(0xFF780C28),
                        unfocusedIndicatorColor = Color(0xFF780C28)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            imageUri?.let { uri ->
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = "Selected Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(15.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable {
                            imagePickerLauncher.launch("image/*")
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Image",
                        fontSize = 14.sp,
                        color = Color(0xFF780C28),
                        fontFamily = poppinsFont
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.plus),
                        contentDescription = "Add Image",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

        }
        }
    }


@Preview(showBackground = true)
@Composable
fun PreviewNewPostActivity() {
    val navController = rememberNavController()
    NewPostActivity(navController = navController)
}
