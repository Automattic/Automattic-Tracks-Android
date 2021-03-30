package com.example.sampletracksapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.automattic.android.tracks.TracksUser
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import java.util.Locale

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        CrashLogging.start(
                this,
                object : CrashLoggingDataProvider {
                    override val sentryDSN = BuildConfig.SENTRY_TEST_PROJECT_DSN
                    override val userHasOptedOut = false
                    override val buildType = BuildConfig.BUILD_TYPE
                    override val releaseName = BuildConfig.VERSION_NAME
                    override val currentUser = TracksUser(userID = "test user id", email = "test@user.com", username = "test username")
                    override val applicationContext = mapOf("sample" to "application context value", "empty" to null)
                    override val userContext = mapOf("sample" to "user context value", "empty" to null)
                    override val locale = Locale.US
                    override val enableCrashLoggingLogs = true
                }
        )
    }
}
