package com.automattic.android.experimentation.remote

import com.automattic.android.experimentation.domain.Assignments
import com.automattic.android.experimentation.domain.Clock
import com.automattic.android.experimentation.domain.SystemClock
import com.automattic.android.experimentation.remote.AssignmentsDtoMapper.toAssignments
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

internal class ExperimentRestClient(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi = Moshi.Builder().build(),
    private val jsonAdapter: AssignmentsDtoJsonAdapter = AssignmentsDtoJsonAdapter(moshi),
    private val urlBuilder: UrlBuilder = ExPlatUrlBuilder(),
    private val clock: Clock = SystemClock(),
    private val dispatcher: CoroutineDispatcher,
) {

    suspend fun fetchAssignments(
        platform: String,
        experimentNames: List<String>,
        anonymousId: String,
        oAuthToken: String?,
    ): Result<Assignments> {
        val url = urlBuilder.buildUrl(platform, experimentNames, anonymousId)

        val request = Request.Builder()
            .url(url)
            .apply { oAuthToken?.let { header("Authorization", "Bearer $it") } }
            .get()
            .build()

        return withContext(dispatcher) {
            try {
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Result.failure(IOException("Unexpected code $response"))
                    } else {
                        runCatching {
                            val dto = jsonAdapter.fromJson(response.body!!.source())!!
                            dto.toAssignments(
                                fetchedAt = clock.currentTimeSeconds(),
                                anonymousId = anonymousId,
                            )
                        }
                    }
                }
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: IllegalStateException) {
                Result.failure(e)
            }
        }
    }
}
