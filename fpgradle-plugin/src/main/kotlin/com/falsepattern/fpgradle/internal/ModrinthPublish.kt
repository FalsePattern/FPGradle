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

package com.falsepattern.fpgradle.internal

import com.falsepattern.fpgradle.FPPlugin
import com.falsepattern.fpgradle.mc
import com.falsepattern.fpgradle.minecraft
import com.falsepattern.fpgradle.modrinth
import com.gtnewhorizons.retrofuturagradle.mcp.ReobfuscatedJar
import com.modrinth.minotaur.Minotaur
import com.modrinth.minotaur.dependencies.DependencyType.REQUIRED
import com.modrinth.minotaur.dependencies.ModDependency
import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.named

class ModrinthPublish: FPPlugin() {

    override fun Project.addPlugins() = listOf(Minotaur::class)

    override fun Project.onPluginPostInitBeforeDeps() {
        val projectId = mc.publish.modrinth.projectId
        val token = mc.publish.modrinth.tokenEnv.map { System.getenv(it) }
        if (projectId.isPresent) {
            with(modrinth) {
                this.token = token.orElse("")
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
                uploadFile.set(tasks.named<ReobfuscatedJar>("reobfJar"))
                gameVersions.add(minecraft.mcVersion)
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
            if (token.isPresent) {
                tasks.named("publish").configure {
                    dependsOn("modrinth")
                }
            }
        }
    }
}