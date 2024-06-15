package com.falsepattern.fpgradle.internal

import com.falsepattern.fpgradle.FPMinecraftProjectExtension
import com.falsepattern.fpgradle.resolvePath
import com.gtnewhorizons.retrofuturagradle.MinecraftExtension
import com.gtnewhorizons.retrofuturagradle.mcp.InjectTagsTask
import org.gradle.api.GradleException
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources

class MinecraftTweaks(ctx: ConfigurationContext): InitTask {
    private val project = ctx.project
    private val manifestAttributes = ctx.manifestAttributes
    private val mc = project.extensions.getByType<FPMinecraftProjectExtension>()
    private val minecraft = project.extensions.getByType<MinecraftExtension>()

    override fun init() {
        jar()
    }

    override fun postInit() {
        validate()
    }

    private fun validate() = with(project) {
        val modGroupPath = resolvePath(mc.modGroup.get())
        if (!file(modGroupPath).exists())
            throw GradleException("Could not resolve \"modGroup\"! Could not find $modGroupPath")
    }

    private fun jar() = with(project) {
        tasks {
            named<ProcessResources>("processResources").configure {
                inputs.property("version", version)
                inputs.property("mcversion", minecraft.mcVersion)
                filesMatching("mcmod.info") {
                    expand(mapOf(
                        Pair("minecraftVersion", minecraft.mcVersion.get()),
                        Pair("modVersion", project.version),
                        Pair("modId", mc.modId.get()),
                        Pair("modName", mc.modName.get())
                    ))
                }
            }
            minecraft.injectedTags.putAll(provider {
                if (mc.generateGradleTokenClass.isPresent && mc.gradleTokenVersion.isPresent)
                    mapOf(Pair(mc.gradleTokenVersion.get(), version))
                else
                    mapOf()
            })

            named<InjectTagsTask>("injectTags").configure {
                inputs.property("tokenClass", mc.generateGradleTokenClass.get())
                inputs.property("tokenVersion", mc.gradleTokenVersion.get())
                if (mc.generateGradleTokenClass.isPresent)
                    outputClassName = mc.generateGradleTokenClass.map { "${mc.modGroup.get()}.$it" }
            }

            named<Jar>("jar").configure {
                manifest {
                    attributes(manifestAttributes.get())
                }
            }

            extensions.getByType<BasePluginExtension>().archivesName = mc.modId
        }
    }
}