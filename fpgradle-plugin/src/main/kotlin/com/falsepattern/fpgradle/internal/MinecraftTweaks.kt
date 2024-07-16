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

import com.falsepattern.fpgradle.*
import com.gtnewhorizons.retrofuturagradle.mcp.InjectTagsTask
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.expand
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named
import org.gradle.language.jvm.tasks.ProcessResources
import kotlin.collections.HashMap
import kotlin.collections.mapOf
import kotlin.collections.set

class MinecraftTweaks: FPPlugin() {

    override fun Project.onPluginInit() {
        jar()
    }

    override fun Project.onPluginPostInitAfterDeps() {
        validate()
    }

    private fun Project.validate() {
        if (mc.mod.rootPkg.isPresent)
            verifyPackage("", "mod -> rootPkg", false)
    }

    private fun Project.jar() {

        tasks {
            named<ProcessResources>("processResources").configure {
                inputs.property("version", mc.mod.version)
                inputs.property("mcversion", minecraft.mcVersion)
                filesMatching("mcmod.info") {
                    expand(
                        "minecraftVersion" to minecraft.mcVersion.get(),
                        "modVersion" to mc.mod.version.get(),
                        "modId" to mc.mod.modid.get(),
                        "modName" to mc.mod.name.get()
                    )
                }
                filesMatching("META-INF/rfb-plugin/*") {
                    expand(
                        "modVersion" to mc.mod.version.get(),
                        "modName" to mc.mod.name.get()
                    )
                }
            }

            minecraft.injectedTags.putAll(provider {
                if (mc.tokens.tokenClass.isPresent) {
                    val result = HashMap<String, String>()

                    if (mc.tokens.modid.isPresent)
                        result[mc.tokens.modid.get()] = mc.mod.modid.get()

                    if (mc.tokens.name.isPresent)
                        result[mc.tokens.name.get()] = mc.mod.name.get()

                    if (mc.tokens.version.isPresent)
                        result[mc.tokens.version.get()] = mc.mod.version.get()

                    if (mc.tokens.rootPkg.isPresent)
                        result[mc.tokens.rootPkg.get()] = mc.mod.rootPkg.get()

                    result
                } else mapOf()
            })

            named<InjectTagsTask>("injectTags").configure {
                inputs.property("tokenId", mc.tokens.modid)
                inputs.property("tokenName", mc.tokens.name)
                inputs.property("tokenVersion", mc.tokens.version)
                inputs.property("tokenGroup", mc.tokens.rootPkg)
                if (mc.tokens.tokenClass.isPresent) {
                    inputs.property("tokenClass", mc.tokens.tokenClass.get())
                    if (mc.tokens.tokenClassIgnoreRootPkg.get()) {
                        outputClassName = mc.tokens.tokenClass
                    } else {
                        outputClassName = mc.tokens.tokenClass.map { "${mc.mod.rootPkg.get()}.$it" }
                    }
                }
                onlyIf {
                    mc.tokens.tokenClass.isPresent
                }
            }

            named<Jar>("jar").configure {
                manifest {
                    attributes(manifestAttributes.get())
                }
            }

            base.archivesName = mc.publish.maven.artifact
        }
    }
}
