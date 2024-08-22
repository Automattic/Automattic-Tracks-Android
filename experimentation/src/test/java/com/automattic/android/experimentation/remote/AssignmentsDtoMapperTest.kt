package com.automattic.android.experimentation.remote

import com.automattic.android.experimentation.domain.Assignments
import com.automattic.android.experimentation.domain.Variation.Control
import com.automattic.android.experimentation.domain.Variation.Treatment
import com.automattic.android.experimentation.remote.AssignmentsDtoMapper.toAssignments
import org.junit.Assert.assertEquals
import org.junit.Test

class AssignmentsDtoMapperTest {

    @Test
    fun `mapping from dto to domain object is successful`() {
        val ttl = 100
        val fetchedAt = 123456L
        val dto = AssignmentsDto(
            variations = mapOf(
                "experiment1" to "treatment1",
                "experiment2" to "control",
                "experiment3" to null,
            ),
            ttl = ttl,
        )
        val expected = Assignments(
            variations = mapOf(
                "experiment1" to Treatment("treatment1"),
                "experiment2" to Control,
                "experiment3" to Control,
            ),
            timeToLive = ttl,
            fetchedAt = fetchedAt,
        )

        val result = dto.toAssignments(fetchedAt)

        assertEquals(expected, result)
    }
}
