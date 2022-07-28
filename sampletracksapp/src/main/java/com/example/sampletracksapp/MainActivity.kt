package com.example.sampletracksapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingUser
import com.automattic.android.tracks.crashlogging.EventLevel
import com.automattic.android.tracks.crashlogging.ExtraKnownKey
import com.example.sampletracksapp.databinding.ActivityMainBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.flowOf
import java.util.Locale

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val crashLogging = CrashLoggingProvider.createInstance(
            this,
            object : CrashLoggingDataProvider {
                override val sentryDSN = BuildConfig.SENTRY_TEST_PROJECT_DSN
                override val buildType = BuildConfig.BUILD_TYPE
                override val releaseName = "test"
                override val locale = Locale.US
                override val enableCrashLoggingLogs = true
                override val user = flowOf(
                    CrashLoggingUser(
                        userID = "test user id",
                        email = "test@user.com",
                        username = "test username"
                    )
                )
                override val applicationContextProvider =
                    flowOf(mapOf("extra" to "application context"))

                override fun shouldDropWrappingException(
                    module: String,
                    type: String,
                    value: String
                ): Boolean {
                    return false
                }

                override fun crashLoggingEnabled(): Boolean {
                    return true
                }

                override fun extraKnownKeys(): List<String> {
                    return emptyList()
                }

                override fun provideExtrasForEvent(
                    currentExtras: Map<ExtraKnownKey, String>,
                    eventLevel: EventLevel
                ): Map<ExtraKnownKey, String> {
                    return mapOf("extra" to "event value")
                }
            },
            appScope = GlobalScope
        )

        ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(root)

            sendReportWithMessage.setOnClickListener {
                crashLogging.sendReport(message = "Message from Tracks test app")
            }

            sendReportWithException.setOnClickListener {
                crashLogging.sendReport(exception = Exception("Exception from Tracks test app"))
            }

            recordBreadcrumbWithMessage.setOnClickListener {
                crashLogging.recordEvent(
                    message = "Custom breadcrumb",
                    category = "Custom category"
                )
            }

            recordBreadcrumbWithException.setOnClickListener {
                crashLogging.recordException(
                    exception = NullPointerException(),
                    category = "Custom exception category"
                )
            }
        }
    }
}
