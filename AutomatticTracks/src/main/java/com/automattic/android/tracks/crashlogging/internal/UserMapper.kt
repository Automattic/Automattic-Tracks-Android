package com.automattic.android.tracks.crashlogging.internal

import com.automattic.android.tracks.TracksUser
import io.sentry.protocol.User

internal fun TracksUser.toSentryUser(): User = User().let { sentryUser ->
    sentryUser.email = email
    sentryUser.username = username
    sentryUser.id = userID
    sentryUser.others = context
    sentryUser
}
