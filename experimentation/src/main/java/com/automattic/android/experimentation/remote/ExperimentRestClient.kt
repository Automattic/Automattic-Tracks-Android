package com.automattic.android.experimentation.remote

import com.automattic.android.experimentation.domain.Assignments
import com.automattic.android.experimentation.domain.Clock
import com.automattic.android.experimentation.domain.SystemClock
import com.automattic.android.experimentation.remote.AssignmentsDtoMapper.toAssignments
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

internal class ExperimentRestClient(
    private val okHttpClient: OkHttpClient = OkHttpClient(),
    private val moshi: Moshi = Moshi.Builder().build(),
    private val jsonAdapter: AssignmentsDtoJsonAdapter = AssignmentsDtoJsonAdapter(moshi),
    private val urlBuilder: UrlBuilder = ExPlatUrlBuilder(),
    private val clock: Clock = SystemClock(),
) {

    suspend fun fetchAssignments(
        platform: String,
        experimentNames: List<String>,
        anonymousId: String? = null,
    ): Result<Assignments> {
        val url = urlBuilder.buildUrl(platform, experimentNames, anonymousId)

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Result.failure(IOException("Unexpected code $response"))
                } else {
                    runCatching {
                        val dto = jsonAdapter.fromJson(response.body!!.source())!!
                        dto.toAssignments(clock.currentTimeSeconds())
                    }
                }
            }
        }
    }
}
