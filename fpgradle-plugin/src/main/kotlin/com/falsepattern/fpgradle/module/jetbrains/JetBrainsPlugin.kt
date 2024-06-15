package com.falsepattern.fpgradle.module.jetbrains

import com.falsepattern.fpgradle.FPPlugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.ide.idea.IdeaPlugin

class JetBrainsPlugin: FPPlugin() {
    override fun addPlugins() = listOf(IdeaPlugin::class)

    override fun onPluginPostInit(project: Project) {
        noCachingOnRunButton(project)
    }

    private fun noCachingOnRunButton(project: Project) = with(project) {
        tasks {
            withType<JavaExec>().configureEach {
                if (name.endsWith("main()"))
                    markTaskNotCompatible(this)
            }
        }
    }

    companion object {
        private fun markTaskNotCompatible(task: Task) = with(task) {
            notCompatibleWithConfigurationCache("""
                |Work around for: https://github.com/gradle/gradle/issues/21364
                |Caching issue when main() is called directly from IntelliJ
            """.trimMargin())
        }
    }
}