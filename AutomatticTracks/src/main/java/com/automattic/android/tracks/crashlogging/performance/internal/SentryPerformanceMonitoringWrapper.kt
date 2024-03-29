package com.automattic.android.tracks.crashlogging.performance.internal

import io.sentry.ITransaction
import io.sentry.Sentry
import io.sentry.TransactionOptions

class SentryPerformanceMonitoringWrapper {

    fun startTransaction(name: String, operation: String, bindToScope: Boolean): ITransaction {
        return Sentry.startTransaction(
            name,
            operation,
            TransactionOptions().apply { isBindToScope = bindToScope }
        )
    }
}
