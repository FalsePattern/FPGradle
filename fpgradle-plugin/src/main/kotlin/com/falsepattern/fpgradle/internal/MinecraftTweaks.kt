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

import com.falsepattern.fpgradle.*
import com.falsepattern.fpgradle.internal.Stubs.Companion.JAR_STUB_TASK
import com.falsepattern.jtweaker.RemoveStubsJar
import com.gtnewhorizons.retrofuturagradle.mcp.InjectTagsTask
import com.gtnewhorizons.retrofuturagradle.mcp.ReobfuscatedJar
import com.gtnewhorizons.retrofuturagradle.minecraft.RunMinecraftTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.expand
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import org.gradle.language.jvm.tasks.ProcessResources

class MinecraftTweaks: FPPlugin() {

    override fun Project.onPluginInit() {
        jar(this)
    }

    override fun Project.onPluginPostInitAfterDeps() {
        validate()
    }

    private fun Project.validate() {
        if (mc.mod.rootPkg.isPresent)
            verifyPackage("", "mod -> rootPkg", false)
    }

    private fun jar(project: Project) {
        val mcMod = project.mc.mod
        val mcTokens = project.mc.tokens
        val minecraft = project.minecraft
        project.tasks {
            named<ProcessResources>("processResources").configure {
                val minecraftVersion = minecraft.mcVersion
                val modVersion = mcMod.version
                val modId = mcMod.modid
                val modName = mcMod.name
                inputs.property("minecraftVersion", minecraftVersion)
                inputs.property("modVersion", modVersion)
                inputs.property("modId", modId)
                inputs.property("modName", modName)
                filesMatching("mcmod.info") {
                    expand(
                        "minecraftVersion" to minecraftVersion.get(),
                        "modVersion" to modVersion.get(),
                        "modId" to modId.get(),
                        "modName" to modName.get()
                    )
                }
                filesMatching("META-INF/rfb-plugin/*") {
                    expand(
                        "modVersion" to modVersion.get(),
                        "modName" to modName.get()
                    )
                }
            }

            minecraft.injectedTags.putAll(project.provider {
                if (mcTokens.tokenClass.isPresent) {
                    val result = HashMap<String, String>()

                    if (mcTokens.modid.isPresent)
                        result[mcTokens.modid.get()] = mcMod.modid.get()

                    if (mcTokens.name.isPresent)
                        result[mcTokens.name.get()] = mcMod.name.get()

                    if (mcTokens.version.isPresent)
                        result[mcTokens.version.get()] = mcMod.version.get()

                    if (mcTokens.rootPkg.isPresent)
                        result[mcTokens.rootPkg.get()] = mcMod.rootPkg.get()

                    result
                } else mapOf()
            })

            named<InjectTagsTask>("injectTags").configure {
                inputs.property("tokenId", mcTokens.modid)
                inputs.property("tokenName", mcTokens.name)
                inputs.property("tokenVersion", mcTokens.version)
                inputs.property("tokenGroup", mcTokens.rootPkg)
                if (mcTokens.tokenClass.isPresent) {
                    inputs.property("tokenClass", mcTokens.tokenClass.get())
                    if (mcTokens.tokenClassIgnoreRootPkg.get()) {
                        outputClassName = mcTokens.tokenClass
                    } else {
                        outputClassName = mcTokens.tokenClass.map { "${mcMod.rootPkg.get()}.$it" }
                    }
                }
                val tokenClass = mcTokens.tokenClass
                onlyIf {
                    tokenClass.isPresent
                }
            }

            named<Jar>("jar").configure {
                manifest {
                    attributes(project.manifestAttributes.get())
                }
            }

            project.base.archivesName = project.mc.publish.maven.artifact

            val jar = project.tasks.named<Jar>("jar")
            val jarRemoveStub = project.tasks.named<RemoveStubsJar>(JAR_STUB_TASK)
            project.tasks.named<ReobfuscatedJar>("reobfJar") {
                @Suppress("UNCHECKED_CAST")
                setInputJarFromTask(jarRemoveStub as TaskProvider<Jar>)
            }
            withType<RunMinecraftTask> {
                if (McRun.standardNonObf().any { it.taskName == this@withType.name } or
                    McRun.modern().any { it.taskName == this@withType.name }) {
                    val oldClasspath = classpath
                    setClasspath(oldClasspath.minus(project.files().from( jar)))
                    classpath(jarRemoveStub)
                }
            }
        }
    }
}
