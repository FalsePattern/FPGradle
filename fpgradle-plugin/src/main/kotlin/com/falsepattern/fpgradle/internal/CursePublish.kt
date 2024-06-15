/*
 * FPGradle
 *
 * Copyright (C) 2024 FalsePattern
 * All Rights Reserved
 *
 * The above copyright notice and this permission notice
 * shall be included in all copies or substantial portions of the Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.falsepattern.fpgradle.internal

import com.falsepattern.fpgradle.FPPlugin
import com.falsepattern.fpgradle.*
import com.gtnewhorizons.retrofuturagradle.MinecraftExtension
import com.matthewprenger.cursegradle.CurseGradlePlugin
import org.gradle.api.Project

class CursePublish: FPPlugin() {
    override fun addPlugins() = listOf(CurseGradlePlugin::class)

    override fun Project.onPluginPostInitBeforeDeps() {
        val projectId = mc.publish.curseforge.projectId
        val token = mc.publish.curseforge.tokenEnv.map { System.getenv(it) }
        if (projectId.isPresent) {
            with(curseforge) {
                apiKey = token.getOrElse("")
                project {
                    id = projectId.get()
                    changelogType = "markdown"
                    changelog = mc.publish.changelog
                    val version = mc.mod.version.get()
                    releaseType = when {
                        version.contains("-a") -> "alpha"
                        version.contains("-b") -> "beta"
                        else -> "release"
                    }
                    addGameVersion(minecraft.mcVersion)
                    addGameVersion("Forge")
                    mainArtifact(tasks.named("jar")) {
                        displayName = mc.mod.version.get()
                    }

                    for (relation in mc.publish.curseforge.relations.get())
                        relations(relation)

                    if (mc.mixin.use) relations {
                        requiredDependency("unimixins")
                    }
                }
                options {
                    javaIntegration = false
                    forgeGradleIntegration = false
                }
            }
            tasks.named("curseforge").configure {
                dependsOn("build")
            }
            if (token.isPresent) {
                tasks.named("publish").configure {
                    dependsOn("curseforge")
                }
            }
        }
    }
}