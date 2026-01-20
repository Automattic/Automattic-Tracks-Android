# NetworkLogger ProGuard Rules

# Keep public API
-keep public class com.automattic.android.tracks.networklogger.NetworkLogger {
    public *;
}

-keep public interface com.automattic.android.tracks.networklogger.NetworkLoggerConfig {
    *;
}

-keep public interface com.automattic.android.tracks.networklogger.EncryptedLoggingClient {
    *;
}

-keep public class com.automattic.android.tracks.networklogger.UploadResult {
    *;
}

-keep public class com.automattic.android.tracks.networklogger.UploadResult$* {
    *;
}

# Keep model classes (for JSON serialization)
-keep class com.automattic.android.tracks.networklogger.model.** { *; }

# OkHttp already has its own ProGuard rules, so we don't need to add anything for it
# Coroutines also have their own rules
