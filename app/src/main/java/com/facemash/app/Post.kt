package com.facemash.app

data class Comment(
    val commenter: String,
    val commentContent: String
)

data class Post(
    val _id: String,
    val uname: String,
    val fName: String,
    val lName: String,
    val content: String,
    val image: String?,        // 👈 image URL  (nullable)
    val createdAt: String,
    val comments: List<Comment>
)