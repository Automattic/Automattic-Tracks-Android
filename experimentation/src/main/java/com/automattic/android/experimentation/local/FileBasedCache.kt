package com.automattic.android.experimentation.local

import com.automattic.android.experimentation.domain.Assignments
import com.automattic.android.experimentation.remote.AssignmentsDtoJsonAdapter
import com.automattic.android.experimentation.remote.AssignmentsDtoMapper.toAssignments
import com.automattic.android.experimentation.remote.AssignmentsDtoMapper.toDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal class FileBasedCache(
    private val cacheDir: File,
    private val moshi: Moshi = Moshi.Builder().build(),
    private val jsonAdapter: AssignmentsDtoJsonAdapter = AssignmentsDtoJsonAdapter(moshi),
) {

    private val assignmentsFile = File(cacheDir, "assignments.json")
    private val type = Types.newParameterizedType(
        Map::class.java,
        Long::class.javaObjectType,
        String::class.java,
    )
    private val wrapperAdapter = moshi.adapter<Map<Long, String>>(type)

    suspend fun getAssignments(): Assignments? {
        return withContext(Dispatchers.IO) {
            assignmentsFile.takeIf { it.exists() }?.readText()?.let { json ->
                val fromJson = wrapperAdapter.fromJson(json) ?: return@let null

                val (fetchedAt, dtoJson) = fromJson.toList().first()

                val dto = jsonAdapter.fromJson(dtoJson)

                dto?.toAssignments(fetchedAt)
            }
        }
    }

    suspend fun saveAssignments(assignments: Assignments) {
        withContext(Dispatchers.IO) {
            val (dto, fetchedAt) = assignments.toDto()
            val dtoJson = jsonAdapter.serializeNulls().toJson(dto)

            val wrapperJson = wrapperAdapter.toJson(mapOf(fetchedAt to dtoJson))
            assignmentsFile.writeText(wrapperJson)
        }
    }
}
