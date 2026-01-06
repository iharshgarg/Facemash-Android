package com.facemash.app

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(onBackToLogin: () -> Unit) {

    BackHandler { onBackToLogin() }

    var fName by remember { mutableStateOf("") }
    var lName by remember { mutableStateOf("") }
    var uname by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }

    var dobDisplay by remember { mutableStateOf("") }
    var dobBackend by remember { mutableStateOf("") }
    var dobMillis by remember { mutableStateOf<Long?>(null) }

    var sex by remember { mutableStateOf("Male") }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    /* ───── DATE PICKER STATE ───── */
    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= System.currentTimeMillis()
            }
        }
    )

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        dobMillis = millis
                        val date = Date(millis)

                        dobDisplay = SimpleDateFormat(
                            "dd-MM-yyyy",
                            Locale("en", "IN")
                        ).format(date)

                        dobBackend = SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.US
                        ).format(date)
                    }
                    showDatePicker = false
                }) { Text("OK") }
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

                val age = dobMillis?.let { calculateAge(it) } ?: 0

                when {
                    fName.isBlank() || lName.isBlank() || uname.isBlank()
                            || contact.isBlank() || pass.isBlank()
                            || dobBackend.isBlank() -> {
                        message = "Please fill all fields"
                        return@Button
                    }

                    age < 13 -> {
                        message = "You must be at least 13 years old to use Facemash"
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
                            fName.trim(),
                            lName.trim(),
                            uname.trim(),
                            contact.trim(),
                            pass,
                            dobBackend,
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

        // 🔐 Privacy Policy acknowledgement
        TextButton(
            onClick = {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.facemash.in/privacy.html")
                    )
                )
            }
        ) {
            Text(
                text = "By continuing, you agree to our Privacy Policy",
                style = MaterialTheme.typography.bodySmall
            )
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

/* ───── AGE CALCULATION ───── */
private fun calculateAge(dobMillis: Long): Int {
    val dob = Calendar.getInstance().apply { timeInMillis = dobMillis }
    val today = Calendar.getInstance()

    var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
    if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
        age--
    }
    return age
}