package com.automattic.android.tracks.crashlogging.performance

import com.automattic.android.tracks.crashlogging.performance.internal.SentryPerformanceMonitoringWrapper

object PerformanceMonitoringRepositoryProvider {

    private val instance = PerformanceTransactionRepository(SentryPerformanceMonitoringWrapper())

    fun createInstance(): PerformanceTransactionRepository = instance
}
