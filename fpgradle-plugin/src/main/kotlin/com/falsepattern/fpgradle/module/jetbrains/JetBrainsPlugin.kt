package com.falsepattern.fpgradle.module.jetbrains

import com.falsepattern.fpgradle.FPPlugin
import com.falsepattern.fpgradle.ext
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.gradle.ext.IdeaExtPlugin

class JetBrainsPlugin: FPPlugin() {
    override fun addPlugins() = listOf(IdeaPlugin::class, IdeaExtPlugin::class)

    override fun onPluginInit(project: Project): Unit = with(project) {
        val idea = ext<IdeaModel>()
        idea.module {
            isDownloadSources = true
            isDownloadJavadoc = true
            inheritOutputDirs = true
        }
        tasks {
            named("processIdeaSettings").configure {
                dependsOn("injectTags", "setupDecompWorkspace")
            }
            named("ideVirtualMainClasses").configure {
                dependsOn("jar", "reobfJar")
            }
        }
    }

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