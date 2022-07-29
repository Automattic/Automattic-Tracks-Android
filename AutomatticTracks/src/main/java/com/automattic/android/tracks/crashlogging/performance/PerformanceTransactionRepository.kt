package com.automattic.android.tracks.crashlogging.performance

import com.automattic.android.tracks.crashlogging.performance.internal.SentryPerformanceMonitoringWrapper
import io.sentry.ITransaction

/**
 * This class has to be a Singleton. It holds state of active performance transactions.
 */
class PerformanceTransactionRepository internal constructor(private val sentryWrapper: SentryPerformanceMonitoringWrapper) {

    private val transactions: MutableMap<TransactionId, ITransaction> = mutableMapOf()

    fun startTransaction(name: String, operation: TransactionOperation): TransactionId {
        return sentryWrapper.startTransaction(name, operation.value, true).let {
            val transactionId = TransactionId(it.eventId.toString())
            transactions[transactionId] = it
            transactionId
        }
    }

    fun finishTransaction(transactionId: TransactionId, transactionStatus: TransactionStatus) {
        transactions[transactionId]?.finish(transactionStatus.toSentrySpanStatus())
    }
}
