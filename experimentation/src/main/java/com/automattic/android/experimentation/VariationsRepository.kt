package com.automattic.android.experimentation

import com.automattic.android.experimentation.domain.Assignments
import com.automattic.android.experimentation.domain.AssignmentsValidator
import com.automattic.android.experimentation.domain.SystemClock
import com.automattic.android.experimentation.domain.Variation
import com.automattic.android.experimentation.local.FileBasedCache
import com.automattic.android.experimentation.remote.ExperimentRestClient
import com.automattic.android.experimentation.repository.AssignmentsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.Call
import okhttp3.OkHttpClient
import java.io.File

/**
 * Repository for accessing [Variation]s for [Experiment]s.
 *
 * Should be a Singleton, as it keeps track of the "active" [Variation]s. See [getVariation] for more details.
 */
public interface VariationsRepository {
    /**
     * Initializes the [VariationsRepository].
     *
     * @param anonymousId Identifier for the user. This should be unique for each user.
     * @param oAuthToken Optional OAuth token for the user. This is used to fetch the [Assignments] from the server.
     *
     */
    public fun initialize(anonymousId: String, oAuthToken: String? = null)

    /**
     * Returns a [Variation] for the provided [Experiment]. [Variation] is then considered "active".
     *
     * Subsequent calls to this method with the same [Experiment] will return the same "active" [Variation] until [clear] or is called.
     * This is a safety mechanism that prevents mixing [Variation] during the same session as this might lead to unexpected behavior.
     *
     * @param experiment [Experiment] for which to get the [Variation]. Must be included in the set provided via [create].
     *
     */
    public fun getVariation(experiment: Experiment): Variation

    /**
     * Clears the [VariationsRepository] state. This will clear "active" [Variation]s.
     * See [getVariation] for more details.
     */
    public fun clear()

    public companion object {
        /**
         * Creates a new instance of [VariationsRepository].
         *
         * @param platform The platform identifier. See ExPlat documentation for more details.
         * @param experiments Set of [Experiment]s to be used by the [VariationsRepository].
         * @param logger to log errors and debug information.
         * @param failFast If `true`, [getVariation] will throw exceptions if the [VariationsRepository] is not initialized or if the [Experiment] is not found.
         * @param cacheDir Directory to use for caching the [Assignments]. This directory should be private to the application.
         * @param callFactory to use for network operations. Defaults to a new instance of [OkHttpClient].
         * @param coroutineScope to use for async operations. Preferably a [CoroutineScope] that is tied to the lifecycle of the application.
         * @param dispatcher to use for async I/O operations. Defaults to [Dispatchers.IO].
         */
        public fun create(
            platform: String,
            experiments: Set<Experiment>,
            logger: ExperimentLogger,
            failFast: Boolean,
            cacheDir: File,
            callFactory: Call.Factory = OkHttpClient(),
            coroutineScope: CoroutineScope,
            dispatcher: CoroutineDispatcher = Dispatchers.IO,
        ): VariationsRepository {
            return ExPlat(
                platform = platform,
                experiments = experiments,
                logger = logger,
                coroutineScope = coroutineScope,
                failFast = failFast,
                assignmentsValidator = AssignmentsValidator(SystemClock()),
                repository = AssignmentsRepository(
                    ExperimentRestClient(dispatcher = dispatcher, callFactory = callFactory),
                    FileBasedCache(
                        cacheDir,
                        dispatcher = dispatcher,
                        scope = coroutineScope,
                        logger = logger,
                        failFast = failFast,
                    ),
                ),
            )
        }
    }
}
