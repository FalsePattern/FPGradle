/*
 * FPGradle
 *
 * Copyright (C) 2024-2025 FalsePattern
 * All Rights Reserved
 *
 * The above copyright notice and this permission notice
 * shall be included in all copies or substantial portions of the Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.falsepattern.fpgradle.module.jetbrains

import com.falsepattern.fpgradle.FPPlugin
import com.falsepattern.fpgradle.idea
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.jetbrains.gradle.ext.IdeaExtPlugin

class JetBrainsPlugin: FPPlugin() {
    override fun Project.addPlugins() = listOf(IdeaPlugin::class, IdeaExtPlugin::class)

    override fun Project.onPluginInit() {
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

    override fun Project.onPluginPostInitBeforeDeps() {
        noCachingOnRunButton()
    }

    private fun Project.noCachingOnRunButton() {
        tasks {
            withType<JavaExec>().configureEach {
                if (name.endsWith("main()"))
                    markNotCompatible()
            }
        }
    }

    companion object {
        private fun Task.markNotCompatible() {
            notCompatibleWithConfigurationCache("""
                |Work around for: https://github.com/gradle/gradle/issues/21364
                |Caching issue when main() is called directly from IntelliJ
            """.trimMargin())
        }
    }
}