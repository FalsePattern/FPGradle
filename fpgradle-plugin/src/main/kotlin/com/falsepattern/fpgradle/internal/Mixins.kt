package com.falsepattern.fpgradle.internal

import com.falsepattern.fpgradle.*
import com.gtnewhorizons.retrofuturagradle.MinecraftExtension
import com.gtnewhorizons.retrofuturagradle.modutils.ModUtils
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.*
import org.gradle.api.plugins.JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME as ANNOTATION_PROCESSOR
import org.gradle.api.plugins.JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME as IMPLEMENTATION

class Mixins(ctx: ConfigurationContext): InitTask {
    private val project = ctx.project
    private val minecraft = project.ext<MinecraftExtension>()
    private val mixinConfigRefMap = project.mc.mod.modid.map { "mixins.$it.refmap.json" }
    private val manifestAttributes = ctx.manifestAttributes

    override fun init() {
        setupDependencies()
        setupGenerateMixinsTask()
        addMixinCommandLineArgs()
        setupManifestAttributes()
    }

    override fun postInit() {
        validate()
    }

    private fun validate() = with(project) {
        if (!mc.mixin.use)
            return

        verifyPackage(mc.mixin.pkg.get(), "mixin -> pkg")

        if (mc.mixin.pluginClass.isPresent) {
            verifyClass(mc.mixin.pluginClass.get(), "mixin -> plugin")
        }
    }

    private fun setupDependencies() = with(project) {
        dependencies {
            addProvider(ANNOTATION_PROCESSOR, provideIfMixins(mc) { "org.ow2.asm:asm-debug-all:5.0.3" })
            addProvider(ANNOTATION_PROCESSOR, provideIfMixins(mc) { "com.google.guava:guava:24.1.1-jre" })
            addProvider(ANNOTATION_PROCESSOR, provideIfMixins(mc) { "com.google.code.gson:gson:2.8.6" })
            addProvider(ANNOTATION_PROCESSOR, provideIfMixins(mc) { mixinProviderSpec })
            addProvider(IMPLEMENTATION, provideIfMixins(mc) {
                project.ext<ModUtils>().enableMixins(mixinProviderSpec, mixinConfigRefMap.get())
            })

            addProvider("runtimeOnlyNonPublishable", provider {
                if (!mc.mixin.use && mc.mixin.hasMixinDeps.get())
                    mixinProviderSpec
                else
                    null
            })
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

    private fun setupGenerateMixinsTask() = with(project) {
        tasks {
            register("generateMixins").configure {
                group = "FPGradle"
                description = "Generates a mixin config file at /src/main/resources/mixins.modid.json if needed"
                onlyIf {
                    mc.mixin.use
                }
                doLast(::generateMixinConfigFile)
            }
            named("processResources").configure {
                dependsOn("generateMixins", "compileJava")
            }
        }
    }

    private fun addMixinCommandLineArgs() = with(project) {
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

    private fun setupManifestAttributes() = with(project) {
        manifestAttributes.putAll(provider {
            if (mc.mixin.use)
                mapOf(Pair("TweakClass", "org.spongepowered.asm.launch.MixinTweaker"),
                      Pair("MixinConfigs", "mixins.${mc.mod.modid.get()}.json"),
                      Pair("ForceLoadAsMod", (!mc.core.containsMixinsAndOrCoreModOnly.get()).toString()))
            else
                mapOf()
        })
    }

    private fun generateMixinConfigFile(task: Task) = with(task.project) {
        val mixinConfigFile = file("src/main/resources/mixins.${mc.mod.modid.get()}.json")
        if (!mixinConfigFile.exists()) {
            val mixinPluginLine = if (mc.mixin.pluginClass.isPresent)
                "\"plugin\": \"${mc.mod.rootPkg.get()}.${mc.mixin.pluginClass.get()}\","
            else
                ""
            mixinConfigFile.writeText("""
                {
                  "required": true,
                  "minVersion": "0.8.5",
                  "package": "${mc.mod.rootPkg.get()}.${mc.mixin.pkg.get()}",
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
    }
}
