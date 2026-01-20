# Network Logger

A privacy-focused HTTP request/response logging library for Android that captures network traffic and uploads encrypted logs to Automattic's encrypted logging service.

## Features

- **OkHttp Interceptor**: Seamlessly integrates with existing OkHttp clients
- **Privacy-First**: Automatically redacts sensitive headers (Authorization, Cookie, etc.)
- **Daily Log Rotation**: Organizes logs by date with automatic handling of midnight boundaries
- **Smart Upload Strategy**:
  - Automatic: Uploads yesterday's logs on app start
  - Manual: On-demand snapshot upload of current logs
- **Configurable**: Control what gets logged, size limits, flush intervals, etc.
- **Efficient**: In-memory circular buffer with periodic disk flushing
- **Marker File Tracking**: Prevents duplicate uploads with reliable state tracking

## Motivation

We need a way to debug HTTP issues without:
- Relying on 3rd party services like Sentry (privacy concerns)
- Requiring users to attach logs to support tickets (manual and error-prone)
- Logging everything in application logs (noise and performance impact)

This library provides a browser DevTools-like experience where Happiness Engineers can retrieve network logs by searching for a username and date.

## Installation

Add the dependency to your app's `build.gradle`:

```gradle
dependencies {
    implementation 'com.automattic.tracks:networklogger:<version>'
    implementation 'com.automattic:encryptedlogging:<version>'
}
```

## Quick Start

### 1. Create an EncryptedLogging Client

First, implement the `EncryptedLoggingClient` interface using the EncryptedLogging library:

```kotlin
class MyEncryptedLoggingClient(
    private val encryptedLogging: EncryptedLogging
) : EncryptedLoggingClient {
    override suspend fun uploadLog(uploadId: String, file: File) {
        encryptedLogging.enqueueSendingEncryptedLogs(
            uuid = uploadId,
            file = file,
            shouldUploadImmediately = true
        )
    }
}
```

### 2. Create NetworkLogger

```kotlin
val networkLogger = NetworkLogger.create(
    context = applicationContext,
    username = currentUser.username, // or null for anonymous
    appName = "MyApp",
    appVersion = BuildConfig.VERSION_NAME,
    encryptedLoggingClient = myEncryptedLoggingClient,
    config = NetworkLoggerConfig.default()
)
```

### 3. Add Interceptor to OkHttp

```kotlin
val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(networkLogger.getInterceptor())
    .build()
```

### 4. Enable Logging & Upload

```kotlin
// In Application.onCreate()
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Enable logging (can be controlled by settings toggle or feature flag)
        if (shouldEnableNetworkLogging()) {
            networkLogger.enable()
        }

        // Upload yesterday's logs on app start
        networkLogger.uploadYesterdayLogs()
    }
}
```

### 5. Manual Upload (Optional)

Add a button in settings to let users upload current logs:

```kotlin
// In Settings screen
lifecycleScope.launch {
    val result = networkLogger.uploadCurrentLogs()
    when (result) {
        is UploadResult.Success -> {
            showToast("Logs uploaded: ${result.uploadId}")
        }
        is UploadResult.Failure -> {
            showToast("Upload failed: ${result.error}")
        }
    }
}
```

## Configuration

Customize logging behavior using `NetworkLoggerConfig`:

```kotlin
val config = object : NetworkLoggerConfig {
    // Only log specific hosts
    override fun shouldLogRequest(request: Request): Boolean {
        return request.url.host.contains("api.example.com")
    }

    // Add custom sensitive headers
    override fun sensitiveHeaders(): Set<String> {
        return setOf(
            "authorization",
            "x-api-key",
            "x-custom-secret"
        )
    }

    // Increase body size limit to 2MB
    override fun maxBodySize(): Long = 2_097_152

    // Increase buffer size
    override fun maxBufferSize(): Int = 200

    // Flush every 10 minutes instead of 5
    override fun flushIntervalMs(): Long = 600_000
}

val networkLogger = NetworkLogger.create(
    context = context,
    // ... other params
    config = config
)
```

## Log Retrieval

### Upload Identifier Format

Logs are uploaded with the identifier: `username-YYYY-MM-DD`

Examples:
- `matt-2026-01-20` (logged-in user)
- `anonymous-2026-01-20` (anonymous user)

### Workflow

1. **User enables logging** (via settings toggle or feature flag)
2. **Requests are captured** throughout the day
3. **On app start**: Yesterday's logs uploaded automatically
4. **HE asks for username**: "What's your username?"
5. **HE searches**: In encrypted-logging UI, search for `matt-2026-01-20`
6. **Download & debug**: HE gets decrypted JSON with all network activity

### Log Format

```json
{
  "metadata": {
    "username": "matt",
    "app_name": "MyApp",
    "app_version": "1.2.3",
    "platform": "android",
    "device_model": "Google Pixel 7",
    "os_version": "14",
    "date": "2026-01-20"
  },
  "requests": [
    {
      "timestamp": "2026-01-20T10:05:23.123Z",
      "method": "POST",
      "url": "https://api.example.com/checkout",
      "request_headers": {
        "Content-Type": "application/json",
        "Authorization": "[REDACTED]"
      },
      "request_body": "{\"product_id\":\"123\"}",
      "response_code": 200,
      "response_headers": {
        "Content-Type": "application/json"
      },
      "response_body": "{\"status\":\"success\"}",
      "duration_ms": 1234,
      "error": null
    }
  ]
}
```

## Privacy & Security

### Automatic Redaction

The following headers are automatically redacted:
- `authorization`
- `cookie`
- `set-cookie`
- `x-auth-token`
- `x-api-key`
- `api-key`

Add custom sensitive headers via `NetworkLoggerConfig.sensitiveHeaders()`.

### Body Logging Rules

- **Binary content skipped**: Images, videos, audio, octet-stream
- **Size limit enforced**: Default 1MB (configurable)
- **Truncation**: Bodies exceeding limit are truncated with `[TRUNCATED]` marker

### Storage

- Logs stored in app's **cache directory** (automatically managed by OS)
- Files named: `network_logs_YYYY-MM-DD.json`
- Marker files track upload state: `network_logs_YYYY-MM-DD.uploaded`
- Successfully uploaded files are deleted automatically

## Feature Flags / Username Filtering

To enable logging only for specific users:

```kotlin
// Get enabled usernames from remote config or ExPlat
val enabledUsernames = remoteConfig.getStringSet("network_logging_enabled_users")

if (currentUser.username in enabledUsernames) {
    networkLogger.enable()
}
```

## Architecture

```
┌─────────────────────────────────────────────────┐
│              OkHttp Client                      │
└───────────────┬─────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────────┐
│      NetworkLoggerInterceptor                   │
│  (Captures requests/responses)                  │
└───────────────┬─────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────────┐
│           LogSanitizer                          │
│  (Redacts sensitive data)                       │
└───────────────┬─────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────────┐
│           LogBuffer                             │
│  (In-memory circular buffer)                    │
└───────────────┬─────────────────────────────────┘
                │ (Periodic flush every 5 min)
                ▼
┌─────────────────────────────────────────────────┐
│           LogFileWriter                         │
│  (Writes to daily JSON files)                   │
└───────────────┬─────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────────┐
│           LogUploader                           │
│  (Marker file tracking)                         │
└───────────────┬─────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────────┐
│      EncryptedLogging Library                   │
│  (Encrypts & uploads to WordPress.com)          │
└─────────────────────────────────────────────────┘
```

## Testing

Run unit tests:

```bash
./gradlew :networklogger:test
```

## Future Enhancements

- Performance metrics dashboard (average duration, error rates)
- More granular filtering (per-endpoint logging)
- Compression for large logs
- Configurable retention periods
- GraphQL query logging support

## License

Same as parent project (GPL-2.0)
