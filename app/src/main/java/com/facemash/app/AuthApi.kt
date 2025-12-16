package com.facemash.app

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray

object AuthApi {

    fun signup(
        fName: String,
        lName: String,
        uname: String,
        contact: String,
        pass: String,
        dob: String,
        sex: String
    ): String {

        val json = JSONObject().apply {
            put("fName", fName)
            put("lName", lName)
            put("uname", uname)
            put("contact", contact)
            put("pass", pass)
            put("dob", dob)
            put("sex", sex)
        }

        val body = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = okhttp3.Request.Builder()
            .url("${ApiClient.BASE_URL}/signup")
            .post(body)
            .build()

        ApiClient.client.newCall(request).execute().use { response ->
            return response.body?.string() ?: "Signup failed"
        }
    }
    fun login(uname: String, pass: String): String {
        val json = JSONObject()
        json.put("uname", uname)
        json.put("pass", pass)

        val body = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("${ApiClient.BASE_URL}/login")
            .post(body)
            .build()

        ApiClient.client.newCall(request).execute().use { response ->
            return response.body?.string() ?: "No response"
        }
    }

    fun checkSession(): String {
        val request = Request.Builder()
            .url("${ApiClient.BASE_URL}/session")
            .get()
            .build()

        ApiClient.client.newCall(request).execute().use { response ->
            return response.body?.string() ?: "No session"
        }
    }

    fun logout(): String {
        val request = Request.Builder()
            .url("${ApiClient.BASE_URL}/logout")
            .post(okhttp3.RequestBody.create(null, ByteArray(0)))
            .build()

        ApiClient.client.newCall(request).execute().use { response ->
            return response.body?.string() ?: "Logged out"
        }
    }


    fun fetchFeed(): List<Post> {
        val request = Request.Builder()
            .url("${ApiClient.BASE_URL}/feed")
            .get()
            .build()

        ApiClient.client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: return emptyList()
            val jsonArray = JSONArray(body)

            val posts = mutableListOf<Post>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                posts.add(
                    Post(
                        fName = obj.getString("fName"),
                        lName = obj.getString("lName"),
                        content = obj.getString("content")
                    )
                )
            }
            return posts
        }
    }

    fun createPost(content: String): String {
        val body = content
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("${ApiClient.BASE_URL}/createPost")
            .post(
                okhttp3.MultipartBody.Builder()
                    .setType(okhttp3.MultipartBody.FORM)
                    .addFormDataPart("content", content)
                    .build()
            )
            .build()

        ApiClient.client.newCall(request).execute().use { response ->
            return response.body?.string() ?: "Post failed"
        }
    }
}