package com.automattic.android.experimentation.domain

import com.automattic.android.experimentation.domain.Variation.Control


data class Assignments(
    val variations: Map<String, Variation>,
    val timeToLive: Int,
    val fetchedAt: Long,
) {
    fun getVariation(experiment: String) = variations[experiment] ?: Control
}
