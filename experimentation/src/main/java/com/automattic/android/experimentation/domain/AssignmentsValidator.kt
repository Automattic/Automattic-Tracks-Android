 package com.automattic.android.experimentation.domain

 import java.util.Date

 internal class AssignmentsValidator(private val clock: Clock) {

    private val Assignments.expiresAt
        get() = fetchedAt + timeToLive

    val Assignments.isStale
        get() = clock.currentTimeSeconds() > expiresAt
 }
