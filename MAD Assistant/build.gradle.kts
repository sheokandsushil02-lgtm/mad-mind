plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}

// MAD self-diagnosis: on CI, surface the real failing task and error
// chain as GitHub annotations (::error::) so failures can be read
// remotely. Uses only stable TaskExecutionListener API.
gradle.addListener(object : org.gradle.api.execution.TaskExecutionListener {
    override fun beforeExecute(task: org.gradle.api.Task) {}
    override fun afterExecute(task: org.gradle.api.Task, state: org.gradle.api.tasks.TaskState) {
        val failure = state.failure ?: return
        println("::error::MAD TASK FAILED [" + task.path + "]: " + (failure.message ?: failure.toString()).take(600))
        var cause: Throwable? = failure.cause
        var depth = 0
        while (cause != null && depth < 6) {
            println("::error::MAD CAUSE " + depth + ": " + (cause.message ?: cause.toString()).take(600))
            cause = cause.cause
            depth++
        }
    }
})
