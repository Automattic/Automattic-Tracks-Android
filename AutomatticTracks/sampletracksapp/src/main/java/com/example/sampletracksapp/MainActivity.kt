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
                override val sentryDSN: String
                    get() = TODO("Not yet implemented")
                override val userHasOptedOut: Boolean
                    get() = TODO("Not yet implemented")
                override val buildType: String
                    get() = TODO("Not yet implemented")
                override val releaseName: String
                    get() = TODO("Not yet implemented")
                override val currentUser: TracksUser?
                    get() = TODO("Not yet implemented")
                override val applicationContext: Map<String, String?>
                    get() = TODO("Not yet implemented")
                override val userContext: Map<String, String?>
                    get() = TODO("Not yet implemented")
                override val locale: Locale?
                    get() = TODO("Not yet implemented")
                override val enableCrashLoggingLogs: Boolean
                    get() = TODO("Not yet implemented")
            }
        )
    }
}
