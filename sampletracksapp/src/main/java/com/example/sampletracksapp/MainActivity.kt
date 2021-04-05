package com.example.sampletracksapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.automattic.android.tracks.TracksUser
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.example.sampletracksapp.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CrashLogging.start(
            this,
            object : CrashLoggingDataProvider {
                override val sentryDSN = BuildConfig.SENTRY_TEST_PROJECT_DSN
                override val userHasOptedOut = false
                override val buildType = BuildConfig.BUILD_TYPE
                override val releaseName = "test"
                override val currentUser = TracksUser(
                    userID = "test user id",
                    email = "test@user.com",
                    username = "test username"
                )
                override val applicationContext = mapOf("sample" to "application context value")
                override val userContext = mapOf("sample" to "user context value")
                override val locale = Locale.US
                override val enableCrashLoggingLogs = true
            }
        )

        ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(root)

            logMessage.setOnClickListener {
                CrashLogging.log("Message from Tracks test app")
            }

            logException.setOnClickListener {
                CrashLogging.log(Exception("Exception from Tracks test app"))
            }

            logExceptionWithExtra.setOnClickListener {
                CrashLogging.log(
                    throwable = Exception("Exception from Tracks test app with extra data"),
                    data = mapOf("extra" to "data bundled with exception")
                )
            }
        }
    }
}
