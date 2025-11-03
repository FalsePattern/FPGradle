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
import com.falsepattern.fpgradle.kotlin
import com.falsepattern.fpgradle.mc
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

class Kotlin: FPPlugin() {
    override fun Project.onPluginPostInitAfterDeps() {
        if (mc.kotlin.forgelinVersion.isPresent) {
            repositories {
                wellKnownMaven("mega_forgelin", "https://mvn.falsepattern.com/gtmega_releases/") {
                    content {
                        includeModule("mega", "forgelin-mc1.7.10")
                    }
                }
                wellKnownMaven("mavenpattern_fplib", "https://mvn.falsepattern.com/releases/") {
                    content {
                        includeModule("com.falsepattern", "falsepatternlib-mc1.7.10")
                    }
                }
            }
            dependencies {
                add(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, "mega:forgelin-mc1.7.10:${mc.kotlin.forgelinVersion.get()}")
            }
        } else {
            //joml moment
            val hkd = mc.kotlin.hasKotlinDeps
            configurations.configureEach {
                if (!hkd.get()) {
                    exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
                    exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk7")
                    exclude("org.jetbrains.kotlin", "kotlin-stdlib")
                    exclude("org.jetbrains.kotlin", "kotlin-stdlib-common")
                }
            }
        }
        pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            val disable = setOf(
                "kaptGenerateStubsMcLauncherKotlin",
                "kaptGenerateStubsPatchedMcKotlin",
                "kaptGenerateStubsInjectedTagsKotlin",
                "compileMcLauncherKotlin",
                "compilePatchedMcKotlin",
                "compileInjectedTagsKotlin",
                "kaptMcLauncherKotlin",
                "kaptPatchedMcKotlin",
                "kaptInjectedTagsKotlin",
                "kspMcLauncherKotlin",
                "kspPatchedMcKotlin",
                "kspInjectedTagsKotlin"
            )
            tasks.configureEach {
                if (name in disable) {
                    enabled = false
                }
            }
        }
    }
}