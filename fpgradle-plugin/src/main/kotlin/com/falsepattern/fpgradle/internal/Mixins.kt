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
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.invoke
import java.io.File
import org.gradle.api.plugins.JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME as ANNOTATION_PROCESSOR

class Mixins: FPPlugin() {
    private lateinit var mixinConfigRefMap: Provider<String>

    override fun Project.onPluginInit() {
        mixinConfigRefMap = mc.mod.modid.map { "mixins.$it.refmap.json" }
        setupDependencies()
        setupGenerateMixinsTask()
        addMixinCommandLineArgs()
        setupManifestAttributes()
    }

    override fun Project.onPluginPostInitAfterDeps() {
        validate()
    }

    private fun Project.validate() {
        if (!mc.mixin.use)
            return

        verifyPackage(mc.mixin.pkg.get(), "mixin -> pkg", mc.mixin.ignoreRootPkg.get())

        if (mc.mixin.pluginClass.isPresent) {
            verifyClass(mc.mixin.pluginClass.get(), "mixin -> plugin", mc.mixin.ignoreRootPkg.get())
        }
    }

    private fun Project.setupDependencies() {
        dependencies {
            val modUtils = modUtils
            addProvider(ANNOTATION_PROCESSOR, provideIfMixins(mc) { "org.ow2.asm:asm-debug-all:5.0.3" })
            addProvider(ANNOTATION_PROCESSOR, provideIfMixins(mc) { "com.google.guava:guava:24.1.1-jre" })
            addProvider(ANNOTATION_PROCESSOR, provideIfMixins(mc) { "com.google.code.gson:gson:2.8.6" })
            addProvider(ANNOTATION_PROCESSOR, provideIfMixins(mc) { modUtils.enableMixins(mixinProviderSpec, mixinConfigRefMap.get()) })

            addProvider("runtimeOnlyNonPublishable", provideIfMixinsRuntime(mc) { mixinProviderSpec })
            addProvider("obfuscatedRuntimeClasspath", provideIfMixinsRuntime(mc) { mixinProviderSpecNoClassifer })
        }
        with(configurations) {
            all {
                resolutionStrategy {
                    dependencySubstitution {
                        for (sub in substituteMixins) {
                            substitute(module(sub))
                                .using(module(mixinProviderSpecNoClassifer))
                                .withClassifier("dev")
                                .because("heh")
                        }
                    }
                }
            }
        }
    }

    private fun Project.setupGenerateMixinsTask() {
        tasks {
            register("generateMixins").configure {
                group = "falsepattern"
                description = "Generates a mixin config file at /src/main/resources/mixins.modid.json if needed"
                val mc = project.mc
                val resDir = file("src/main/resources");
                onlyIf {
                    mc.mixin.use
                }
                doLast { generateMixinConfigFile(mc, resDir.resolve("mixins.${mc.mod.modid.get()}.json")) }
            }
            named("processResources").configure {
                dependsOn("generateMixins", "compileJava")
            }
        }
    }

    private fun Project.addMixinCommandLineArgs() {
        minecraft.extraRunJvmArguments.addAll(provider {
            if ((mc.mixin.use || mc.mixin.hasMixinDeps.get()) && mc.mixin.debug.get())
                listOf(
                    "-Dmixin.debug.countInjections=true",
                    "-Dmixin.debug.verbose=true",
                    "-Dmixin.debug.export=true"
                )
            else
                listOf()
        })
    }

    private fun Project.setupManifestAttributes() {
        manifestAttributes.putAll(provider {
            if (mc.mixin.use)
                mapOf(Pair("TweakClass", "org.spongepowered.asm.launch.MixinTweaker"),
                      Pair("MixinConfigs", "mixins.${mc.mod.modid.get()}.json"),
                      Pair("ForceLoadAsMod", (!mc.core.containsMixinsAndOrCoreModOnly.get()).toString()))
            else
                mapOf()
        })
    }

    private fun Task.generateMixinConfigFile(mc: FPMinecraftProjectExtension, mixinConfigFile: File) {
        if (!mixinConfigFile.exists()) {
            val mixinPluginLine = if (mc.mixin.pluginClass.isPresent) {
                if (mc.mixin.ignoreRootPkg.get()) {
                    "\"plugin\": \"${mc.mixin.pluginClass.get()}\","
                } else {
                    "\"plugin\": \"${mc.mod.rootPkg.get()}.${mc.mixin.pluginClass.get()}\","
                }
            } else
                ""
            val pkg = if (mc.mixin.ignoreRootPkg.get())
                mc.mixin.pkg.get()
            else
                "${mc.mod.rootPkg.get()}.${mc.mixin.pkg.get()}"
            mixinConfigFile.writeText("""
                {
                  "required": true,
                  "minVersion": "0.8.5",
                  "package": "$pkg",
                  $mixinPluginLine
                  "refmap": "${mixinConfigRefMap.get()}",
                  "target": "@env(DEFAULT)",
                  "compatibilityLevel": "JAVA_18",
                  "mixins": [],
                  "client": [],
                  "server": []
                }
            """.trimIndent())
        }
    }

    companion object {
        private val substituteMixins = listOf(
            "com.gtnewhorizon:gtnhmixins",
            "com.github.GTNewHorizons:Mixingasm",
            "com.github.GTNewHorizons:SpongePoweredMixin",
            "com.github.GTNewHorizons:SpongeMixins",
            "io.github.legacymoddingmc:unimixins"
        )

        private const val mixinProviderGroup = "com.github.LegacyModdingMC.UniMixins"
        private const val mixinProviderModule = "unimixins-all-1.7.10"
        private const val mixinProviderVersion = "0.1.17"
        private const val mixinProviderSpecNoClassifer = "$mixinProviderGroup:$mixinProviderModule:$mixinProviderVersion"
        private const val mixinProviderSpec = "$mixinProviderSpecNoClassifer:dev"

        private fun <T> Project.provideIfMixins(mc: FPMinecraftProjectExtension, provider: FPMinecraftProjectExtension.() -> T) = provider {
            if (mc.mixin.use)
                provider(mc)
            else
                null
        }
        private fun <T> Project.provideIfMixinsRuntime(mc: FPMinecraftProjectExtension, provider: FPMinecraftProjectExtension.() -> T) = provider {
            if (mc.mixin.use || mc.mixin.hasMixinDeps.get())
                provider(mc)
            else
                null
        }
    }
}
