package com.automattic.android.tracks.experiment

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.model.experiments.Assignments
import org.wordpress.android.fluxc.model.experiments.Variation
import org.wordpress.android.fluxc.model.experiments.Variation.Control
import org.wordpress.android.fluxc.model.experiments.Variation.Treatment
import org.wordpress.android.fluxc.store.ExperimentStore
import org.wordpress.android.fluxc.store.ExperimentStore.OnAssignmentsFetched
import org.wordpress.android.fluxc.utils.AppLogWrapper
import java.util.Date

@ExperimentalCoroutinesApi
class ExPlatTest {
    private val experiments: Set<Experiment> = mock()
    private val experimentStore: ExperimentStore = mock()
    private val appLogWrapper: AppLogWrapper = mock()
    private lateinit var exPlat: ExPlat
    private lateinit var dummyExperiment: Experiment

    @Before
    fun setUp() {
        exPlat = createExPlat(isDebug = false)
        dummyExperiment = object : Experiment("dummy", exPlat) {}

        setupExperiments(setOf(dummyExperiment))
    }

    @Test
    fun `refreshIfNeeded fetches assignments if cache is null`() = runTest {
        setupAssignments(cachedAssignments = null, fetchedAssignments = buildAssignments())

        exPlat.refreshIfNeeded()

        verify(experimentStore, times(1)).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `refreshIfNeeded fetches assignments if cache is stale`() = runTest {
        setupAssignments(cachedAssignments = buildAssignments(isStale = true), fetchedAssignments = buildAssignments())

        exPlat.refreshIfNeeded()

        verify(experimentStore, times(1)).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `refreshIfNeeded does not fetch assignments if cache is fresh`() = runTest {
        setupAssignments(cachedAssignments = buildAssignments(isStale = false), fetchedAssignments = buildAssignments())

        exPlat.refreshIfNeeded()

        verify(experimentStore, never()).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `forceRefresh fetches assignments if cache is fresh`() = runTest {
        setupAssignments(cachedAssignments = buildAssignments(isStale = true), fetchedAssignments = buildAssignments())

        exPlat.forceRefresh()

        verify(experimentStore, times(1)).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `clear calls experiment store`() = runTest {
        exPlat.clear()

        verify(experimentStore, times(1)).clearCachedAssignments()
    }

    @Test
    fun `getVariation fetches assignments if cache is null`() = runTest {
        setupAssignments(cachedAssignments = null, fetchedAssignments = buildAssignments())

        exPlat.getVariation(dummyExperiment, shouldRefreshIfStale = true)

        verify(experimentStore, times(1)).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `getVariation fetches assignments if cache is stale`() = runTest {
        setupAssignments(cachedAssignments = buildAssignments(isStale = true), fetchedAssignments = buildAssignments())

        exPlat.getVariation(dummyExperiment, shouldRefreshIfStale = true)

        verify(experimentStore, times(1)).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `getVariation does not fetch assignments if cache is fresh`() = runTest {
        setupAssignments(cachedAssignments = buildAssignments(isStale = false), fetchedAssignments = buildAssignments())

        exPlat.getVariation(dummyExperiment, shouldRefreshIfStale = true)

        verify(experimentStore, never()).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `getVariation does not fetch assignments if cache is null but shouldRefreshIfStale is false`() = runTest {
        setupAssignments(cachedAssignments = null, fetchedAssignments = buildAssignments())

        exPlat.getVariation(dummyExperiment, shouldRefreshIfStale = false)

        verify(experimentStore, never()).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `getVariation does not fetch assignments if cache is stale but shouldRefreshIfStale is false`() = runTest {
        setupAssignments(cachedAssignments = null, fetchedAssignments = buildAssignments())

        exPlat.getVariation(dummyExperiment, shouldRefreshIfStale = false)

        verify(experimentStore, never()).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `getVariation does not return different cached assignments if active variation exists`() = runTest {
        val controlVariation = Control
        val treatmentVariation = Treatment("treatment")

        val treatmentAssignments = buildAssignments(variations = mapOf(dummyExperiment.name to treatmentVariation))

        setupAssignments(cachedAssignments = null, fetchedAssignments = treatmentAssignments)

        val firstVariation = exPlat.getVariation(dummyExperiment, shouldRefreshIfStale = false)
        assertThat(firstVariation).isEqualTo(controlVariation)

        exPlat.forceRefresh()

        setupAssignments(cachedAssignments = treatmentAssignments, fetchedAssignments = treatmentAssignments)

        val secondVariation = exPlat.getVariation(dummyExperiment, shouldRefreshIfStale = false)
        assertThat(secondVariation).isEqualTo(controlVariation)
    }

    @Test
    fun `forceRefresh fetches assignments if experiments is not empty`() = runTest {
        setupExperiments(setOf(dummyExperiment))

        exPlat.forceRefresh()

        verify(experimentStore, times(1)).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `forceRefresh does not interact with store if experiments is empty`() = runTest {
        setupExperiments(emptySet())

        exPlat.forceRefresh()

        verifyZeroInteractions(experimentStore)
    }

    @Test
    fun `refreshIfNeeded does not interact with store if experiments is empty`() = runTest {
        setupExperiments(emptySet())

        exPlat.refreshIfNeeded()

        verifyZeroInteractions(experimentStore)
    }

    @Test
    fun `getVariation does not interact with store if experiments is empty`() = runTest {
        setupExperiments(emptySet())

        try {
            exPlat.getVariation(dummyExperiment, false)
        } catch (e: IllegalArgumentException) {
            // Do nothing.
        } finally {
            verifyZeroInteractions(experimentStore)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getVariation throws IllegalArgumentException if experiment was not found and is debug`() {
        runTest {
            val exPlatDebug = createExPlat(isDebug = true)
            setupExperiments(emptySet())
            exPlatDebug.getVariation(dummyExperiment, false)
        }
    }

    private fun createExPlat(isDebug: Boolean): ExPlat =
            ExPlat(
                    experiments = experiments,
                    experimentStore = experimentStore,
                    appLogWrapper = appLogWrapper,
                    coroutineScope = CoroutineScope(Dispatchers.Unconfined),
                    isDebug = isDebug
            )

    private fun setupExperiments(experiments: Set<Experiment>) {
        whenever(this.experiments).thenReturn(experiments)
    }

    private suspend fun setupAssignments(cachedAssignments: Assignments?, fetchedAssignments: Assignments) {
        whenever(experimentStore.getCachedAssignments()).thenReturn(cachedAssignments)
        whenever(experimentStore.fetchAssignments(any(), any(), anyOrNull()))
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
