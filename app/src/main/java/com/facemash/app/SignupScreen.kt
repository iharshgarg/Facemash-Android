package com.facemash.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(onBackToLogin: () -> Unit) {

    BackHandler {
        onBackToLogin()
    }

    var fName by remember { mutableStateOf("") }
    var lName by remember { mutableStateOf("") }
    var uname by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }

    var dobDisplay by remember { mutableStateOf("") }   // DD-MM-YYYY (UI)
    var dobBackend by remember { mutableStateOf("") }   // YYYY-MM-DD (API)

    var sex by remember { mutableStateOf("Male") }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    /* ───── DATE PICKER STATE ───── */
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val date = Date(millis)

                            // 🇮🇳 display format
                            dobDisplay = SimpleDateFormat(
                                "dd-MM-yyyy",
                                Locale("en", "IN")
                            ).format(date)

                            // 🔁 backend-safe format
                            dobBackend = SimpleDateFormat(
                                "yyyy-MM-dd",
                                Locale.US
                            ).format(date)
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text("Create Account", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(12.dp))

        /* ───── NAME ROW ───── */
        Row {
            OutlinedTextField(
                value = fName,
                onValueChange = { fName = it },
                label = { Text("First name") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = lName,
                onValueChange = { lName = it },
                label = { Text("Last name") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uname,
            onValueChange = { uname = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = contact,
            onValueChange = { contact = it },
            label = { Text("Email or Phone") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        /* ───── PASSWORD ROW ───── */
        Row {
            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("Password") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = confirmPass,
                onValueChange = { confirmPass = it },
                label = { Text("Confirm") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        /* ───── DOB PICKER ───── */
        OutlinedTextField(
            value = dobDisplay,
            onValueChange = {},
            label = { Text("Date of Birth (DD-MM-YYYY)") },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Pick date"
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        /* ───── SEX ROW ───── */
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = sex == "Male",
                onClick = { sex = "Male" }
            )
            Text("Male")

            Spacer(modifier = Modifier.width(16.dp))

            RadioButton(
                selected = sex == "Female",
                onClick = { sex = "Female" }
            )
            Text("Female")
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
            onClick = {

                val f = fName.trim()
                val l = lName.trim()
                val u = uname.trim()
                val c = contact.trim()

                when {
                    f.isEmpty() || l.isEmpty() || u.isEmpty()
                            || c.isEmpty() || pass.isEmpty()
                            || dobBackend.isEmpty() -> {
                        message = "Please fill all fields"
                        return@Button
                    }

                    pass != confirmPass -> {
                        message = "Passwords do not match"
                        confirmPass = ""
                        return@Button
                    }
                }

                loading = true
                message = "Creating account..."

                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        AuthApi.signup(
                            f, l, u, c,
                            pass,
                            dobBackend, // ✅ YYYY-MM-DD
                            sex
                        )
                    }
                    message = result
                    loading = false
                }
            }
        ) {
            Text(if (loading) "Please wait..." else "Sign Up")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(message)

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onBackToLogin) {
            Text("Already have an account? Login")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}