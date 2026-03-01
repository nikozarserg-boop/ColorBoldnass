// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
}

tasks.register("syncProject") {
    group = "custom"
    description = "Task for synchronization (placeholder)"
    doLast {
        println("Sync task executed. Note: IDE sync is separate from Gradle tasks.")
    }
}

tasks.register("buildProject") {
    group = "custom"
    description = "Custom build task"
    dependsOn(":app:assembleDebug")
    doLast {
        println("Build task finished.")
    }
}
