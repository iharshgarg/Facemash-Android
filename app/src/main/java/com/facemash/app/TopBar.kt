package com.facemash.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale

// 🎨 OG FACEBOOK COLORS
private val FacebookBlue = Color(0xFF3B5998)
private val FacebookBlueLight = Color(0xFF4E71BA)

@Composable
fun TopBar(
    currentUsername: String,
    currentUserFirstName: String,
    onHome: () -> Unit,
    onProfile: () -> Unit,
    onSearch: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        color = FacebookBlue,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // 🔵 FACEMASH LOGO
            TextButton(
                onClick = onHome,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    "facemash",
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 🔍 SEARCH BAR
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(FacebookBlueLight)
                    .clickable { onSearch() }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Search users",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // 👤 PROFILE
            TextButton(
                onClick = onProfile,
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data("${ApiClient.BASE_URL}/dp/$currentUsername")
                        .addHeader("Cookie", ApiClient.getCookieHeader() ?: "")
                        .allowHardware(false)
                        .build(),
                    contentDescription = "DP",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    currentUserFirstName,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // 🏠 HOME
            TextButton(
                onClick = onHome,
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text(
                    "Home",
                    color = Color.White,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // 🚪 LOGOUT
            TextButton(
                onClick = onLogout,
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text(
                    "Logout",
                    color = Color.White,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}