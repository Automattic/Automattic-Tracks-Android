package com.automattic.android.tracks.crashlogging.performance

import com.automattic.android.tracks.crashlogging.PerformanceMonitoringConfig

object NoopPerformanceSampler : PerformanceSampler {
    override fun sample(
        transactionName: String,
        transactionStatus: TransactionStatus
    ): PerformanceMonitoringConfig {
        // no-op
        return PerformanceMonitoringConfig.Disabled
    }
}
