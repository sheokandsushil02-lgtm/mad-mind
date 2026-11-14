plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}

// MAD self-diagnosis: when the build fails, write the real failure
// (task + cause chain) into GitHub Actions annotations so it can be
// read remotely via the check-run annotations API.
@Suppress("DEPRECATION")
gradle.buildFinished { result ->
    result.failure?.let { f ->
        println("::error::MAD-BUILD-FAILED: " + (f.message ?: f.javaClass.name).take(500))
        var cause: Throwable? = f.cause
        var depth = 0
        while (cause != null && depth < 6) {
            println("::error::MAD-CAUSE-" + depth + ": " + (cause.message ?: cause.javaClass.name).take(500))
            cause = cause.cause
            depth++
        }
    }
}
