package com.facemash.app

data class UserProfile(
    val fullName: String,
    val dob: String?,          // nullable (safe)
    val posts: List<Post>
)