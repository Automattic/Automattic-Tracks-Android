package com.example.sampletracksapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.automattic.android.tracks.crashlogging.CrashLoggingOkHttpInterceptorProvider
import com.automattic.android.tracks.crashlogging.JsException
import com.automattic.android.tracks.crashlogging.JsExceptionCallback
import com.automattic.android.tracks.crashlogging.JsExceptionStackTraceElement
import com.automattic.android.tracks.crashlogging.RequestFormatter
import com.automattic.android.tracks.crashlogging.performance.PerformanceMonitoringRepositoryProvider
import com.automattic.android.tracks.crashlogging.performance.PerformanceTransactionRepository
import com.automattic.android.tracks.crashlogging.performance.TransactionOperation
import com.automattic.android.tracks.crashlogging.performance.TransactionStatus
import com.example.sampletracksapp.databinding.ActivityMainBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeoutException

class MainActivity : AppCompatActivity() {
    val transactionRepository: PerformanceTransactionRepository =
        PerformanceMonitoringRepositoryProvider.createInstance()

    val crashLogging: CrashLogging
        get() = (application as SampleApp).crashLogging

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                            function = "function",
                        ),
                    ),
                    context = mapOf("context" to "value"),
                    tags = mapOf("tag" to "SomeTag"),
                    isHandled = true,
                    handledBy = "SomeHandler",
                )
                crashLogging.sendJavaScriptReport(jsException, callback)
            }

            recordBreadcrumbWithMessage.setOnClickListener {
                crashLogging.recordEvent(
                    message = "Custom breadcrumb",
                    category = "Custom category",
                )
            }

            recordBreadcrumbWithException.setOnClickListener {
                crashLogging.recordException(
                    exception = NullPointerException(),
                    category = "Custom exception category",
                )
            }

            val okHttp = OkHttpClient.Builder().addInterceptor(
                CrashLoggingOkHttpInterceptorProvider.createInstance(object : RequestFormatter {
                    override fun formatRequestUrl(request: Request): String {
                        return "Url formatted by RequestFormatter"
                    }
                }),
            ).build()

            executePerformanceTransaction.setOnClickListener {
                val transactionId = transactionRepository.startTransaction(
                    "test name",
                    TransactionOperation.UI_LOAD,
                )

                okHttp.newCall(
                    Request.Builder()
                        .url("https://jsonplaceholder.typicode.com/posts/1")
                        .build(),
                ).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        transactionRepository.finishTransaction(
                            transactionId,
                            TransactionStatus.ABORTED,
                        )
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val spanId = transactionRepository.startSpan(
                            transactionId,
                            "test span",
                            "test operation",
                        )

                        Thread.sleep(1000)

                        transactionRepository.finishSpan(spanId, 408, TimeoutException())

                        transactionRepository.finishTransaction(
                            transactionId,
                            TransactionStatus.SUCCESSFUL,
                        )
                    }
                })
            }

            openExperimentation.setOnClickListener {
                ExperimentationDialogFragment().show(supportFragmentManager, "ExperimentationDialogFragment")
            }

            forceAnr.setOnClickListener {
                Thread.sleep(7000)
            }
        }
    }
}
