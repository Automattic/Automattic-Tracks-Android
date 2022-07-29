package com.automattic.android.tracks.crashlogging.performance

import com.automattic.android.tracks.crashlogging.performance.internal.SentryPerformanceMonitoringWrapper

object PerformanceMonitoringRepositoryProvider {

    fun createInstance(): PerformanceTransactionRepository =
        PerformanceTransactionRepository(SentryPerformanceMonitoringWrapper())
}
