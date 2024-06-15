package com.falsepattern.fpgradle.module.git

import com.falsepattern.fpgradle.getJarResource
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class ExtractGitIgnoreTask: DefaultTask() {
    init {
        group = "falsepattern"
        description = "Extracts `.gitignore` into the root directory."
    }

    @TaskAction
    fun extractGitIgnore() {
        getJarResource("_gitignore")?.use { input ->
            val dir = project.rootDir.resolve(".gitignore")

            dir.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}