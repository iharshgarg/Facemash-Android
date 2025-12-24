package com.facemash.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.layout.ContentScale

@Composable
fun ProfileScreen(
    username: String,                 // 👤 profile being viewed
    currentUsername: String,          // 👤 logged-in uname
    currentUserFirstName: String,     // 👤 logged-in first name
    currentUserFullName: String,      // 👤 logged-in full name
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onOpenMyProfile: () -> Unit,
    onLogout: () -> Unit
) {

    val context = LocalContext.current

    var displayName by remember { mutableStateOf("") }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    val commentTexts = remember { mutableStateMapOf<String, String>() }
    val scope = rememberCoroutineScope()

    suspend fun loadProfile() {
        loading = true
        try {
            val result = withContext(Dispatchers.IO) {
                AuthApi.fetchProfile(username)
            }
            displayName = result.first
            posts = result.second
        } finally {
            loading = false
        }
    }

    LaunchedEffect(username) {
        loadProfile()
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // 🔒 FIXED TOP BAR
        TopBar(
            currentUsername = currentUsername,
            currentUserFirstName = currentUserFirstName,
            onHome = onBack,
            onProfile = onOpenMyProfile,   // 🔑 ALWAYS GO TO MY PROFILE
            onSearch = onSearch,
            onLogout = onLogout
        )

        // 🔽 EVERYTHING BELOW SCROLLS
        LazyColumn(modifier = Modifier.fillMaxSize()) {

            // 👤 PROFILE HEADER
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        displayName,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "@$username",
                        color = MaterialTheme.colorScheme.primary
                    )
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                }
            }

            // ⏳ LOADING
            if (loading) {
                item {
                    Text(
                        "Loading profile...",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // 📰 POSTS
            items(posts, key = { it._id }) { post ->

                val commentText = commentTexts[post._id] ?: ""

                Column(modifier = Modifier.padding(16.dp)) {

                    // 🔹 HEADER (DP + NAME)
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data("${ApiClient.BASE_URL}/dp/${post.uname}")
                                .addHeader("Cookie", ApiClient.getCookieHeader() ?: "")
                                .allowHardware(false)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,   // 🔑 FIX
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

                    // 🖼 POST IMAGE
                    if (!post.image.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data("${ApiClient.BASE_URL}/pics/${post.image}")
                                .addHeader("Cookie", ApiClient.getCookieHeader() ?: "")
                                .allowHardware(false)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 🕒 DATE
                    Text(
                        formatPostDate(post.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 💬 COMMENTS
                    post.comments.forEach {
                        Text(
                            "${it.commenter}: ${it.commentContent}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ➕ ADD COMMENT
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
                                        postId = post._id,
                                        commenter = currentUserFullName, // ✅ FULL NAME
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