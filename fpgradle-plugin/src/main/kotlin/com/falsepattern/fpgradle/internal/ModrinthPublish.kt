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

import com.falsepattern.fpgradle.ext
import com.falsepattern.fpgradle.mc
import com.falsepattern.fpgradle.modrinth
import com.gtnewhorizons.retrofuturagradle.MinecraftExtension
import com.modrinth.minotaur.dependencies.DependencyType.REQUIRED
import com.modrinth.minotaur.dependencies.ModDependency
import org.gradle.kotlin.dsl.*

class ModrinthPublish(ctx: ConfigurationContext): InitTask {
    private val project = ctx.project

    override fun postInit() = with(project) {
        val projectId = mc.publish.modrinth.projectId
        val token = System.getenv(mc.publish.modrinth.tokenEnv.get())
        if (projectId.isPresent && token != null) {
            with(modrinth) {
                this.token = token
                this.projectId = projectId
                versionNumber = mc.mod.version
                versionType = mc.mod.version.map {
                    when {
                        it.contains("-a") -> "alpha"
                        it.contains("-b") -> "beta"
                        else -> "release"
                    }
                }
                changelog = mc.publish.changelog
                uploadFile.set(tasks.named("jar"))
                gameVersions.add(ext<MinecraftExtension>().mcVersion)
                loaders.add("forge")
                for (dep in mc.publish.modrinth.dependencies.get()) {
                    dependencies.add(dep())
                }
                if (mc.mixin.use)
                    dependencies.add(ModDependency("ghjoiQAl", REQUIRED))
            }
            tasks.named("modrinth").configure {
                dependsOn("build")
            }
            tasks.named("publish").configure {
                dependsOn("modrinth")
            }
        }
    }
}