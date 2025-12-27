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
import coil.imageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.background
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun ProfileScreen(
    username: String,                 // profile being viewed
    currentUsername: String,          // logged-in username
    currentUserFirstName: String,     // logged-in first name
    currentUserFullName: String,      // logged-in full name
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onOpenMyProfile: () -> Unit,
    onLogout: () -> Unit,
    onOpenProfile: (String) -> Unit
) {

    val context = LocalContext.current

    var displayName by remember { mutableStateOf("") }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    var dobString by remember { mutableStateOf<String?>(null) }
    var age by remember { mutableStateOf<Int?>(null) }
    var sex by remember { mutableStateOf<String?>(null) }
    var contact by remember { mutableStateOf<String?>(null) }
    var friends by remember { mutableStateOf<List<String>>(emptyList()) }
    var friendsExpanded by remember { mutableStateOf(false) }

    val commentTexts = remember { mutableStateMapOf<String, String>() }
    val scope = rememberCoroutineScope()

    var friendReqRefreshKey by remember { mutableStateOf(0) }

    var friendReqStatus by remember { mutableStateOf<String?>(null) }
    var sendingFriendReq by remember { mutableStateOf(false) }

    var uploadingDp by remember { mutableStateOf(false) }
    var selectedDpUri by remember { mutableStateOf<Uri?>(null) }
    var isPickingDp by remember { mutableStateOf(false) }
    val pickDpLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        isPickingDp = false

        if (uri != null && !uploadingDp) {
            uploadingDp = true

            scope.launch {
                val success = withContext(Dispatchers.IO) {
                    AuthApi.uploadProfileDp(context, uri)
                }

                if (success) {

                    // 🧹 CLEAR COIL CACHE (MEMORY + DISK)
                    context.imageLoader.memoryCache?.clear()
                    context.imageLoader.diskCache?.clear()

                    // 🔥 HARD APP UI RESTART
                    val intent = context.packageManager
                        .getLaunchIntentForPackage(context.packageName)

                    intent?.addFlags(
                        android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    )

                    context.startActivity(intent)
                }

                uploadingDp = false
            }
        }
    }


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
            friends = profile.friends
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

        val refreshState = rememberSwipeRefreshState(isRefreshing = loading)

        SwipeRefresh(
            state = refreshState,
            onRefresh = {
                scope.launch {
                    loadProfile()
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
                                loadProfile()
                                friendReqRefreshKey++
                            }
                        }
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }

                // 👤 PROFILE HEADER (DP + NAME)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, bottom = 12.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {

                        // 🧑 PROFILE DP (LARGE) + CHANGE BUTTON
                        Box(
                            contentAlignment = Alignment.BottomEnd
                        ) {
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

                            // ✅ SHOW ONLY ON MY OWN PROFILE
                            if (username == currentUsername) {
                                IconButton(
                                    enabled = !isPickingDp && !uploadingDp,
                                    onClick = {
                                        if (!isPickingDp && !uploadingDp) {
                                            isPickingDp = true
                                            pickDpLauncher.launch("image/*")
                                        }
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                ) {
                                    if (uploadingDp) {
                                        CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(18.dp),
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Change Photo",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

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

                        // ➕ ADD FRIEND BUTTON (SAFE + NO FLICKER)
                        if (
                            !loading &&
                            username != currentUsername &&
                            !friends.contains(currentUsername)
                        ) {

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                enabled = !sendingFriendReq,
                                onClick = {
                                    sendingFriendReq = true

                                    scope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            AuthApi.sendFriendRequest(username)
                                        }

                                        friendReqStatus = result
                                        sendingFriendReq = false
                                    }
                                }
                            ) {
                                if (sendingFriendReq) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Text("Add Friend")
                                }
                            }

                            // 📩 STATUS MESSAGE (same as web)
                            if (friendReqStatus != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    friendReqStatus!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (!contact.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                contact!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (friends.isNotEmpty()) {

                            Spacer(modifier = Modifier.height(12.dp))

                            // 🔽 FRIENDS HEADER (CLICKABLE)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { friendsExpanded = !friendsExpanded }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {

                                Text(
                                    text = if (friendsExpanded)
                                        "▼ Friends (${friends.size})"
                                    else
                                        "▶ Friends (${friends.size})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // 👥 FRIEND LIST (ONLY WHEN EXPANDED)
                            if (friendsExpanded) {

                                Spacer(modifier = Modifier.height(4.dp))

                                friends.forEach { friendUname ->

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                            .clickable {
                                                onOpenProfile(friendUname)   // ✅ open friend profile
                                            },
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {

                                        // 🧱 FIXED DP COLUMN
                                        Box(
                                            modifier = Modifier.width(36.dp),
                                            contentAlignment = androidx.compose.ui.Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data("${ApiClient.BASE_URL}/dp/$friendUname")
                                                    .addHeader(
                                                        "Cookie",
                                                        ApiClient.getCookieHeader() ?: ""
                                                    )
                                                    .allowHardware(false)
                                                    .build(),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // 🧱 FIXED NAME COLUMN
                                        Box(
                                            modifier = Modifier.width(140.dp),
                                            contentAlignment = androidx.compose.ui.Alignment.CenterStart
                                        ) {
                                            Text(
                                                text = "@$friendUname",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
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