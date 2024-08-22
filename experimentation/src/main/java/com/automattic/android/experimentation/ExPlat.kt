package com.automattic.android.experimentation

import com.automattic.android.experimentation.domain.Assignments
import com.automattic.android.experimentation.domain.Variation
import com.automattic.android.experimentation.domain.Variation.Control
import com.automattic.android.experimentation.ExPlat.RefreshStrategy.ALWAYS
import com.automattic.android.experimentation.ExPlat.RefreshStrategy.IF_STALE
import com.automattic.android.experimentation.ExPlat.RefreshStrategy.NEVER
import com.automattic.android.experimentation.domain.AssignmentsValidator
import com.automattic.android.experimentation.domain.SystemClock
import com.automattic.android.experimentation.local.FileBasedCache
import com.automattic.android.experimentation.remote.AssignmentsDtoJsonAdapter
import com.automattic.android.experimentation.remote.ExPlatUrlBuilder
import com.automattic.android.experimentation.remote.ExperimentRestClient
import com.squareup.moshi.Moshi
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.wordpress.android.fluxc.store.ExperimentStore.Platform
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog.T

class ExPlat internal constructor(
    private val platform: Platform,
    private val experiments: Set<Experiment>,
    private val appLogWrapper: AppLogWrapper,
    private val coroutineScope: CoroutineScope,
    private val isDebug: Boolean,
    private val assignmentsRepository: AssignmentsRepository,
    private val assignmentsValidator: AssignmentsValidator
) {
    private val experimentIdentifiers: List<String> = experiments.map { it.identifier }

    /**
     * This returns the current active [Variation] for the provided [Experiment].
     */
    fun getVariation(
        experiment: Experiment,
        shouldRefreshIfStale: Boolean = false,
    ): Flow<Variation> {
        return flow<Variation> {
            val experimentIdentifier = experiment.identifier
            if (!experimentIdentifiers.contains(experimentIdentifier)) {
                val message = "ExPlat: experiment not found: \"${experimentIdentifier}\"! " +
                        "Make sure to include it in the set provided via constructor."
                appLogWrapper.e(T.API, message)
                if (isDebug) throw IllegalArgumentException(message) else emit(Control)
            }

            emit(
                getAssignments(if (shouldRefreshIfStale) IF_STALE else NEVER)
                    .getVariation(experimentIdentifier)
            )

        }
    }

    suspend fun refreshIfNeeded() {
        refresh(refreshStrategy = IF_STALE)
    }

    suspend fun forceRefresh() {
        refresh(refreshStrategy = ALWAYS)
    }

    suspend fun clear() {
        appLogWrapper.d(T.API, "ExPlat: clearing cached assignments and active variations")
        runBlocking {
            assignmentsRepository.clearCachedAssignments()
        }
    }

    private suspend fun refresh(refreshStrategy: RefreshStrategy) {
        if (experimentIdentifiers.isNotEmpty()) {
            getAssignments(refreshStrategy)
        }
    }

    private suspend fun getAssignments(refreshStrategy: RefreshStrategy): Assignments {
        val cachedAssignments = assignmentsRepository.getCachedAssignments()

        return if (
            cachedAssignments == null ||
            refreshStrategy == ALWAYS ||
            (refreshStrategy == IF_STALE && assignmentsValidator.run { cachedAssignments.isStale })
        ) {
            fetchAssignments()
            assignmentsRepository.getCachedAssignments() ?: Assignments(emptyMap(), 0, 0)
        } else {
            cachedAssignments
        }
    }

    private suspend fun fetchAssignments() =
        assignmentsRepository.fetchAssignments(platform.value, experimentIdentifiers).fold(
            onFailure = {
                appLogWrapper.d(
                    T.API,
                    "ExPlat: fetching assignments failed with result: $it"
                )
            },
            onSuccess = {
                appLogWrapper.d(
                    T.API,
                    "ExPlat: fetching assignments successful with result: $it"
                )
            }
        )

    private enum class RefreshStrategy { ALWAYS, IF_STALE, NEVER }

    companion object {
        fun create(
            platform: Platform,
            experiments: Set<Experiment>,
            appLogWrapper: AppLogWrapper,
            coroutineScope: CoroutineScope,
            isDebug: Boolean,
            cacheDir: File,
        ) {
            val moshi = Moshi.Builder().build()
            val jsonAdapter = AssignmentsDtoJsonAdapter(moshi)
            val clock = SystemClock()
            ExPlat(
                platform = platform,
                experiments = experiments,
                appLogWrapper = appLogWrapper,
                coroutineScope = coroutineScope,
                isDebug = isDebug,
                assignmentsRepository = AssignmentsRepository(
                    ExperimentRestClient(
                        OkHttpClient(),
                        moshi,
                        jsonAdapter,
                        ExPlatUrlBuilder(),
                        clock,
                    ),
                    FileBasedCache(cacheDir, moshi, jsonAdapter),
                ),
                assignmentsValidator = AssignmentsValidator(clock)
            )
        }
    }
}
