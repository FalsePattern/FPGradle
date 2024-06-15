package com.falsepattern.fpgradle.internal

import com.falsepattern.fpgradle.FPMinecraftProjectExtension
import com.falsepattern.fpgradle.resolvePath
import com.gtnewhorizons.retrofuturagradle.mcp.DeobfuscateTask
import org.gradle.api.GradleException
import org.gradle.kotlin.dsl.*

class FMLTweaks(ctx: ConfigurationContext): InitTask {
    private val project = ctx.project
    private val manifestAttributes = ctx.manifestAttributes
    private val mc = project.extensions.getByType<FPMinecraftProjectExtension>()

    override fun init() {
        coremodInit()
    }

    override fun postInit() {
        coreModPostInit()
    }

    private fun coremodInit() = with(project) {
        manifestAttributes.putAll(provider {
            val res = HashMap<String, String>()
            with(mc) {
                if (!containsMixinsAndOrCoreModOnly.get() && (usesMixins.get() || coreModClass.isPresent)) {
                    res["FMLCorePluginContainsFMLMod"] = "true"
                }

                if (coreModClass.isPresent) {
                    res["FMLCorePlugin"] = "${modGroup.get()}.${coreModClass.get()}"
                }
            }
            res
        })
    }

    private fun coreModPostInit() = with(project) {
        if (!mc.coreModClass.isPresent)
            return
        val targetFile = "${resolvePath("${mc.modGroup.get()}.${mc.coreModClass.get()}")}.java"
        if (!project.file(targetFile).exists())
            throw GradleException("Could not resolve \"coreModClass\"! Could not find $targetFile")
    }

    private fun accessTransformerInit() = with(project) {
        manifestAttributes.putAll(provider {
            val res = HashMap<String, String>()
            if (mc.accessTransformersFile.isPresent) {
                res["FMLAT"] = mc.accessTransformersFile.get()
            }
            res
        })
    }

    private fun accessTransformerPostInit() = with(project) {
        with(mc) {
            if (!accessTransformersFile.isPresent)
                return
            for (atFile in accessTransformersFile.get().split(" ")) {
                val targetFile = "src/main/resources/META-INF/${atFile.trim()}"
                if (!file(targetFile).exists())
                    throw GradleException("Could not resolve \"accessTransformersFile\"! Could not find $targetFile")

                tasks {
                    named<DeobfuscateTask>("deobfuscateMergedJarToSrg") {
                        accessTransformerFiles.from(targetFile)
                    }
                    named<DeobfuscateTask>("srgifyBinpatchedJar") {
                        accessTransformerFiles.from(targetFile)
                    }
                }
            }
        }
    }
}