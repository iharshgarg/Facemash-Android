package com.facemash.app

data class ProfileData(
    val fullName: String,
    val dob: String?,
    val sex: String?,
    val posts: List<Post>
)