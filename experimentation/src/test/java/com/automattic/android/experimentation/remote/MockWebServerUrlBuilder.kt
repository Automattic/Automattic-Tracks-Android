package com.automattic.android.experimentation.remote

import okhttp3.HttpUrl
import okhttp3.mockwebserver.MockWebServer

internal open class MockWebServerUrlBuilder(
    private val exPlatUrlBuilder: ExPlatUrlBuilder,
    private val server: MockWebServer,
) : UrlBuilder {
    override fun buildUrl(
        platform: String,
        experimentNames: List<String>,
        anonymousId: String?,
    ): HttpUrl {
        return exPlatUrlBuilder.buildUrl(
            platform,
            experimentNames,
            anonymousId,
        ).newBuilder()
            .scheme("http")
            .host(server.url("/").host)
            .port(server.url("/").port)
            .build()
    }
}

internal class UnknownHostMockWebServerUrlBuilder(
    exPlatUrlBuilder: ExPlatUrlBuilder,
    server: MockWebServer,
) : MockWebServerUrlBuilder(exPlatUrlBuilder, server) {
    override fun buildUrl(
        platform: String,
        experimentNames: List<String>,
        anonymousId: String?,
    ): HttpUrl {
        return super.buildUrl(platform, experimentNames, anonymousId).newBuilder()
            .host("unknownhost")
            .build()
    }
}
