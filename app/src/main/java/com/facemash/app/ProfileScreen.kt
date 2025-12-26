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
    username: String,                 // profile being viewed
    currentUsername: String,          // logged-in username
    currentUserFirstName: String,     // logged-in first name
    currentUserFullName: String,      // logged-in full name
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onOpenMyProfile: () -> Unit,
    onLogout: () -> Unit
) {

    val context = LocalContext.current

    var displayName by remember { mutableStateOf("") }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    var dobString by remember { mutableStateOf<String?>(null) }
    var age by remember { mutableStateOf<Int?>(null) }
    var sex by remember { mutableStateOf<String?>(null) }
    var contact by remember { mutableStateOf<String?>(null) }

    val commentTexts = remember { mutableStateMapOf<String, String>() }
    val scope = rememberCoroutineScope()

    suspend fun loadProfile() {
        loading = true
        try {
            val profile = withContext(Dispatchers.IO) {
                AuthApi.fetchProfile(username)
            }

            displayName = profile.fullName
            dobString = profile.dob
            sex = profile.sex
            contact = profile.contact
            posts = profile.posts

            age = calculateAgeSafe(dobString)

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
            onProfile = onOpenMyProfile,
            onSearch = onSearch,
            onLogout = onLogout
        )

        // 🔽 EVERYTHING BELOW SCROLLS
        LazyColumn(modifier = Modifier.fillMaxSize()) {

            // 👤 PROFILE HEADER (DP + NAME)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 12.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {

                    // 🧑 PROFILE DP (LARGE)
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data("${ApiClient.BASE_URL}/dp/$username")
                            .addHeader("Cookie", ApiClient.getCookieHeader() ?: "")
                            .allowHardware(false)
                            .build(),
                        contentDescription = "Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val pronoun = when (sex) {
                        "Male" -> "he"
                        "Female" -> "she"
                        else -> null
                    }

                    val metaText = when {
                        age != null && pronoun != null -> "$age, $pronoun"
                        age != null -> "$age"
                        pronoun != null -> pronoun
                        else -> null
                    }

                    Text(
                        if (metaText != null) "$displayName ($metaText)" else displayName,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        "@$username",
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (!contact.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            contact!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Divider(modifier = Modifier.padding(top = 16.dp))
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

                    // 🔹 POST HEADER (DP + NAME)
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data("${ApiClient.BASE_URL}/dp/${post.uname}")
                                .addHeader("Cookie", ApiClient.getCookieHeader() ?: "")
                                .allowHardware(false)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
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
                                        commenter = currentUserFullName,
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

private fun calculateAgeSafe(dob: String?): Int? {
    if (dob.isNullOrBlank()) return null

    return try {
        val year = dob.substring(0, 4).toInt()
        val month = dob.substring(5, 7).toInt() - 1
        val day = dob.substring(8, 10).toInt()

        val dobCal = java.util.Calendar.getInstance()
        dobCal.set(year, month, day)

        val today = java.util.Calendar.getInstance()

        var age = today.get(java.util.Calendar.YEAR) -
                dobCal.get(java.util.Calendar.YEAR)

        if (
            today.get(java.util.Calendar.DAY_OF_YEAR) <
            dobCal.get(java.util.Calendar.DAY_OF_YEAR)
        ) {
            age--
        }

        if (age < 0) null else age
    } catch (e: Exception) {
        null
    }
}