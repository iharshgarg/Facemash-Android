package com.facemash.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SuggestionBoxSection(
    currentUsername: String,
    myFriends: List<String>,
    onUserClick: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var users by remember { mutableStateOf<List<UserSearchResult>>(emptyList()) }
    var sendingReq by remember { mutableStateOf<String?>(null) }
    var sentStatus by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    LaunchedEffect(Unit) {
        users = withContext(Dispatchers.IO) {
            AuthApi.fetchSuggestionUsers()
                .reversed()
                .filter { it.uname != currentUsername }
        }
    }

    if (users.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {

        /* ───── TITLE DIVIDER ───── */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(modifier = Modifier.weight(1f))
            Text(
                "New Users",
                modifier = Modifier.padding(horizontal = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Divider(modifier = Modifier.weight(1f))
        }

        /* ───── SUGGESTIONS ───── */
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(users) { user ->

                Column(
                    modifier = Modifier.width(88.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data("${ApiClient.BASE_URL}/dp/${user.uname}")
                            .addHeader("Cookie", ApiClient.getCookieHeader() ?: "")
                            .allowHardware(false)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .clickable { onUserClick(user.uname) }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${user.fName} ${user.lName}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ➕ ADD FRIEND (ONLY IF NOT FRIEND)
                    // ➕ ADD FRIEND (ONLY IF NOT FRIEND)
                    if (!myFriends.contains(user.uname)) {

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            modifier = Modifier.width(96.dp),
                            enabled = sendingReq != user.uname,
                            onClick = {
                                sendingReq = user.uname

                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        AuthApi.sendFriendRequest(user.uname)
                                    }

                                    // ✅ show "Request sent"
                                    sentStatus = sentStatus + (user.uname to true)
                                    sendingReq = null

                                    // ⏳ revert back after 1.2s
                                    kotlinx.coroutines.delay(1200)
                                    sentStatus = sentStatus - user.uname
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            when {
                                sendingReq == user.uname -> {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                sentStatus[user.uname] == true -> {
                                    Text(
                                        text = "Request sent",
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 2,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                else -> {
                                    Text("Add", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }
}