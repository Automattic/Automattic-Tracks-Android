package com.automattic.android.experimentation.local

import com.automattic.android.experimentation.domain.Assignments
import com.automattic.android.experimentation.domain.Variation.Control
import com.automattic.android.experimentation.domain.Variation.Treatment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.io.path.createTempDirectory

@OptIn(ExperimentalCoroutinesApi::class)
internal class FileBasedCacheTest {

    private lateinit var sut: FileBasedCache

    @Test
    fun `saving and reading assignments is successful`() = runTest {
        sut = fileBasedCache()
        sut.saveAssignments(TEST_ASSIGNMENTS)

        val result = sut.getAssignments()

        assertEquals(TEST_ASSIGNMENTS, result)
    }

    @Test
    fun `updating assignments is successful`() = runTest {
        sut = fileBasedCache()
        val updatedTestAssignments = TEST_ASSIGNMENTS.copy(
            variations = mapOf(
                "experiment1" to Treatment("variation1"),
                "experiment2" to Control,
            ),
            fetchedAt = 987654321L,
        )
        sut.saveAssignments(TEST_ASSIGNMENTS)

        sut.saveAssignments(updatedTestAssignments)
        val result = sut.getAssignments()

        assertEquals(updatedTestAssignments, result)
    }

    @Test
    fun `getting assignments from empty cache returns no results`() = runTest {
        sut = fileBasedCache()

        val result = sut.getAssignments()

        assertNull(result)
    }

    @Test
    fun `clearing cache without content doesn't crash`() = runTest {
        sut = fileBasedCache()

        sut.clear()
    }

    @Test
    fun `clearing cache is successful`() = runTest {
        sut = fileBasedCache()
        sut.saveAssignments(TEST_ASSIGNMENTS)

        sut.clear()

        val result = sut.getAssignments()
        assertNull(result)
    }

    private fun fileBasedCache() =
        FileBasedCache(cacheDir = createTempDirectory().toFile())

    companion object {
        private val TEST_ASSIGNMENTS = Assignments(
            variations = mapOf(
                "experiment1" to Control,
                "experiment2" to Treatment("variation2"),
            ),
            ttl = 3600,
            fetchedAt = 123456789L,
        )
    }
}
