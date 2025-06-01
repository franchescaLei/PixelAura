package com.example.pixelaura.model


//user data class
data class User(
    val username: String = "",
    val email: String = "",
    val handle: String = "",
    val profile_picture: String = "",
    val followers_count: Int = 0,
    val following_count: Int = 0,
    val b_day: String = "",
    val header: String = "",
    val bio: String = "",
    val following: List<String> = emptyList()
)
