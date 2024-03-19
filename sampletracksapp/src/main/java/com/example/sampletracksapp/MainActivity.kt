package com.example.sampletracksapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingOkHttpInterceptorProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingUser
import com.automattic.android.tracks.crashlogging.EventLevel
import com.automattic.android.tracks.crashlogging.ExtraKnownKey
import com.automattic.android.tracks.crashlogging.JsException
import com.automattic.android.tracks.crashlogging.JsExceptionCallback
import com.automattic.android.tracks.crashlogging.JsExceptionStackTraceElement
import com.automattic.android.tracks.crashlogging.PerformanceMonitoringConfig
import com.automattic.android.tracks.crashlogging.RequestFormatter
import com.automattic.android.tracks.crashlogging.performance.PerformanceMonitoringRepositoryProvider
import com.automattic.android.tracks.crashlogging.performance.PerformanceTransactionRepository
import com.automattic.android.tracks.crashlogging.performance.TransactionOperation
import com.automattic.android.tracks.crashlogging.performance.TransactionStatus
import com.example.sampletracksapp.databinding.ActivityMainBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.flowOf
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.Locale

class MainActivity : AppCompatActivity() {
    val transactionRepository: PerformanceTransactionRepository =
        PerformanceMonitoringRepositoryProvider.createInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val crashLogging = CrashLoggingProvider.createInstance(
            application,
            object : CrashLoggingDataProvider {
                override val sentryDSN = BuildConfig.SENTRY_TEST_PROJECT_DSN
                override val buildType = BuildConfig.BUILD_TYPE
                override val locale = Locale.US
                override val enableCrashLoggingLogs = true
                override val performanceMonitoringConfig = PerformanceMonitoringConfig.Enabled(sampleRate = 1.0, profilesSampleRate = 1.0)
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

            sendReportWithJavaScriptException.setOnClickListener {
                val callback = object : JsExceptionCallback {
                    override fun onReportSent(sent: Boolean) {
                        Log.d("JsExceptionCallback", "onReportSent: $sent")
                    }
                }
                val jsException = JsException(
                    type = "Error",
                    message = "JavaScript exception from Tracks test app",
                    stackTrace = listOf(
                        JsExceptionStackTraceElement(
                            fileName = "file.js",
                            lineNumber = 1,
                            colNumber = 1,
                            function = "function"
                        )
                    ),
                    context = mapOf("context" to "value"),
                    tags = mapOf("tag" to "SomeTag"),
                    isHandled = true,
                    handledBy = "SomeHandler"
                )
                crashLogging.sendJavaScriptReport(jsException, callback)
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

            val okHttp = OkHttpClient.Builder().addInterceptor(
                CrashLoggingOkHttpInterceptorProvider.createInstance(object : RequestFormatter {
                    override fun formatRequestUrl(request: Request): String {
                        return "Url formatted by RequestFormatter"
                    }
                })
            ).build()

            executePerformanceTransaction.setOnClickListener {
                val transactionId = transactionRepository.startTransaction(
                    "test name",
                    TransactionOperation.UI_LOAD
                )

                okHttp.newCall(
                    Request.Builder()
                        .url("https://jsonplaceholder.typicode.com/posts/1")
                        .build()
                ).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        transactionRepository.finishTransaction(
                            transactionId,
                            TransactionStatus.ABORTED
                        )
                    }

                    override fun onResponse(call: Call, response: Response) {
                        transactionRepository.finishTransaction(
                            transactionId,
                            TransactionStatus.SUCCESSFUL
                        )
                    }
                })
            }
        }
    }
}
