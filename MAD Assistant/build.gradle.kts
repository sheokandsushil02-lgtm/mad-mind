import org.gradle.api.initialization.Settings

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}

// Diagnostic: surface Android SDK/AGP configuration issues clearly in CI logs
gradle.settingsEvaluated {
    println("=== MAD-BUILD-DIAG settings evaluated ===")
    println("ANDROID_HOME=" + System.getenv("ANDROID_HOME"))
    println("ANDROID_SDK_ROOT=" + System.getenv("ANDROID_SDK_ROOT"))
}
