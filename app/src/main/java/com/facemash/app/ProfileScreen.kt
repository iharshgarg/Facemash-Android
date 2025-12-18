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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip


@Composable
fun ProfileScreen(
    username: String,
    currentUserName: String,
    currentUserFirstName: String,
    onBack: () -> Unit
) {

    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    // 🔑 comment text per post
    val commentTexts = remember { mutableStateMapOf<String, String>() }

    val scope = rememberCoroutineScope()

    suspend fun loadProfile() {
        loading = true
        try {
            val result = withContext(Dispatchers.IO) {
                AuthApi.fetchProfile(username)
            }
            name = result.first
            posts = result.second
        } finally {
            loading = false
        }
    }

    LaunchedEffect(username) {
        loadProfile()
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // Top bar
        TopBar(
            currentUserName = currentUserName,
            currentUserFirstName = currentUserFirstName,
            onHome = onBack,
            onProfile = { /* already here */ },
            onLogout = onBack
        )

        if (loading) {
            Text("Loading profile...", modifier = Modifier.padding(16.dp))
        } else {

            Column(modifier = Modifier.padding(16.dp)) {
                Text(name, style = MaterialTheme.typography.titleLarge)
                Text("@$username", color = MaterialTheme.colorScheme.primary)
            }

            Divider()

            LazyColumn {
                items(posts, key = { it._id }) { post ->

                    val commentText = commentTexts[post._id] ?: ""

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {

                        // 🔹 HEADER (DP + Name) — SAME AS FEED
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

                        // 🔹 POST CONTENT
                        Text(
                            post.content,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        // 🔹 POST IMAGE (IF ANY)
                        if (!post.image.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))

                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data("${ApiClient.BASE_URL}/pics/${post.image}")
                                    .addHeader("Cookie", ApiClient.getCookieHeader() ?: "")
                                    .allowHardware(false)
                                    .build(),
                                contentDescription = "Post image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                            )
                        }

                        // 🔹 DATE
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            formatPostDate(post.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 🔹 COMMENTS
                        post.comments.forEach { comment ->
                            Text(
                                "${comment.commenter}: ${comment.commentContent}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 🔹 ADD COMMENT
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
                                    loadProfile()
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