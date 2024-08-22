package com.automattic.android.experimentation.remote

import com.automattic.android.experimentation.domain.Assignments
import com.automattic.android.experimentation.domain.Variation

internal object AssignmentsDtoMapper {
    fun AssignmentsDto.toAssignments(fetchedAt: Long): Assignments {
        return Assignments(
            variations = variations.mapValues { (_, value) ->
                if (value == null || value == "control") {
                    Variation.Control
                } else {
                    Variation.Treatment(value)
                }
            },
            timeToLive = ttl,
            fetchedAt = fetchedAt,
        )
    }
    fun Assignments.toDto(): Pair<AssignmentsDto, Long> {
        return AssignmentsDto(
            variations = variations.mapValues { (_, value) ->
                when (value) {
                    is Variation.Control -> null
                    is Variation.Treatment -> value.name
                }
            },
            ttl = timeToLive,
        ) to fetchedAt
    }
}
