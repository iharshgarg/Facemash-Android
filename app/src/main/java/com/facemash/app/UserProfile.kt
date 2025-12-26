package com.facemash.app

data class UserProfile(
    val fullName: String,
    val dob: String?,
    val sex: String?,
    val contact: String?,     // 👈 NEW
    val posts: List<Post>
)