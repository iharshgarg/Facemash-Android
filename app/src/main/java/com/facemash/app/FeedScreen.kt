package com.facemash.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
@Composable
fun FeedScreen(
    currentUserName: String,
    onLogout: () -> Unit,
    onOpenProfile: () -> Unit
) {

    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var newPost by remember { mutableStateOf("") }

    // 🔑 store comment text per post ID
    val commentTexts = remember { mutableStateMapOf<String, String>() }

    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
    }

    suspend fun loadFeed() {
        loading = true
        try {
            posts = withContext(Dispatchers.IO) {
                AuthApi.fetchFeed()
            }
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        loadFeed()
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onOpenProfile) {
                Text("Facemash", style = MaterialTheme.typography.headlineSmall)
            }
            Button(onClick = onLogout) {
                Text("Logout")
            }
        }

        // Create Post
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = newPost,
                onValueChange = { newPost = it },
                label = { Text("What's on your mind?") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                enabled = newPost.isNotBlank() || selectedImageUri != null,
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            if (selectedImageUri != null) {
                                AuthApi.createPostWithImage(
                                    context = context,
                                    content = newPost,
                                    imageUri = selectedImageUri!!
                                )
                            } else {
                                AuthApi.createPost(newPost)
                            }
                        }

                        newPost = ""
                        selectedImageUri = null
                        loadFeed()
                    }
                }
            ) {
                Text("Post")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { imagePicker.launch("image/*") }) {
                Text(
                    if (selectedImageUri == null)
                        "Pick Image"
                    else
                        "Image Selected"
                )
            }
        }

        Divider()

        if (loading) {
            Text("Loading feed...", modifier = Modifier.padding(16.dp))
        } else {
            val context = LocalContext.current
            val imageLoader = remember {
                ImageLoaderProvider.get(context)
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(posts, key = { it._id }) { post ->

                    val commentText = commentTexts[post._id] ?: ""

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {

                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {

                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data("${ApiClient.BASE_URL}/dp/${post.uname}")
                                    .addHeader("Cookie", ApiClient.getCookieHeader() ?: "")
                                    .allowHardware(false)
                                    .build(),
                                contentDescription = "Profile picture",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                "${post.fName} ${post.lName}",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(post.content)

                        if (!post.image.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))

                            val cookie = ApiClient.getCookieHeader()

                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data("${ApiClient.BASE_URL}/pics/${post.image}")
                                    .addHeader("Cookie", ApiClient.getCookieHeader() ?: "")
                                    .allowHardware(false)
                                    .listener(
                                        onStart = {
                                            println("IMAGE STARTED: ${post.image}")
                                        },
                                        onSuccess = { _, _ ->
                                            println("IMAGE SUCCESS: ${post.image}")
                                        },
                                        onError = { _, result ->
                                            println("IMAGE FAILED: ${post.image}")
                                            println("ERROR: ${result.throwable}")
                                        }
                                    )
                                    .build(),
                                contentDescription = "Post image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Comments
                        post.comments.forEach { comment ->
                            Text(
                                "${comment.commenter}: ${comment.commentContent}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Add comment
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = {
                                commentTexts[post._id] = it
                            },
                            label = { Text("Write a comment") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            enabled = commentText.isNotBlank(),
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        AuthApi.addComment(
                                            postId = post._id,
                                            commenter = currentUserName,
                                            commentContent = commentText
                                        )
                                    }
                                    commentTexts[post._id] = ""
                                    loadFeed()
                                }
                            }
                        ) {
                            Text("Comment")
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                    }
                }
            }
        }
    }
}