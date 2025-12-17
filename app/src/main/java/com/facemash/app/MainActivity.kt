package com.facemash.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ApiClient.init(applicationContext)

        setContent {

            var showSignup by remember { mutableStateOf(false) }
            var isLoggedIn by remember { mutableStateOf(false) }
            var userInfo by remember { mutableStateOf("") }
            var checkingSession by remember { mutableStateOf(true) }
            var viewingProfile by remember { mutableStateOf<String?>(null) }
            var currentUsername by remember { mutableStateOf("") }
            var currentFullName by remember { mutableStateOf("") }

            val scope = rememberCoroutineScope()

            // Auto-login check on app start
            LaunchedEffect(Unit) {
                val sessionResult = withContext(Dispatchers.IO) {
                    AuthApi.checkSession()
                }

                if (sessionResult.contains("uname")) {
                    isLoggedIn = true
                    userInfo = sessionResult

                    val fNameRegex = """"fName"\s*:\s*"([^"]+)"""".toRegex()
                    val lNameRegex = """"lName"\s*:\s*"([^"]+)"""".toRegex()

                    val fName = fNameRegex.find(sessionResult)?.groupValues?.get(1) ?: ""
                    val lName = lNameRegex.find(sessionResult)?.groupValues?.get(1) ?: ""

                    currentFullName = "$fName $lName"

                    val unameRegex = """"uname"\s*:\s*"([^"]+)"""".toRegex()
                    currentUsername = unameRegex.find(sessionResult)?.groupValues?.get(1) ?: ""
                }
                checkingSession = false
            }

            when {
                checkingSession -> {
                    Text("Checking session...")
                }
                isLoggedIn -> {
                    if (viewingProfile != null) {
                        ProfileScreen(
                            username = viewingProfile!!,
                            currentUserName = currentFullName,
                            onBack = { viewingProfile = null }
                        )
                    } else {
                        FeedScreen(
                            currentUserName = currentFullName,
                            onLogout = {
                                ApiClient.clearCookies()
                                isLoggedIn = false
                                userInfo = ""
                            },
                            onOpenProfile = {
                                viewingProfile = currentUsername
                            }
                        )
                    }
                }
                else -> {
                    if (showSignup) {
                        SignupScreen {
                            showSignup = false
                        }
                    } else {
                        LoginScreen(
                            onLoginSuccess = { sessionData ->
                                isLoggedIn = true
                                userInfo = sessionData

                                // extract username safely
                                val unameRegex = """"uname"\s*:\s*"([^"]+)"""".toRegex()
                                currentUsername = unameRegex.find(sessionData)?.groupValues?.get(1) ?: ""

                                val fNameRegex = """"fName"\s*:\s*"([^"]+)"""".toRegex()
                                val lNameRegex = """"lName"\s*:\s*"([^"]+)"""".toRegex()

                                val fName = fNameRegex.find(sessionData)?.groupValues?.get(1) ?: ""
                                val lName = lNameRegex.find(sessionData)?.groupValues?.get(1) ?: ""

                                currentFullName = "$fName $lName"
                            },
                            onSignup = {
                                showSignup = true
                            }
                        )
                    }
                }
            }
        }
    }
}