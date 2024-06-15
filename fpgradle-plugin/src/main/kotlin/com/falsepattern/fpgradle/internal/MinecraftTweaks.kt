package com.falsepattern.fpgradle.internal

import com.falsepattern.fpgradle.*
import com.gtnewhorizons.retrofuturagradle.MinecraftExtension
import com.gtnewhorizons.retrofuturagradle.mcp.InjectTagsTask
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources

class MinecraftTweaks(ctx: ConfigurationContext): InitTask {
    private val project = ctx.project
    private val manifestAttributes = ctx.manifestAttributes
    private val minecraft = project.ext<MinecraftExtension>()

    override fun init() {
        jar()
    }

    override fun postInit() {
        validate()
    }

    private fun validate() = with(project) {
        if (mc.mod.group.isPresent)
            verifyPackage("", "mod -> group")
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
                        Pair("modId", mc.mod.id.get()),
                        Pair("modName", mc.mod.name.get())
                    ))
                }
            }

            minecraft.injectedTags.putAll(provider {
                if (mc.token.tokenClass.isPresent) {
                    val result = HashMap<String, String>()

                    if (mc.token.modId.isPresent)
                        result[mc.token.modId.get()] = mc.mod.id.get()

                    if (mc.token.modName.isPresent)
                        result[mc.token.modName.get()] = mc.mod.name.get()

                    if (mc.token.version.isPresent)
                        result[mc.token.version.get()] = mc.mod.version.get()

                    if (mc.token.groupName.isPresent)
                        result[mc.token.groupName.get()] = mc.mod.group.get()

                    result
                } else mapOf()
            })

            named<InjectTagsTask>("injectTags").configure {
                inputs.property("tokenId", mc.token.modId.get())
                inputs.property("tokenName", mc.token.modName.get())
                inputs.property("tokenVersion", mc.token.version.get())
                inputs.property("tokenGroup", mc.token.groupName.get())
                if (mc.token.tokenClass.isPresent) {
                    inputs.property("tokenClass", mc.token.tokenClass.get())
                    outputClassName = mc.token.tokenClass.map { "${mc.mod.group.get()}.$it" }
                }
                onlyIf {
                    mc.token.tokenClass.isPresent
                }
            }

            named<Jar>("jar").configure {
                manifest {
                    attributes(manifestAttributes.get())
                }
            }

            ext<BasePluginExtension>().archivesName = mc.mod.id
        }
    }
}