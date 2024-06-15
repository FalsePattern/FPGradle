package com.falsepattern.fpgradle.module.git

import com.falsepattern.fpgradle.getJarResource
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class ExtractGitAttributesTask: DefaultTask() {
    init {
        group = "falsepattern"
        description = "Extracts `.gitattributes` into the root directory."
    }

    @TaskAction
    fun extractGitAttributes() {
        getJarResource("_gitattributes")?.use { input ->
            val dir = project.rootDir.resolve(".gitattributes")

            dir.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    companion object {

    }
}