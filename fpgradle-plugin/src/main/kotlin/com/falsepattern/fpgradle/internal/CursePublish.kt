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

import com.falsepattern.fpgradle.curseforge
import com.falsepattern.fpgradle.ext
import com.falsepattern.fpgradle.mc
import com.gtnewhorizons.retrofuturagradle.MinecraftExtension

class CursePublish(ctx: ConfigurationContext): InitTask {
    private val project = ctx.project

    override fun postInit() = with(project) {
        val projectId = mc.publish.curseforge.projectId
        val token = System.getenv(mc.publish.curseforge.tokenEnv.get())
        if (projectId.isPresent && token != null) {
            with(curseforge) {
                apiKey = token
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
                    addGameVersion(ext<MinecraftExtension>().mcVersion)
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
            tasks.named("publish").configure {
                dependsOn("curseforge")
            }
        }
    }
}