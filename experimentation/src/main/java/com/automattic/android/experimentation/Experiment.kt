package com.automattic.android.experimentation

import org.wordpress.android.fluxc.model.experiments.Variation

open class Experiment(
    val name: String,
    private val exPlat: ExPlat
) {
    @Suppress("unused")
    @JvmOverloads
    fun getVariation(shouldRefreshIfStale: Boolean = false): Variation {
        return exPlat.getVariation(
            experimentName = name,
            shouldRefreshIfStale = shouldRefreshIfStale
        )
    }
}
