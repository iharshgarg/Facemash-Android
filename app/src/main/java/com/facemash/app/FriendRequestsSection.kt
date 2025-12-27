package com.facemash.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FriendRequestsSection(
    onRequestHandled: () -> Unit = {}
) {

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var requests by remember { mutableStateOf<List<String>>(emptyList()) }
    var actionInProgress by remember { mutableStateOf<String?>(null) }

    fun loadRequests() {
        scope.launch {
            requests = withContext(Dispatchers.IO) {
                AuthApi.fetchFriendRequests()
            }
        }
    }

    // 🔥 Load once — no loading flicker on scroll
    LaunchedEffect(Unit) {
        loadRequests()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ✅ NO REQUESTS
        if (requests.isEmpty()) {
            Text(
                text = "No new friend requests",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {

            // 👥 REQUEST LIST
            requests.forEach { requester ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        // 👤 DP + TEXT (CENTERED)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {

                            // 👤 REQUESTER DP (CROPPED, NO EMPTY SPACE)
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data("${ApiClient.BASE_URL}/dp/$requester")
                                    .addHeader(
                                        "Cookie",
                                        ApiClient.getCookieHeader() ?: ""
                                    )
                                    .allowHardware(false)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop, // 🔑 important
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "$requester sent you a friend request",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // ✅ ACTION BUTTONS
                        Row(
                            horizontalArrangement = Arrangement.Center
                        ) {

                            Button(
                                enabled = actionInProgress == null,
                                onClick = {
                                    actionInProgress = requester
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            AuthApi.acceptFriendRequest(requester)
                                        }
                                        actionInProgress = null
                                        loadRequests()
                                        onRequestHandled()
                                    }
                                }
                            ) {
                                Text("Accept")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedButton(
                                enabled = actionInProgress == null,
                                onClick = {
                                    actionInProgress = requester
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            AuthApi.rejectFriendRequest(requester)
                                        }
                                        actionInProgress = null
                                        loadRequests()
                                    }
                                }
                            ) {
                                Text("Decline")
                            }
                        }
                    }
                }
            }
        }
    }
}