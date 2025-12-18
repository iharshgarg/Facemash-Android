package com.facemash.app

import java.text.SimpleDateFormat
import java.util.*

fun formatPostDate(isoDate: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")

        val date = parser.parse(isoDate) ?: return ""

        val formatter = SimpleDateFormat("dd MMM yyyy 'at' h:mm a", Locale.getDefault())
        formatter.format(date)
    } catch (e: Exception) {
        ""
    }
}