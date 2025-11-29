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
import com.falsepattern.fpgradle.FPMinecraftProjectExtension.Java
import com.falsepattern.fpgradle.FPPlugin
import com.falsepattern.fpgradle.internal.Stubs.Companion.JAR_STUB_TASK
import com.falsepattern.fpgradle.mc
import com.falsepattern.jtweaker.RemoveStubsJar
import com.gtnewhorizons.retrofuturagradle.mcp.ReobfuscatedJar
import com.gtnewhorizons.retrofuturagradle.minecraft.RunMinecraftTask
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.assign
import xyz.wagyourtail.jvmdg.gradle.JVMDowngraderPlugin
import xyz.wagyourtail.jvmdg.gradle.jvmdg
import xyz.wagyourtail.jvmdg.gradle.task.DowngradeJar
import xyz.wagyourtail.jvmdg.gradle.task.ShadeJar

class JvmDG: FPPlugin() {
    override fun Project.addPlugins() = listOf(JVMDowngraderPlugin::class)
    override fun Project.onPluginInit() {
        setupDowngradeConfigurations()
        setupTasksForSetInternal(
            mc.java.compatibility,
            mc.java.jvmDowngraderShade,
            null,
            null,
            "dev",
            mc.java.jvmDowngraderShadePackage,
            true,
            null
        )
    }

    override fun Project.onPluginPostInitBeforeDeps() {
        if (mc.java.compatibility.get() == Java.Compatibility.JvmDowngrader) {
            val downgradeJar = tasks.named<DowngradeJar>("downgradeJar")
            val shadeDowngradeJar = tasks.named<ShadeJar>("shadeDowngradedApi")
            val shadeRuntime = mc.java.jvmDowngraderShade.map { it != FPMinecraftProjectExtension.Java.JvmDowngraderShade.DoNotShade }
            val removeStubJar = tasks.named<RemoveStubsJar>(JAR_STUB_TASK)
            tasks.withType<RunMinecraftTask> {
                if (McRun.standardNonObf().any { it.taskName == this@withType.name } or
                    McRun.modern().any { it.taskName == this@withType.name }) {
                    val oldClasspath = classpath
                    setClasspath(oldClasspath.minus(files().from(removeStubJar)))
                    classpath(shadeRuntime.flatMap { if (it) shadeDowngradeJar else downgradeJar })
                }
            }

            tasks {
                val downgradeApiJar = register<DowngradeJar>("downgradeApiJar")
                val apiJar = named<Jar>("apiJar")

                wireDowngrade(provider { true }, apiJar, downgradeApiJar, null, "api")
            }
        }
    }

    companion object {
        fun Project.setupDowngradeTasksForSet(javaCompatibility: Provider<Java.Compatibility>,
                                              jvmDowngraderShade: Provider<Java.JvmDowngraderShade>,
                                              archiveBaseName: Provider<String>,
                                              archiveVersion: Provider<String>,
                                              classifier: String,
                                              shadePkg: Provider<String>,
                                              hasReobfJar: Boolean,
                                              set: SourceSet): TaskProvider<out org.gradle.api.tasks.bundling.Jar> {
            return setupTasksForSetInternal(javaCompatibility, jvmDowngraderShade, archiveBaseName, archiveVersion, classifier, shadePkg, hasReobfJar, set)
        }

        private fun Project.setupTasksForSetInternal(javaCompatibility: Provider<Java.Compatibility>,
                                                     jvmDowngraderShade: Provider<Java.JvmDowngraderShade>,
                                                     archiveBaseName: Provider<String>?,
                                                     archiveVersion: Provider<String>?,
                                                     classifier: String,
                                                     shadePkg: Provider<String>,
                                                     hasReobfJar: Boolean,
                                                     set: SourceSet?): TaskProvider<out org.gradle.api.tasks.bundling.Jar> {
            val hasJvmDG = javaCompatibility.map { it == Java.Compatibility.JvmDowngrader }
            val shadeRuntimeCompatible = jvmDowngraderShade.map { it == FPMinecraftProjectExtension.Java.JvmDowngraderShade.ProjectIsLgpl21PlusCompatible }
            val shadeRuntime = jvmDowngraderShade.map { it != FPMinecraftProjectExtension.Java.JvmDowngraderShade.DoNotShade }
            val shadePath: (String) -> String = {
                shadePkg.get().replace('.', '/')
            }
            if (set == null) {
                jvmdg.shadePath = shadePath
                jvmdg.shadeInlining = shadeRuntime
            }
            val shadeDowngradeJar = set?.let { tasks.register<ShadeJar>("shadeDowngradedApi${it.name}") } ?: tasks.named<ShadeJar>("shadeDowngradedApi")
            val downgradeJar = set?.let { tasks.register<DowngradeJar>("downgradeJar${it.name}") } ?: tasks.named<DowngradeJar>("downgradeJar")
            val jarRemoveStub = tasks.named<RemoveStubsJar>(set?.let { "${it.jarTaskName}RemoveStubs" } ?: JAR_STUB_TASK)
            val reobfJar = if (hasReobfJar) tasks.named<ReobfuscatedJar>(set?.let { "reobf${it.jarTaskName}" } ?: "reobfJar") else null

            shadeDowngradeJar.configure {
                archiveBaseName?.let { this.archiveBaseName = it }
                archiveVersion?.let { this.archiveVersion = it }
            }

            downgradeJar.configure {
                archiveBaseName?.let { this.archiveBaseName = it }
                archiveVersion?.let { this.archiveVersion = it }
            }

            if (set != null) {
                shadeDowngradeJar.configure {
                    this.inputFile = downgradeJar.flatMap { it.archiveFile }
                    this.shadePath = shadePath
                    this.shadeInlining = shadeRuntime
                }
            }

            wireDowngrade(
                hasJvmDG,
                jarRemoveStub,
                downgradeJar,
                ShadeParams(
                    shadeRuntime,
                    shadeRuntimeCompatible,
                    shadeDowngradeJar
                ),
                classifier)

            reobfJar?.configure {
                dependsOn(shadeDowngradeJar)
                @Suppress("UNCHECKED_CAST")
                setInputJarFromTask(shadeDowngradeJar as TaskProvider<org.gradle.jvm.tasks.Jar>)
            }

            if (set == null) {
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
            return shadeDowngradeJar
        }

        private fun Project.setupDowngradeConfigurations() {
            repositories {
                mavenNamed("wagyourtail_jvmdowngrader_api", {name, _ ->
                    repositories.maven {
                        this.name = name
                        url = uri("https://maven.wagyourtail.xyz/releases")
                        content {
                            includeModule("xyz.wagyourtail.jvmdowngrader", "jvmdowngrader-java-api")
                        }
                    }
                })
            }
        }

        private fun Project.wireDowngrade(
            hasJvmDG: Provider<Boolean>,
            input: TaskProvider<out Jar>,
            downgradeJar: TaskProvider<DowngradeJar>,
            shade: ShadeParams?,
            classifier: String
        ) {
            input.configure {
                if (hasJvmDG.get()) {
                    archiveClassifier = "$classifier-predowngrade"
                    destinationDirectory.set(layout.buildDirectory.dir("tmp/fpgradle-libs"))
                }
            }

            downgradeJar.configure {
                inputFile = input.flatMap { it.archiveFile }
                dependsOn(input)
                if (shade != null && shade.shadeRuntime.get()) {
                    //Only put the pre-shaded jar in tmp if it is safe to do so.
                    //
                    //Excerpt from JVMDowngrader license:
                    //
                    //For the purpose of Licensing, the produced jar from this task, or the downgrading task
                    // should be considered a "Combined Work", as it contains the original code from the input
                    // jar and the shaded code from jvmdowngrader's api.
                    //
                    //And this does, usually, mean that you shouldn't need to use the exact same license.
                    // Running this tool, should be a thing the end-user is capable of doing, thus section 6.a should
                    // be satisfied as long as your project provides the unshaded/undowngraded jar as well,
                    // or alternatively provides source code to build said jar, or the post-shaded jar.
                    archiveClassifier = "$classifier-predgshade"
                    if (shade.shadeRuntimeCompatible.get()) {
                        destinationDirectory.set(layout.buildDirectory.dir("tmp/fpgradle-libs"))
                    }
                } else {
                    archiveClassifier = classifier
                }
                onlyIf {
                    hasJvmDG.get()
                }
            }

            shade?.shadeDowngradeJar?.configure {
                val shadeRuntime = shade.shadeRuntime
                archiveClassifier = classifier
                onlyIf {
                    hasJvmDG.get() && shadeRuntime.get()
                }
            }
        }


        private data class ShadeParams(
            val shadeRuntime: Provider<Boolean>,
            val shadeRuntimeCompatible: Provider<Boolean>,
            val shadeDowngradeJar: TaskProvider<ShadeJar>)
    }
}