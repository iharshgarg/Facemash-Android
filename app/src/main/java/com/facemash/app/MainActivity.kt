package com.facemash.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ApiClient.init(applicationContext)

        setContent {

            var appStatus by remember { mutableStateOf<AppStatus>(AppStatus.Loading) }

            var showSignup by remember { mutableStateOf(false) }
            var isLoggedIn by remember { mutableStateOf(false) }
            var viewingProfile by remember { mutableStateOf<String?>(null) }
            var showSearch by remember { mutableStateOf(false) }

            var currentUsername by remember { mutableStateOf("") }
            var currentFullName by remember { mutableStateOf("") }
            var currentUserFirstName by remember { mutableStateOf("") }

            // 🔍 SAFE NETWORK + SERVER CHECK
            LaunchedEffect(Unit) {

                val hasInternet = NetworkChecker.hasInternet()
                if (!hasInternet) {
                    appStatus = AppStatus.NoInternet
                    return@LaunchedEffect
                }

                val serverUp = NetworkChecker.isServerUp()
                if (!serverUp) {
                    appStatus = AppStatus.ServerDown
                    return@LaunchedEffect
                }

                val sessionResult = withContext(Dispatchers.IO) {
                    AuthApi.checkSession()
                }

                if (sessionResult.contains("uname")) {
                    isLoggedIn = true

                    val fName =
                        """"fName"\s*:\s*"([^"]+)"""".toRegex()
                            .find(sessionResult)?.groupValues?.get(1) ?: ""

                    val lName =
                        """"lName"\s*:\s*"([^"]+)"""".toRegex()
                            .find(sessionResult)?.groupValues?.get(1) ?: ""

                    currentUserFirstName = fName
                    currentFullName = "$fName $lName"

                    currentUsername =
                        """"uname"\s*:\s*"([^"]+)"""".toRegex()
                            .find(sessionResult)?.groupValues?.get(1) ?: ""
                }

                appStatus = AppStatus.Online
            }

            // 🧠 UI STATE MACHINE
            when (appStatus) {

                AppStatus.Loading -> {
                    CenterText("Checking connectivity…")
                }

                AppStatus.NoInternet -> {
                    CenterText("You are not connected to internet!")
                }

                AppStatus.ServerDown -> {
                    CenterText("Facemash server is booting up, please wait…")
                }

                AppStatus.Online -> {

                    if (!isLoggedIn) {

                        if (showSignup) {
                            SignupScreen { showSignup = false }
                        } else {
                            LoginScreen(
                                onLoginSuccess = { sessionData ->
                                    isLoggedIn = true

                                    val fName =
                                        """"fName"\s*:\s*"([^"]+)"""".toRegex()
                                            .find(sessionData)?.groupValues?.get(1) ?: ""

                                    val lName =
                                        """"lName"\s*:\s*"([^"]+)"""".toRegex()
                                            .find(sessionData)?.groupValues?.get(1) ?: ""

                                    currentUserFirstName = fName
                                    currentFullName = "$fName $lName"

                                    currentUsername =
                                        """"uname"\s*:\s*"([^"]+)"""".toRegex()
                                            .find(sessionData)?.groupValues?.get(1) ?: ""
                                },
                                onSignup = { showSignup = true }
                            )
                        }

                    } else {

                        when {
                            showSearch -> {
                                SearchScreen(
                                    onUserClick = {
                                        showSearch = false
                                        viewingProfile = it
                                    },
                                    onBack = { showSearch = false }
                                )
                            }

                            viewingProfile != null -> {
                                ProfileScreen(
                                    username = viewingProfile!!,
                                    currentUserName = currentFullName,
                                    currentUserFirstName = currentUserFirstName,
                                    onBack = { viewingProfile = null },
                                    onSearch = { showSearch = true },
                                    onOpenMyProfile = {
                                        viewingProfile = currentUsername
                                        showSearch = false
                                    }
                                )
                            }

                            else -> {
                                FeedScreen(
                                    currentUserName = currentFullName,
                                    currentUserFirstName = currentUserFirstName,
                                    onLogout = {
                                        ApiClient.clearCookies()
                                        isLoggedIn = false
                                    },
                                    onOpenProfile = {
                                        viewingProfile = currentUsername
                                    },
                                    onOpenSearch = {
                                        showSearch = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CenterText(text: String) {
    Surface {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(text)
        }
    }
}