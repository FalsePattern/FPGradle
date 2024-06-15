package com.falsepattern.fpgradle.internal

import com.falsepattern.fpgradle.FPMinecraftProjectExtension
import com.falsepattern.fpgradle.resolvePath
import com.gtnewhorizons.retrofuturagradle.MinecraftExtension
import com.gtnewhorizons.retrofuturagradle.modutils.ModUtils
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.kotlin.dsl.*
import org.gradle.api.plugins.JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME as ANNOTATION_PROCESSOR
import org.gradle.api.plugins.JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME as IMPLEMENTATION
import org.gradle.api.plugins.JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME as RUNTIME_ONLY

class Mixins(ctx: ConfigurationContext): InitTask {
    private val project = ctx.project
    private val mc = project.extensions.getByType<FPMinecraftProjectExtension>()
    private val minecraft = project.extensions.getByType<MinecraftExtension>()
    private val mixinConfigRefMap = mc.modId.map { "mixins.$it.refmap.json" }
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
        if (!mc.usesMixins.get())
            return

        val mixinPackagePath = resolvePath("${mc.modGroup.get()}.${mc.mixinsPackage.get()}")
        if (!file(mixinPackagePath).exists())
            throw GradleException("Could not resolve \"mixinsPackage\"! Could not find $mixinPackagePath")

        if (mc.mixinPlugin.isPresent) {
            val mixinPluginPath = "${resolvePath("${mc.modGroup.get()}.${mc.mixinPlugin.get()}")}.java"
            if (!file(mixinPluginPath).exists())
                throw GradleException("Could not resolve \"mixinPlugin\"! Could not find $mixinPluginPath")
        }
    }

    private fun setupDependencies() = with(project) {
        dependencies {
            add(ANNOTATION_PROCESSOR, "org.ow2.asm:asm-debug-all:5.0.3");
            add(ANNOTATION_PROCESSOR, "com.google.guava:guava:24.1.1-jre");
            add(ANNOTATION_PROCESSOR, "com.google.code.gson:gson:2.8.6");
            add(ANNOTATION_PROCESSOR, mixinProviderSpec);
            addProvider(IMPLEMENTATION, provider {
                if (mc.usesMixins.get())
                    project.extensions.getByType<ModUtils>().enableMixins(mixinProviderSpec, mixinConfigRefMap.get())
                else
                    null
            })

            //TODO runtimeOnlyNonPublishable
            addProvider(RUNTIME_ONLY, provider {
                if (!mc.usesMixins.get() && mc.forceEnableMixins.get())
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
                    mc.usesMixins.get()
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
            if ((mc.usesMixins.get() || mc.forceEnableMixins.get()) && mc.usesMixinsDebug.get())
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
            if (mc.usesMixins.get())
                mapOf(Pair("TweakClass", "org.spongepowered.asm.launch.MixinTweaker"),
                      Pair("MixinConfigs", "mixins.${mc.modId.get()}.json"),
                      Pair("ForceLoadAsMod", (!mc.containsMixinsAndOrCoreModOnly.get()).toString()))
            else
                mapOf()
        })
    }

    private fun generateMixinConfigFile(task: Task) = with(task.project) {
        val mixinConfigFile = file("src/main/resources/mixins.${mc.modId.get()}.json")
        if (!mixinConfigFile.exists()) {
            val mixinPluginLine = if (mc.mixinPlugin.isPresent)
                "\"plugin\": \"${mc.modGroup.get()}.${mc.mixinPlugin.get()}\","
            else
                ""
            mixinConfigFile.writeText("""
                {
                  "required": true,
                  "minVersion": "0.8.5",
                  "package": "${mc.modGroup.get()}.${mc.mixinsPackage.get()}",
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
    }
}