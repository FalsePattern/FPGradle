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
import com.gtnewhorizons.retrofuturagradle.mcp.DeobfuscateTask
import com.gtnewhorizons.retrofuturagradle.minecraft.RunMinecraftTask
import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named
import kotlin.collections.HashMap
import kotlin.collections.set

class FMLTweaks: FPPlugin() {
    override fun Project.onPluginInit() {
        coremodInit()
        accessTransformerInit()
    }

    override fun Project.onPluginPostInitBeforeDeps() {
        coreModPostInit()
        accessTransformerPostInit()
        runArgs()
    }

    private fun Project.runArgs() {
        tasks.named<RunMinecraftTask>("runClient") {
            username = mc.run.username.get()
            if (mc.run.userUUID.isPresent)
                userUUID = mc.run.userUUID.get().toString()
        }
    }

    private fun Project.coremodInit() {
        manifestAttributes.putAll(provider {
            val res = HashMap<String, String>()
            with(mc) {
                if (!mc.core.containsMixinsAndOrCoreModOnly.get() && (mc.mixin.use || mc.core.coreModClass.isPresent)) {
                    res["FMLCorePluginContainsFMLMod"] = "true"
                }

                if (mc.core.coreModClass.isPresent) {
                    if (mc.core.coreModIgnoreRootPkg.get()) {
                        res["FMLCorePlugin"] = mc.core.coreModClass.get()
                    } else {
                        res["FMLCorePlugin"] = "${mod.rootPkg.get()}.${mc.core.coreModClass.get()}"
                    }
                }
            }
            res
        })
    }

    private fun Project.coreModPostInit() {
        if (!mc.core.coreModClass.isPresent)
            return
        verifyClass(mc.core.coreModClass.get(), "core -> coreModClass", mc.core.coreModIgnoreRootPkg.get())
    }

    private fun Project.accessTransformerInit() {
        manifestAttributes.putAll(provider {
            val res = HashMap<String, String>()
            if (mc.core.accessTransformerFile.isPresent) {
                res["FMLAT"] = mc.core.accessTransformerFile.get()
            }
            res
        })
    }

    private fun Project.accessTransformerPostInit() {
        if (!mc.core.accessTransformerFile.isPresent)
            return
        for (atFile in mc.core.accessTransformerFile.get().split(" ")) {
            val targetFile = "src/main/resources/META-INF/${atFile.trim()}"
            verifyFile(targetFile, "core -> accessTransformerFile")

            tasks {
                named<DeobfuscateTask>("deobfuscateMergedJarToSrg") {
                    accessTransformerFiles.from(targetFile)
                }
                named<DeobfuscateTask>("srgifyBinpatchedJar") {
                    accessTransformerFiles.from(targetFile)
                }
            }
        }
    }
}
