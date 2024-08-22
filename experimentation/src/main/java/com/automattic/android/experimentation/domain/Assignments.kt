package com.automattic.android.experimentation.domain

data class Assignments(
    val variations: Map<String, Variation>,
    val timeToLive: Int,
    val fetchedAt: Long,
)
