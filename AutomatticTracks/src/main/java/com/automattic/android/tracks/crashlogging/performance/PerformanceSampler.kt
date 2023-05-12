package com.automattic.android.tracks.crashlogging.performance

import com.automattic.android.tracks.crashlogging.PerformanceMonitoringConfig

interface PerformanceSampler {

    fun sample(transactionName: String): PerformanceMonitoringConfig
}
