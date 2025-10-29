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
import com.falsepattern.fpgradle.internal.ModernJavaTweaks.Companion.MODERN_PATCH_DEPS
import com.gtnewhorizons.retrofuturagradle.modutils.ModUtils
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Property
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
        val f = ModUtils::class.java.getDeclaredField("mixinRefMap")
        f.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val property = f.get(modUtils) as Property<String>
        property.set(mixinConfigRefMap.flatMap {
            if (mc.mixin.use) {
                provider { it }
            } else {
                provider { null }
            }
        })
        dependencies {
            addProvider(ANNOTATION_PROCESSOR, provideIfMixins(mc) { PackageRegistry.MIXINS_AP_ASM })
            addProvider(ANNOTATION_PROCESSOR, provideIfMixins(mc) { PackageRegistry.MIXINS_AP_GUAVA })
            addProvider(ANNOTATION_PROCESSOR, provideIfMixins(mc) { PackageRegistry.MIXINS_AP_GSON })
            addProvider(ANNOTATION_PROCESSOR, provideIfMixins(mc) { MIXIN_PROVIDER_SPEC })

            addProvider("devOnlyNonPublishable", provideIfMixins(mc) { MIXIN_PROVIDER_SPEC })
            addProvider("runtimeOnlyNonPublishable", provider {
                if (!mc.mixin.use && mc.mixin.hasMixinDeps.get())
                    MIXIN_PROVIDER_SPEC
                else
                    null
            })
            addProvider("obfuscatedRuntimeClasspath", provider {
                if (mc.mixin.use || mc.mixin.hasMixinDeps.get())
                    MIXIN_PROVIDER_SPEC_NO_CLASSIFIER
                else
                    null
            })
            addProvider(MODERN_PATCH_DEPS, provider {
                if (mc.java.compatibility.get() == FPMinecraftProjectExtension.Java.Compatibility.ModernJava) {
                    null
                } else {
                    MIXIN_PROVIDER_SPEC
                }
            })
        }
        with(configurations) {
            all {
                resolutionStrategy {
                    dependencySubstitution {
                        for (sub in substituteMixins) {
                            substitute(module(sub))
                                .using(module(MIXIN_PROVIDER_SPEC_NO_CLASSIFIER))
                                .withClassifier("dev")
                                .because("heh")
                        }
                    }
                }
            }
        }
    }

    private fun Project.setupGenerateMixinsTask() {
        val mcMixin = project.mc.mixin
        val pluginClass = mcMixin.pluginClass
        val pkg = mcMixin.pkg
        val ignoreRootPkg = mcMixin.ignoreRootPkg
        val rootPkg = project.mc.mod.rootPkg
        val javaCompat = project.mc.java.compatibility
        val modId = project.mc.mod.modid
        tasks {
            register("generateMixins").configure {
                group = "falsepattern"
                description = "Generates a mixin config file at /src/main/resources/mixins.modid.json if needed"
                val resDir = file("src/main/resources");
                onlyIf {
                    pkg.isPresent && pluginClass.isPresent
                }
                doLast { generateMixinConfigFile(pluginClass, pkg, ignoreRootPkg, rootPkg, javaCompat, resDir.resolve("mixins.${modId.get()}.json")) }
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
            if (mc.mixin.use) {
                val configs = ArrayList<String>()
                if (mc.mixin.pluginClass.isPresent) {
                    configs.add("mixins.${mc.mod.modid.get()}.json")
                }
                if (mc.mixin.extraConfigs.isPresent) {
                    configs.addAll(mc.mixin.extraConfigs.get())
                }
                mapOf(
                    Pair("TweakClass", "org.spongepowered.asm.launch.MixinTweaker"),
                    Pair("MixinConfigs", configs.joinToString(separator = ",")),
                    Pair("ForceLoadAsMod", (!mc.core.containsMixinsAndOrCoreModOnly.get()).toString())
                )
            } else {
                mapOf()
            }
        })
    }

    private fun generateMixinConfigFile(pluginClass: Provider<String>, pkg: Provider<String>, ignoreRootPkg: Provider<Boolean>, rootPkg: Provider<String>, javaCompat: Property<FPMinecraftProjectExtension.Java.Compatibility>, mixinConfigFile: File) {
        if (!mixinConfigFile.exists()) {
            val mixinPluginLine = if (pluginClass.isPresent) {
                if (ignoreRootPkg.get()) {
                    "\"plugin\": \"${pluginClass.get()}\","
                } else {
                    "\"plugin\": \"${rootPkg.get()}.${pluginClass.get()}\","
                }
            } else
                ""
            val pkg = if (ignoreRootPkg.get())
                pkg.get()
            else
                "${rootPkg.get()}.${pkg.get()}"
            mixinConfigFile.writeText("""
                {
                  "required": true,
                  "minVersion": "0.8.5",
                  "package": "$pkg",
                  $mixinPluginLine
                  "refmap": "${mixinConfigRefMap.get()}",
                  "target": "@env(DEFAULT)",
                  "compatibilityLevel": "${if (javaCompat.get() == FPMinecraftProjectExtension.Java.Compatibility.ModernJava) "JAVA_8" else "JAVA_18"}",
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
            "com.github.LegacyModdingMC.UniMixins:unimixins-all-1.7.10"
        )

        private const val MIXIN_PROVIDER_SPEC_NO_CLASSIFIER = "${PackageRegistry.MIXINS_GROUP}:${PackageRegistry.MIXINS_MODULE}:${PackageRegistry.MIXINS_VERSION}"
        private const val MIXIN_PROVIDER_SPEC = "$MIXIN_PROVIDER_SPEC_NO_CLASSIFIER:dev"

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
