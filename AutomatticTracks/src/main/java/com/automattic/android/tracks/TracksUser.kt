package com.automattic.android.tracks

data class TracksUser(
    val userID: String,
    val email: String,
    val username: String,
    val context: Map<String, String>,
)
