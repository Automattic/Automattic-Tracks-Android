package com.automattic.android.tracks.crashlogging.performance

import io.sentry.ITransaction
import io.sentry.Sentry
import java.util.UUID

class TransactionRepository {

    private val transactions: MutableMap<TransactionId, ITransaction> = mutableMapOf()

    fun startTransaction(name: String, operation: TransactionOperation): TransactionId {
        val transaction = Sentry.startTransaction(name, operation.value, true)
        val uuid = TransactionId(UUID.randomUUID().toString())

        transactions[uuid] = transaction

        return uuid
    }

    fun finishTransaction(transactionId: TransactionId) {
        transactions[transactionId]?.finish()
    }
}
