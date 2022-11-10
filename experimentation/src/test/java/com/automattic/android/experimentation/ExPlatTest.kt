package com.automattic.android.experimentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.experiments.Assignments
import org.wordpress.android.fluxc.model.experiments.Variation
import org.wordpress.android.fluxc.model.experiments.Variation.Control
import org.wordpress.android.fluxc.model.experiments.Variation.Treatment
import org.wordpress.android.fluxc.store.ExperimentStore
import org.wordpress.android.fluxc.store.ExperimentStore.OnAssignmentsFetched
import org.wordpress.android.fluxc.store.ExperimentStore.Platform
import org.wordpress.android.fluxc.utils.AppLogWrapper
import java.util.Date

@ExperimentalCoroutinesApi
class ExPlatTest {
    private val platform = Platform.WORDPRESS_ANDROID
    private val experimentStore: ExperimentStore = mock()
    private val appLogWrapper: AppLogWrapper = mock()
    private var exPlat: ExPlat = createExPlat(
        isDebug = false,
        experiments = emptySet()
    )
    private val dummyExperiment = object : Experiment {
        override val identifier: String = "dummy"
    }

    @Test
    fun `refreshIfNeeded fetches assignments if cache is null`() = runBlockingTest {
        exPlat = createExPlat(
            isDebug = true,
            experiments = setOf(dummyExperiment)
        )
        setupAssignments(cachedAssignments = null, fetchedAssignments = buildAssignments())

        exPlat.refreshIfNeeded()

        verify(experimentStore, times(1)).fetchAssignments(eq(platform), any(), anyOrNull())
    }

    @Test
    fun `refreshIfNeeded fetches assignments if cache is stale`() = runBlockingTest {
        exPlat = createExPlat(
            isDebug = true,
            experiments = setOf(dummyExperiment)
        )
        setupAssignments(cachedAssignments = buildAssignments(isStale = true), fetchedAssignments = buildAssignments())

        exPlat.refreshIfNeeded()

        verify(experimentStore, times(1)).fetchAssignments(eq(platform), any(), anyOrNull())
    }

    @Test
    fun `refreshIfNeeded does not fetch assignments if cache is fresh`() = runBlockingTest {
        setupAssignments(cachedAssignments = buildAssignments(isStale = false), fetchedAssignments = buildAssignments())

        exPlat.refreshIfNeeded()

        verify(experimentStore, never()).fetchAssignments(eq(platform), any(), anyOrNull())
    }

    @Test
    fun `forceRefresh fetches assignments if cache is fresh`() = runBlockingTest {
        exPlat = createExPlat(
            isDebug = true,
            experiments = setOf(dummyExperiment)
        )
        setupAssignments(cachedAssignments = buildAssignments(isStale = true), fetchedAssignments = buildAssignments())

        exPlat.forceRefresh()

        verify(experimentStore, times(1)).fetchAssignments(eq(platform), any(), anyOrNull())
    }

    @Test
    fun `clear calls experiment store`() = runBlockingTest {
        exPlat.clear()

        verify(experimentStore, times(1)).clearCachedAssignments()
    }

    @Test
    fun `getVariation fetches assignments if cache is null`() = runBlockingTest {
        exPlat = createExPlat(
            isDebug = true,
            experiments = setOf(dummyExperiment)
        )
        setupAssignments(cachedAssignments = null, fetchedAssignments = buildAssignments())

        exPlat.getVariation(dummyExperiment, shouldRefreshIfStale = true)

        verify(experimentStore, times(1)).fetchAssignments(eq(platform), any(), anyOrNull())
    }

    @Test
    fun `getVariation fetches assignments if cache is stale`() = runBlockingTest {
        exPlat = createExPlat(
            isDebug = true,
            experiments = setOf(dummyExperiment)
        )
        setupAssignments(cachedAssignments = buildAssignments(isStale = true), fetchedAssignments = buildAssignments())

        exPlat.getVariation(dummyExperiment, shouldRefreshIfStale = true)

        verify(experimentStore, times(1)).fetchAssignments(eq(platform), any(), anyOrNull())
    }

    @Test
    fun `getVariation does not fetch assignments if cache is fresh`() = runBlockingTest {
        setupAssignments(cachedAssignments = buildAssignments(isStale = false), fetchedAssignments = buildAssignments())

        exPlat.getVariation(dummyExperiment, shouldRefreshIfStale = true)

        verify(experimentStore, never()).fetchAssignments(eq(platform), any(), anyOrNull())
    }

    @Test
    fun `getVariation does not fetch assignments if cache is null but shouldRefreshIfStale is false`() = runBlockingTest {
        setupAssignments(cachedAssignments = null, fetchedAssignments = buildAssignments())

        exPlat.getVariation(dummyExperiment, shouldRefreshIfStale = false)

        verify(experimentStore, never()).fetchAssignments(eq(platform), any(), anyOrNull())
    }

    @Test
    fun `getVariation does not fetch assignments if cache is stale but shouldRefreshIfStale is false`() = runBlockingTest {
        setupAssignments(cachedAssignments = null, fetchedAssignments = buildAssignments())

        exPlat.getVariation(dummyExperiment, shouldRefreshIfStale = false)

        verify(experimentStore, never()).fetchAssignments(eq(platform), any(), anyOrNull())
    }

    @Test
    fun `getVariation does not return different cached assignments if active variation exists`() = runBlockingTest {
        val controlVariation = Control
        val treatmentVariation = Treatment("treatment")

        val treatmentAssignments = buildAssignments(variations = mapOf(dummyExperiment.identifier to treatmentVariation))

        setupAssignments(cachedAssignments = null, fetchedAssignments = treatmentAssignments)

        val firstVariation = exPlat.getVariation(dummyExperiment, shouldRefreshIfStale = false)
        assertThat(firstVariation).isEqualTo(controlVariation)

        exPlat.forceRefresh()

        setupAssignments(cachedAssignments = treatmentAssignments, fetchedAssignments = treatmentAssignments)

        val secondVariation = exPlat.getVariation(dummyExperiment, shouldRefreshIfStale = false)
        assertThat(secondVariation).isEqualTo(controlVariation)
    }

    @Test
    fun `forceRefresh fetches assignments if experiments is not empty`() = runBlockingTest {
        exPlat = createExPlat(
            isDebug = true,
            experiments = setOf(dummyExperiment)
        )
        exPlat.forceRefresh()

        verify(experimentStore, times(1)).fetchAssignments(eq(platform), any(), anyOrNull())
    }

    @Test
    fun `forceRefresh does not interact with store if experiments is empty`() = runBlockingTest {
        exPlat.forceRefresh()

        verifyNoInteractions(experimentStore)
    }

    @Test
    fun `refreshIfNeeded does not interact with store if experiments is empty`() = runBlockingTest {
        exPlat.refreshIfNeeded()

        verifyNoInteractions(experimentStore)
    }

    @Test
    fun `getVariation does not interact with store if experiments is empty`() = runBlockingTest {
        try {
            exPlat.getVariation(dummyExperiment, false)
        } catch (e: IllegalArgumentException) {
            // Do nothing.
        } finally {
            verifyNoInteractions(experimentStore)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getVariation throws IllegalArgumentException if experiment was not found and is debug`() {
        runBlockingTest {
            exPlat = createExPlat(
                isDebug = true,
                experiments = emptySet()
            )
            exPlat.getVariation(dummyExperiment, false)
        }
    }

    private fun createExPlat(isDebug: Boolean, experiments: Set<Experiment>): ExPlat =
        ExPlat(
            platform = platform,
            experiments = experiments,
            experimentStore = experimentStore,
            appLogWrapper = appLogWrapper,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
            isDebug = isDebug
        )

    private suspend fun setupAssignments(cachedAssignments: Assignments?, fetchedAssignments: Assignments) {
        whenever(experimentStore.getCachedAssignments()).thenReturn(cachedAssignments)
        whenever(experimentStore.fetchAssignments(eq(platform), any(), anyOrNull()))
            .thenReturn(OnAssignmentsFetched(fetchedAssignments))
    }

    private fun buildAssignments(
        isStale: Boolean = false,
        variations: Map<String, Variation> = emptyMap()
    ): Assignments {
        val now = System.currentTimeMillis()
        val oneHourAgo = now - ONE_HOUR_IN_SECONDS * 1000
        val oneHourFromNow = now + ONE_HOUR_IN_SECONDS * 1000
        return if (isStale) {
            Assignments(variations, ONE_HOUR_IN_SECONDS, Date(oneHourAgo))
        } else {
            Assignments(variations, ONE_HOUR_IN_SECONDS, Date(oneHourFromNow))
        }
    }

    companion object {
        private const val ONE_HOUR_IN_SECONDS = 3600
    }
}
