package com.facemash.app

data class UserProfile(
    val fullName: String,
    val dob: String?,          // nullable
    val sex: String?,          // "Male" / "Female"
    val posts: List<Post>
)