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

            /* -------------------- GLOBAL APP STATE -------------------- */

            var appStatus by remember { mutableStateOf<AppStatus>(AppStatus.Loading) }

            var isLoggedIn by remember { mutableStateOf(false) }
            var showSignup by remember { mutableStateOf(false) }
            var showSearch by remember { mutableStateOf(false) }
            var viewingProfile by remember { mutableStateOf<String?>(null) }

            var currentUsername by remember { mutableStateOf("") }
            var currentUserFirstName by remember { mutableStateOf("") }
            var currentUserFullName by remember { mutableStateOf("") }

            /* -------------------- STARTUP CHECK -------------------- */

            LaunchedEffect(Unit) {
                try {
                    // 1️⃣ Internet check (IO THREAD)
                    val hasInternet = withContext(Dispatchers.IO) {
                        NetworkChecker.hasInternet()
                    }
                    if (!hasInternet) {
                        appStatus = AppStatus.NoInternet
                        return@LaunchedEffect
                    }

                    // 2️⃣ Server heartbeat (IO THREAD)
                    val serverUp = withContext(Dispatchers.IO) {
                        NetworkChecker.isServerUp()
                    }
                    if (!serverUp) {
                        appStatus = AppStatus.ServerDown
                        return@LaunchedEffect
                    }

                    // 3️⃣ Session check (IO THREAD)
                    val sessionResult = withContext(Dispatchers.IO) {
                        AuthApi.checkSession()
                    }

                    if (sessionResult.contains("uname")) {
                        isLoggedIn = true

                        currentUsername =
                            """"uname"\s*:\s*"([^"]+)"""".toRegex()
                                .find(sessionResult)?.groupValues?.get(1) ?: ""

                        val fName =
                            """"fName"\s*:\s*"([^"]+)"""".toRegex()
                                .find(sessionResult)?.groupValues?.get(1) ?: ""

                        val lName =
                            """"lName"\s*:\s*"([^"]+)"""".toRegex()
                                .find(sessionResult)?.groupValues?.get(1) ?: ""

                        currentUserFirstName = fName
                        currentUserFullName = "$fName $lName"
                    }

                    appStatus = AppStatus.Online

                } catch (e: Exception) {
                    appStatus = AppStatus.ServerDown
                }
            }

            /* -------------------- UI STATE MACHINE -------------------- */

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
                            SignupScreen {
                                showSignup = false
                            }
                        } else {
                            LoginScreen(
                                onLoginSuccess = { sessionData ->
                                    isLoggedIn = true

                                    currentUsername =
                                        """"uname"\s*:\s*"([^"]+)"""".toRegex()
                                            .find(sessionData)?.groupValues?.get(1) ?: ""

                                    val fName =
                                        """"fName"\s*:\s*"([^"]+)"""".toRegex()
                                            .find(sessionData)?.groupValues?.get(1) ?: ""

                                    val lName =
                                        """"lName"\s*:\s*"([^"]+)"""".toRegex()
                                            .find(sessionData)?.groupValues?.get(1) ?: ""

                                    currentUserFirstName = fName
                                    currentUserFullName = "$fName $lName"
                                },
                                onSignup = { showSignup = true }
                            )
                        }

                    } else {

                        when {
                            showSearch -> {
                                SearchScreen(
                                    onUserClick = { uname ->
                                        showSearch = false
                                        viewingProfile = uname
                                    },
                                    onBack = { showSearch = false }
                                )
                            }

                            viewingProfile != null -> {
                                ProfileScreen(
                                    username = viewingProfile!!,
                                    currentUsername = currentUsername,
                                    currentUserFirstName = currentUserFirstName,
                                    currentUserFullName = currentUserFullName,
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
                                    currentUsername = currentUsername,
                                    currentUserFirstName = currentUserFirstName,
                                    currentUserFullName = currentUserFullName,
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

/* -------------------- HELPERS -------------------- */

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