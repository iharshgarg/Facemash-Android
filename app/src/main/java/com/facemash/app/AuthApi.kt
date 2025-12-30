package com.facemash.app

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray
import android.content.Context
import android.net.Uri
import okhttp3.MultipartBody
import java.io.File
import okhttp3.RequestBody.Companion.asRequestBody

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

        val request = okhttp3.Request.Builder()
            .url("${ApiClient.BASE_URL}/feed")
            .get()
            .build()

        ApiClient.client.newCall(request).execute().use { response ->

            if (!response.isSuccessful) return emptyList()

            val body = response.body?.string() ?: return emptyList()
            val jsonArray = org.json.JSONArray(body)

            val posts = mutableListOf<Post>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

                val commentsArray = obj.getJSONArray("comments")
                val comments = mutableListOf<Comment>()

                for (j in 0 until commentsArray.length()) {
                    val c = commentsArray.getJSONObject(j)
                    comments.add(
                        Comment(
                            commenter = c.getString("commenter"),
                            commentContent = c.getString("commentContent")
                        )
                    )
                }

                val imageValue = if (obj.has("image") && !obj.isNull("image")) {
                    obj.getString("image")
                } else {
                    null
                }

                posts.add(
                    Post(
                        _id = obj.getString("_id"),
                        uname = obj.getString("uname"),
                        fName = obj.getString("fName"),
                        lName = obj.getString("lName"),
                        content = obj.getString("content"),
                        image = imageValue,
                        createdAt = obj.getString("createdAt"),
                        comments = comments
                    )
                )
            }
            return posts
        }
    }

    fun addComment(
        postId: String,
        commenter: String,
        commentContent: String
    ): Boolean {

        val json = org.json.JSONObject().apply {
            put("_id", postId)
            put("commenter", commenter)
            put("commentContent", commentContent)
        }

        val body = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = okhttp3.Request.Builder()
            .url("${ApiClient.BASE_URL}/post-comment")
            .post(body)
            .build()

        ApiClient.client.newCall(request).execute().use {
            return it.isSuccessful
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

    fun createPostWithImage(
        context: Context,
        content: String,
        imageUri: Uri
    ): Boolean {

        val inputStream = context.contentResolver.openInputStream(imageUri) ?: return false
        val tempFile = File.createTempFile("upload", ".jpg", context.cacheDir)

        tempFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("content", content)
            .addFormDataPart(
                "pic",
                tempFile.name,
                tempFile.asRequestBody("image/*".toMediaType())
            )
            .build()

        val request = okhttp3.Request.Builder()
            .url("${ApiClient.BASE_URL}/createPost")
            .post(requestBody)
            .build()

        ApiClient.client.newCall(request).execute().use {
            return it.isSuccessful
        }
    }

    fun fetchProfile(username: String): UserProfile {

        val request = Request.Builder()
            .url("${ApiClient.BASE_URL}/users/$username")
            .get()
            .build()

        ApiClient.client.newCall(request).execute().use { response ->

            if (!response.isSuccessful) {
                return UserProfile("", null, null, null, emptyList(), emptyList())
            }

            val body = response.body?.string()
                ?: return UserProfile("", null, null, null, emptyList(), emptyList())

            val obj = JSONObject(body)

            val fullName = obj.getString("fName") + " " + obj.getString("lName")

            val dob = if (obj.has("dob") && !obj.isNull("dob")) {
                obj.getString("dob")
            } else null

            val sex = if (obj.has("sex") && !obj.isNull("sex")) {
                obj.getString("sex")
            } else null

            val contact = if (obj.has("contact") && !obj.isNull("contact")) {
                obj.getString("contact")
            } else null

            // ✅ FRIENDS LIST
            val friends = if (obj.has("friends") && !obj.isNull("friends")) {
                val arr = obj.getJSONArray("friends")
                List(arr.length()) { i -> arr.getString(i) }
            } else emptyList()

            val postsArray = obj.getJSONArray("posts")
            val posts = mutableListOf<Post>()

            for (i in 0 until postsArray.length()) {
                val p = postsArray.getJSONObject(i)

                val commentsArray = p.getJSONArray("comments")
                val comments = mutableListOf<Comment>()

                for (j in 0 until commentsArray.length()) {
                    val c = commentsArray.getJSONObject(j)
                    comments.add(
                        Comment(
                            commenter = c.getString("commenter"),
                            commentContent = c.getString("commentContent")
                        )
                    )
                }

                val imageValue =
                    if (p.has("image") && !p.isNull("image") && p.getString("image").isNotBlank())
                        p.getString("image")
                    else null

                posts.add(
                    Post(
                        _id = p.getString("_id"),
                        uname = p.getString("uname"),
                        fName = p.getString("fName"),
                        lName = p.getString("lName"),
                        content = p.getString("content"),
                        image = imageValue,
                        createdAt = p.getString("createdAt"),
                        comments = comments
                    )
                )
            }

            return UserProfile(
                fullName = fullName,
                dob = dob,
                sex = sex,
                contact = contact,
                friends = friends,
                posts = posts
            )
        }
    }

    fun uploadProfileDp(
        context: Context,
        imageUri: Uri
    ): Boolean {
        val inputStream = context.contentResolver.openInputStream(imageUri)
            ?: return false

        val tempFile = File.createTempFile("dp_upload", ".jpg", context.cacheDir)

        tempFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "dp",
                tempFile.name,
                tempFile.asRequestBody("image/*".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url("${ApiClient.BASE_URL}/upload-dp")
            .post(requestBody)
            .addHeader("Cookie", ApiClient.getCookieHeader() ?: "")
            .build()

        ApiClient.client.newCall(request).execute().use {
            return it.isSuccessful
        }
    }

    suspend fun searchUsers(query: String): List<UserSearchResult> {
        if (query.isBlank()) return emptyList()

        val json = JSONObject()
        json.put("query", query)

        val requestBody = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = okhttp3.Request.Builder()
            .url(ApiClient.BASE_URL + "/search")
            .post(requestBody)
            .addHeader("Cookie", ApiClient.getCookieHeader() ?: "")
            .build()

        val response = ApiClient.client.newCall(request).execute()
        val body = response.body?.string() ?: return emptyList()

        val jsonArray = JSONArray(body)
        val results = mutableListOf<UserSearchResult>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            results.add(
                UserSearchResult(
                    uname = obj.getString("uname"),
                    fName = obj.getString("fName"),
                    lName = obj.getString("lName")
                )
            )
        }

        return results
    }

    fun sendFriendRequest(targetUsername: String): String {

        val json = JSONObject().apply {
            put("targetUsername", targetUsername)
        }

        val body = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("${ApiClient.BASE_URL}/send-friend-req")
            .post(body)
            .addHeader("Cookie", ApiClient.getCookieHeader() ?: "")
            .build()

        ApiClient.client.newCall(request).execute().use { response ->
            return response.body?.string() ?: "Failed to send request"
        }
    }

    fun fetchFriendRequests(): List<String> {

        val request = Request.Builder()
            .url("${ApiClient.BASE_URL}/notifs")
            .get()
            .addHeader("Cookie", ApiClient.getCookieHeader() ?: "")
            .build()

        ApiClient.client.newCall(request).execute().use { response ->

            if (!response.isSuccessful) return emptyList()

            val body = response.body?.string() ?: return emptyList()
            val obj = JSONObject(body)

            val arr = obj.getJSONArray("friendRequests")
            return List(arr.length()) { i -> arr.getString(i) }
        }
    }

    fun acceptFriendRequest(requesterUsername: String): String {

        val json = JSONObject().apply {
            put("requesterUsername", requesterUsername)
        }

        val body = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("${ApiClient.BASE_URL}/accept-friend-req")
            .post(body)
            .addHeader("Cookie", ApiClient.getCookieHeader() ?: "")
            .build()

        ApiClient.client.newCall(request).execute().use {
            return it.body?.string() ?: "Failed to accept request"
        }
    }

    fun rejectFriendRequest(requesterUsername: String): String {

        val json = JSONObject().apply {
            put("requesterUsername", requesterUsername)
        }

        val body = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("${ApiClient.BASE_URL}/reject-friend-req")
            .post(body)
            .addHeader("Cookie", ApiClient.getCookieHeader() ?: "")
            .build()

        ApiClient.client.newCall(request).execute().use {
            return it.body?.string() ?: "Failed to reject request"
        }
    }

    fun fetchSuggestionUsers(): List<UserSearchResult> {

        val request = Request.Builder()
            .url("${ApiClient.BASE_URL}/suggestion-box")
            .get()
            .addHeader("Cookie", ApiClient.getCookieHeader() ?: "")
            .build()

        ApiClient.client.newCall(request).execute().use { response ->

            if (!response.isSuccessful) return emptyList()

            val body = response.body?.string() ?: return emptyList()
            val jsonArray = JSONArray(body)

            val users = mutableListOf<UserSearchResult>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                users.add(
                    UserSearchResult(
                        uname = obj.getString("uname"),
                        fName = obj.getString("fName"),
                        lName = obj.getString("lName")
                    )
                )
            }

            return users
        }
    }

    fun fetchConversation(friendUsername: String): List<ChatMessage> {

        val request = okhttp3.Request.Builder()
            .url("${ApiClient.BASE_URL}/conversation/$friendUsername")
            .get()
            .addHeader("Cookie", ApiClient.getCookieHeader() ?: "")
            .build()

        ApiClient.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()

            val body = response.body?.string() ?: return emptyList()
            val json = org.json.JSONObject(body)
            val messagesArray = json.getJSONArray("messages")

            val messages = mutableListOf<ChatMessage>()

            for (i in 0 until messagesArray.length()) {
                val obj = messagesArray.getJSONObject(i)
                messages.add(
                    ChatMessage(
                        sender = obj.getString("sender"),
                        content = obj.getString("content"),
                        timestamp = obj.getString("timestamp")
                    )
                )
            }
            return messages
        }
    }
}