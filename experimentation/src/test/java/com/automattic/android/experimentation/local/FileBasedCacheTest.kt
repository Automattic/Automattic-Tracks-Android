package com.automattic.android.experimentation.local

import com.automattic.android.experimentation.ExperimentLogger
import com.automattic.android.experimentation.domain.Assignments
import com.automattic.android.experimentation.domain.Variation.Control
import com.automattic.android.experimentation.domain.Variation.Treatment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.io.path.createTempDirectory

@OptIn(ExperimentalCoroutinesApi::class)
internal class FileBasedCacheTest {

    @get:Rule
    val tempDir = TemporaryFolder()

    @Test
    fun `saving and reading assignments is successful`() = runTest {
        val sut = fileBasedCache(this)
        sut.saveAssignments(TEST_ASSIGNMENTS)

        val result = sut.getAssignments()

        assertEquals(TEST_ASSIGNMENTS, result)
    }

    @Test
    fun `updating assignments is successful`() = runTest {
        val sut = fileBasedCache(this)
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
        val sut = fileBasedCache(this)

        val result = sut.getAssignments()

        assertNull(result)
    }

    @Test
    fun `clearing empty cache has no effect`() = runTest {
        val sut = fileBasedCache(this)

        sut.clear()
    }

    @Test
    fun `getting assignments from empty file returns null and deletes the file`() = runTest {
        val cacheDir = tempDir.newFolder()
        val assignmentsFile = File(cacheDir, "assignments.json").apply { createNewFile() }
        val sut = fileBasedCache(this, cacheDir = cacheDir)

        val result = sut.getAssignments()

        assertNull(result)
        assertFalse(assignmentsFile.exists())
    }

    @Test
    fun `getting assignments from corrupted file returns null and deletes the file`() = runTest {
        val cacheDir = tempDir.newFolder()
        val assignmentsFile = File(cacheDir, "assignments.json").apply { writeText("{corrupted") }
        val sut = fileBasedCache(this, cacheDir = cacheDir)

        val result = sut.getAssignments()

        assertNull(result)
        assertFalse(assignmentsFile.exists())
    }

    @Test
    fun `saving cache when cache dir doesnt exist is successful`() = runTest {
        val cacheDir = File(tempDir.newFolder(), "cache").apply { assert(!this.exists()) }
        val sut = fileBasedCache(this, cacheDir = cacheDir)
        sut.saveAssignments(TEST_ASSIGNMENTS)

        val result = sut.getAssignments()

        assertEquals(TEST_ASSIGNMENTS, result)
        cacheDir.deleteRecursively()
    }

    private fun fileBasedCache(
        scope: TestScope,
        cacheDir: File = createTempDirectory().toFile(),
    ) = FileBasedCache(
        cacheDir = cacheDir,
        dispatcher = StandardTestDispatcher(scope.testScheduler),
        scope = scope,
        logger = object : ExperimentLogger {
            override fun d(message: String) = Unit
            override fun e(message: String, throwable: Throwable?) = Unit
        },
        failFast = true,
    )

    companion object {
        private val TEST_ASSIGNMENTS = Assignments(
            variations = mapOf(
                "experiment1" to Control,
                "experiment2" to Treatment("variation2"),
            ),
            timeToLive = 3600,
            fetchedAt = 123456789L,
            anonymousId = "id",
        )
    }
}
