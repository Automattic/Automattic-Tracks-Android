 package com.automattic.android.experimentation.domain

 import java.util.Date

 internal class AssignmentsValidator {

    private val Assignments.expiresAt
        get() = fetchedAt + timeToLive

    fun Assignments.isStale(clock: Clock) = clock.currentTimeSeconds() > expiresAt
 }
