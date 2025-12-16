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

@Composable
fun FeedScreen(onLogout: () -> Unit) {

    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var newPost by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    suspend fun loadFeed() {
        loading = true
        posts = withContext(Dispatchers.IO) {
            AuthApi.fetchFeed()
        }
        loading = false
    }

    LaunchedEffect(Unit) {
        loadFeed()
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Facemash", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = onLogout) {
                Text("Logout")
            }
        }

        // Create Post
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = newPost,
                onValueChange = { newPost = it },
                label = { Text("What's on your mind?") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                enabled = newPost.isNotBlank(),
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            AuthApi.createPost(newPost)
                        }
                        newPost = ""
                        loadFeed()
                    }
                }
            ) {
                Text("Post")
            }
        }

        Divider()

        // Feed
        if (loading) {
            Text("Loading feed...", modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(posts) { post ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "${post.fName} ${post.lName}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(post.content)
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                    }
                }
            }
        }
    }
}