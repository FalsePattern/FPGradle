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
        if (mc.mod.rootPkg.isPresent)
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
                        Pair("modId", mc.mod.modid.get()),
                        Pair("modName", mc.mod.name.get())
                    ))
                }
            }

            minecraft.injectedTags.putAll(provider {
                if (mc.tokens.tokenClass.isPresent) {
                    val result = HashMap<String, String>()

                    if (mc.tokens.modid.isPresent)
                        result[mc.tokens.modid.get()] = mc.mod.modid.get()

                    if (mc.tokens.name.isPresent)
                        result[mc.tokens.name.get()] = mc.mod.name.get()

                    if (mc.tokens.version.isPresent)
                        result[mc.tokens.version.get()] = mc.mod.version.get()

                    if (mc.tokens.rootPkg.isPresent)
                        result[mc.tokens.rootPkg.get()] = mc.mod.rootPkg.get()

                    result
                } else mapOf()
            })

            named<InjectTagsTask>("injectTags").configure {
                inputs.property("tokenId", mc.tokens.modid.get())
                inputs.property("tokenName", mc.tokens.name.get())
                inputs.property("tokenVersion", mc.tokens.version.get())
                inputs.property("tokenGroup", mc.tokens.rootPkg.get())
                if (mc.tokens.tokenClass.isPresent) {
                    inputs.property("tokenClass", mc.tokens.tokenClass.get())
                    outputClassName = mc.tokens.tokenClass.map { "${mc.mod.rootPkg.get()}.$it" }
                }
                onlyIf {
                    mc.tokens.tokenClass.isPresent
                }
            }

            named<Jar>("jar").configure {
                manifest {
                    attributes(manifestAttributes.get())
                }
            }

            ext<BasePluginExtension>().archivesName = mc.mod.modid
        }
    }
}
