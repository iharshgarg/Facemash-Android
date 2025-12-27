package com.facemash.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    var loading by remember { mutableStateOf(true) }
    var actionInProgress by remember { mutableStateOf<String?>(null) }

    fun loadRequests() {
        scope.launch {
            loading = true
            requests = withContext(Dispatchers.IO) {
                AuthApi.fetchFriendRequests()
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        loadRequests()
    }

    Column(modifier = Modifier.fillMaxWidth()) {

        Text(
            "Friend Requests",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        when {
            loading -> {
                Text("Loading requests…", style = MaterialTheme.typography.bodySmall)
            }

            requests.isEmpty() -> {
                Text(
                    "No new friend requests",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> {
                requests.forEach { requester ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {

                            Text(
                                "$requester sent you a friend request",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row {
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
}