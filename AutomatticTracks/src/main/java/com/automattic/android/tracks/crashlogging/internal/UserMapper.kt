package com.automattic.android.tracks.crashlogging.internal

import com.automattic.android.tracks.crashlogging.CrashLoggingUser
import io.sentry.protocol.User

internal fun CrashLoggingUser.toSentryUser(): User = User().let { sentryUser ->
    sentryUser.email = email
    sentryUser.username = username
    sentryUser.id = userID
    sentryUser
}
