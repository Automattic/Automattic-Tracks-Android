package com.automattic.android.experimentation.domain

internal fun interface Clock {
    fun currentTimeSeconds(): Long
}

internal class SystemClock : Clock {
    override fun currentTimeSeconds(): Long = System.currentTimeMillis() / 1000
}
