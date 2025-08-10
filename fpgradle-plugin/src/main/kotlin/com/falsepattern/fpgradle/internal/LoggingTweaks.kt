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

import com.falsepattern.fpgradle.FPMinecraftProjectExtension
import com.falsepattern.fpgradle.FPPlugin
import com.falsepattern.fpgradle.mc
import com.gtnewhorizons.retrofuturagradle.minecraft.RunMinecraftTask
import org.gradle.api.Project
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import org.gradle.language.jvm.tasks.ProcessResources

class LoggingTweaks: FPPlugin() {
    override fun Project.onPluginInit() {
        val mc = mc
        tasks.named<ProcessResources>("processPatchedMcResources").configure {
            inputs.property("logLevel", mc.logging.level)

            val logLevel = mc.logging.level.get()

            val replacement = if (logLevel == FPMinecraftProjectExtension.Logging.Level.ALL) "" else "level=\"${logLevel.name}\""
            filesMatching("log4j2.xml") {
                filter {
                    it.replace("level=\"INFO\"", replacement)
                }
            }
        }
        tasks.withType<RunMinecraftTask>().configureEach {
            if (mc.java.compatibility.get() == FPMinecraftProjectExtension.Java.Compatibility.ModernJava) {
                val path = layout.buildDirectory.file("resources/patchedMc/log4j2.xml").get().asFile.toURI()
                systemProperty("log4j.configurationFile", path)
            }
        }
    }
}