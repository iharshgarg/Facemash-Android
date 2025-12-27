package com.facemash.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FriendRequestsSection(
    onRequestHandled: () -> Unit = {}
) {

    val scope = rememberCoroutineScope()

    var requests by remember { mutableStateOf<List<String>>(emptyList()) }
    var actionInProgress by remember { mutableStateOf<String?>(null) }

    fun loadRequests() {
        scope.launch {
            requests = withContext(Dispatchers.IO) {
                AuthApi.fetchFriendRequests()
            }
        }
    }

    // 🔥 Load once, no loading flicker
    LaunchedEffect(Unit) {
        loadRequests()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ✅ NO REQUESTS → SIMPLE, CALM UI
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

                        Text(
                            text = "$requester sent you a friend request",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

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