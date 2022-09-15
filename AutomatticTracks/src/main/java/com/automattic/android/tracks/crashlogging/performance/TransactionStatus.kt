package com.automattic.android.tracks.crashlogging.performance

import io.sentry.SpanStatus

enum class TransactionStatus {
    SUCCESSFUL, ABORTED
}

fun TransactionStatus.toSentrySpanStatus() =
    when (this) {
        TransactionStatus.SUCCESSFUL -> SpanStatus.OK
        TransactionStatus.ABORTED -> SpanStatus.ABORTED
    }

fun SpanStatus?.toTracksTransactionStatus(): TransactionStatus =
    when (this) {
        SpanStatus.OK -> TransactionStatus.SUCCESSFUL
        else -> TransactionStatus.ABORTED
    }
