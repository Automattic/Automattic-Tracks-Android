package com.example.sampletracksapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingUser
import com.automattic.android.tracks.crashlogging.EventLevel
import com.automattic.android.tracks.crashlogging.ExtraKnownKey
import com.example.sampletracksapp.databinding.ActivityMainBinding
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
                override fun shouldDropWrappingException(
                    module: String,
                    type: String,
                    value: String
                ): Boolean {
                    return false
                }

                override fun userProvider(): CrashLoggingUser {
                    return CrashLoggingUser(
                        userID = "test user id",
                        email = "test@user.com",
                        username = "test username"
                    )
                }

                override fun userHasOptOutProvider(): Boolean {
                    return false
                }

                override fun extraKnownKeys(): List<String> {
                    return emptyList()
                }

                override fun provideExtrasForEvent(
                    currentExtras: Map<ExtraKnownKey, String>,
                    eventLevel: EventLevel
                ): Map<ExtraKnownKey, String> {
                    return mapOf("extra" to "value")
                }
            }
        )

        ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(root)

            logMessage.setOnClickListener {
                crashLogging.log("Message from Tracks test app")
            }

            logException.setOnClickListener {
                crashLogging.log(Exception("Exception from Tracks test app"))
            }

            logExceptionWithExtra.setOnClickListener {
                crashLogging.log(
                    throwable = Exception("Exception from Tracks test app with extra data"),
                    data = mapOf("extra" to "data bundled with exception")
                )
            }
        }
    }
}
