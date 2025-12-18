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
    currentUserFirstName: String,
    onLogout: () -> Unit,
    onOpenProfile: () -> Unit
) {
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var newPost by remember { mutableStateOf("") }

    val commentTexts = remember { mutableStateMapOf<String, String>() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }

    suspend fun loadFeed() {
        loading = true
        try {
            posts = withContext(Dispatchers.IO) { AuthApi.fetchFeed() }
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { loadFeed() }

    Column(modifier = Modifier.fillMaxSize()) {

        // 🔒 FIXED TOP BAR
        TopBar(
            currentUserName = currentUserName,
            currentUserFirstName = currentUserFirstName,
            onHome = { scope.launch { loadFeed() } },
            onProfile = onOpenProfile,
            onLogout = onLogout
        )

        // 🔽 EVERYTHING BELOW SCROLLS
        LazyColumn(modifier = Modifier.fillMaxSize()) {

            // 📝 CREATE POST (SCROLLS)
            item {
                Column(modifier = Modifier.padding(16.dp)) {

                    OutlinedTextField(
                        value = newPost,
                        onValueChange = { newPost = it },
                        label = { Text("What's on your mind?") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row {
                        Button(onClick = { imagePicker.launch("image/*") }) {
                            Text(if (selectedImageUri == null) "Upload Photo" else "Photo Selected")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            enabled = newPost.isNotBlank() || selectedImageUri != null,
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        if (selectedImageUri != null) {
                                            AuthApi.createPostWithImage(
                                                context,
                                                newPost,
                                                selectedImageUri!!
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
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                }
            }

            // ⏳ LOADING
            if (loading) {
                item {
                    Text("Loading feed...", modifier = Modifier.padding(16.dp))
                }
            }

            // 📰 POSTS
            items(posts, key = { it._id }) { post ->
                val commentText = commentTexts[post._id] ?: ""

                Column(modifier = Modifier.padding(16.dp)) {

                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data("${ApiClient.BASE_URL}/dp/${post.uname}")
                                .addHeader("Cookie", ApiClient.getCookieHeader() ?: "")
                                .allowHardware(false)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${post.fName} ${post.lName}", style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(post.content)

                    if (!post.image.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data("${ApiClient.BASE_URL}/pics/${post.image}")
                                .addHeader("Cookie", ApiClient.getCookieHeader() ?: "")
                                .allowHardware(false)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(250.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        formatPostDate(post.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    post.comments.forEach {
                        Text("${it.commenter}: ${it.commentContent}", style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentTexts[post._id] = it },
                        label = { Text("Write a comment") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        enabled = commentText.isNotBlank(),
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    AuthApi.addComment(
                                        post._id,
                                        currentUserName,
                                        commentText
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