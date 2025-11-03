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
import com.falsepattern.fpgradle.internal.Stubs.Companion.JAR_STUB_TASK
import com.falsepattern.fpgradle.mc
import com.falsepattern.jtweaker.RemoveStubsJar
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.gtnewhorizons.retrofuturagradle.mcp.ReobfuscatedJar
import com.gtnewhorizons.retrofuturagradle.minecraft.RunMinecraftTask
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import xyz.wagyourtail.jvmdg.gradle.JVMDowngraderPlugin
import xyz.wagyourtail.jvmdg.gradle.jvmdg
import xyz.wagyourtail.jvmdg.gradle.task.DowngradeJar
import xyz.wagyourtail.jvmdg.gradle.task.ShadeJar

class JvmDG: FPPlugin() {
    override fun Project.addPlugins() = listOf(JVMDowngraderPlugin::class)
    override fun Project.onPluginInit() {
        setupDowngradeConfigurations()
        setupMainJarTasks()
    }

    private fun Project.setupDowngradeConfigurations() {
        repositories {
            mavenNamed("wagyourtail_jvmdowngrader_api", {name, _ ->
                val repo = repositories.maven {
                    this.name = name
                    url = uri("https://maven.wagyourtail.xyz/releases")
                }
                exclusiveContent {
                    forRepositories(repo)
                    filter {
                        includeModule("xyz.wagyourtail.jvmdowngrader", "jvmdowngrader-java-api")
                    }
                }
                repo
            })
        }
        configurations.create(JVMDG_CONFIG)
    }

    override fun Project.onPluginPostInitBeforeDeps() {
        if (mc.java.compatibility.get() == FPMinecraftProjectExtension.Java.Compatibility.JvmDowngrader) {
            val downgradeJar = tasks.named<DowngradeJar>("downgradeJar")
            val shadeRuntime = mc.java.jvm_downgrader_shade_runtime_my_project_is_compatible_with_lgpl2_1_plus
            val removeStubJar = tasks.named<RemoveStubsJar>(JAR_STUB_TASK)
            dependencies {
                add(if (shadeRuntime.get()) JVMDG_CONFIG else "implementation", "xyz.wagyourtail.jvmdowngrader:jvmdowngrader-java-api:1.3.3:downgraded-8")
            }
            tasks.withType<RunMinecraftTask> {
                if (McRun.standardNonObf().any { it.taskName == this@withType.name } or
                    McRun.modern().any { it.taskName == this@withType.name }) {
                    val oldClasspath = classpath
                    setClasspath(oldClasspath.minus(files().from(removeStubJar)))
                    classpath(downgradeJar)
                    if (shadeRuntime.get()) {
                        classpath(configurations.named(JVMDG_CONFIG))
                    }
                }
            }
        }
    }

    private fun Project.setupMainJarTasks() {
        val hasJvmDG = mc.java.compatibility.map { it == FPMinecraftProjectExtension.Java.Compatibility.JvmDowngrader }
        val shadeRuntime = mc.java.jvm_downgrader_shade_runtime_my_project_is_compatible_with_lgpl2_1_plus
        val rootPkg = mc.mod.rootPkg
        jvmdg.shadePath = {
            rootPkg.get().replace('.', '/')
        }
        jvmdg.shadeInlining = shadeRuntime
        tasks {
            val shadeDowngradeJar = named<ShadeJar>("shadeDowngradedApi")
            val downgradeJar = named<DowngradeJar>("downgradeJar")
            val jarRemoveStub = named<RemoveStubsJar>(JAR_STUB_TASK)
            val reobfJar = named<ReobfuscatedJar>("reobfJar")

            jarRemoveStub.configure {
                if (hasJvmDG.get()) {
                    archiveClassifier = "dev-predowngrade"
                    destinationDirectory.set(layout.buildDirectory.dir("tmp/fpgradle-libs"))
                }
            }

            downgradeJar.configure {
                inputFile = jarRemoveStub.flatMap { it.archiveFile }
                dependsOn(jarRemoveStub)
                if (shadeRuntime.get()) {
                    archiveClassifier = "dev-predgshade"
                    destinationDirectory.set(layout.buildDirectory.dir("tmp/fpgradle-libs"))
                } else {
                    archiveClassifier = "dev"
                }
                onlyIf {
                    hasJvmDG.get()
                }
            }

            shadeDowngradeJar.configure {
                archiveClassifier = "dev"
                onlyIf {
                    hasJvmDG.get() && shadeRuntime.get()
                }
            }

            reobfJar.configure {
                dependsOn(shadeDowngradeJar)
                @Suppress("UNCHECKED_CAST")
                setInputJarFromTask(shadeDowngradeJar as TaskProvider<org.gradle.jvm.tasks.Jar>)
            }

            for (outgoingConfig in listOf("runtimeElements", "apiElements")) {
                val outgoing = configurations.getByName(outgoingConfig)
                outgoing.outgoing.artifacts.clear()
                outgoing.outgoing.artifact(shadeDowngradeJar)
                afterEvaluate {
                    if (hasJvmDG.get()) {
                        val javaVersionAttr = Attribute.of("org.gradle.jvm.version", Int::class.javaObjectType)
                        outgoing.attributes {
                            attribute(javaVersionAttr, 8)
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val JVMDG_CONFIG = "jvmDowngraderMcRunDeps"
    }
}