package com.falsepattern.fpgradle.module.lombok

import com.falsepattern.fpgradle.getJarResource
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class ExtractLombokConfigTask: DefaultTask() {
    init {
        group = "falsepattern"
        description = "Extracts the `lombok.config` into the `/src` directory."
    }

    @TaskAction
    fun extractLombokConfig() {
        getJarResource("lombok.config")?.use { input ->
            val dir = project.rootDir.resolve("src").resolve("lombok.config")

            dir.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}