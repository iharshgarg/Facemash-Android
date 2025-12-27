package com.facemash.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun FeedScreen(
    currentUsername: String,          // ✅ uname (lion)
    currentUserFirstName: String,     // ✅ John
    currentUserFullName: String,      // ✅ John Leawes
    onLogout: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSearch: () -> Unit
) {

    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var newPost by remember { mutableStateOf("") }

    val commentTexts = remember { mutableStateMapOf<String, String>() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var friendReqRefreshKey by remember { mutableStateOf(0) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }

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

    LaunchedEffect(Unit) { loadFeed() }

    Column(modifier = Modifier.fillMaxSize()) {

        // 🔒 FIXED TOP BAR
        TopBar(
            currentUsername = currentUsername,
            currentUserFirstName = currentUserFirstName,
            onHome = { scope.launch { loadFeed() } },
            onProfile = onOpenProfile,
            onSearch = onOpenSearch,
            onLogout = onLogout
        )

        val refreshState = rememberSwipeRefreshState(isRefreshing = loading)

        SwipeRefresh(
            state = refreshState,
            onRefresh = {
                scope.launch {
                    loadFeed()
                    friendReqRefreshKey++
                }
            }
        ) { // 🔽 EVERYTHING BELOW SCROLLS
            LazyColumn(modifier = Modifier.fillMaxSize()) {

                // 🔔 FRIEND REQUESTS (TOP, BELOW TOPBAR)
                item {
                    FriendRequestsSection(
                        refreshKey = friendReqRefreshKey,
                        onRequestHandled = {
                            scope.launch {
                                loadFeed()
                                friendReqRefreshKey++
                            }
                        }
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }

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
                                Text(
                                    if (selectedImageUri == null)
                                        "Upload Photo"
                                    else
                                        "Photo Selected"
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

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

                        // 🖼 IMAGE
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