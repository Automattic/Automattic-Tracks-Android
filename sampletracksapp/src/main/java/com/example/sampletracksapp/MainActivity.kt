package com.example.sampletracksapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingOkHttpInterceptorProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingUser
import com.automattic.android.tracks.crashlogging.EventLevel
import com.automattic.android.tracks.crashlogging.ExtraKnownKey
import com.automattic.android.tracks.crashlogging.PerformanceMonitoringConfig
import com.automattic.android.tracks.crashlogging.RequestFormatter
import com.automattic.android.tracks.crashlogging.performance.PerformanceMonitoringRepositoryProvider
import com.automattic.android.tracks.crashlogging.performance.TransactionOperation
import com.automattic.android.tracks.crashlogging.performance.PerformanceTransactionRepository
import com.automattic.android.tracks.crashlogging.performance.TransactionStatus
import com.example.sampletracksapp.databinding.ActivityMainBinding
import com.example.sampletracksapp.performance.Track
import com.example.sampletracksapp.performance.TracksDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale

class MainActivity : AppCompatActivity() {

    val transactionRepository: PerformanceTransactionRepository = PerformanceMonitoringRepositoryProvider.createInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val crashLogging = CrashLoggingProvider.createInstance(
            application,
            object : CrashLoggingDataProvider {
                override val sentryDSN = BuildConfig.SENTRY_TEST_PROJECT_DSN
                override val buildType = BuildConfig.BUILD_TYPE
                override val releaseName = "test"
                override val locale = Locale.US
                override val enableCrashLoggingLogs = true
                override val performanceMonitoringConfig = PerformanceMonitoringConfig.Enabled(1.0)
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

            val db = Room.databaseBuilder(
                applicationContext,
                TracksDatabase::class.java, "database-name"
            ).build()

            val okHttp = OkHttpClient.Builder().addInterceptor(
                CrashLoggingOkHttpInterceptorProvider.createInstance(object : RequestFormatter {
                    override fun formatRequestUrl(request: Request): String {
                        return "Url formatted by RequestFormatter"
                    }
                })
            ).build()

            executePerformanceTransaction.setOnClickListener {

                val transactionId = transactionRepository.startTransaction("test name", TransactionOperation.UI_LOAD)

                GlobalScope.launch {
                    withContext(Dispatchers.IO) {
                        db.tracksDao().get()

                        val someData = okHttp.newCall(
                            Request.Builder()
                                .url("https://jsonplaceholder.typicode.com/posts/1")
                                .build()
                        ).execute().let {
                            it.body?.string().orEmpty()
                        }

                        db.tracksDao().insert(Track(someData))
                        transactionRepository.finishTransaction(transactionId, TransactionStatus.SUCCESSFUL)
                    }
                }
            }
        }
    }
}
