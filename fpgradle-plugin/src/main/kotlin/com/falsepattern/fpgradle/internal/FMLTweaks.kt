package com.falsepattern.fpgradle.internal

import com.falsepattern.fpgradle.ext
import com.falsepattern.fpgradle.mc
import com.falsepattern.fpgradle.verifyClass
import com.falsepattern.fpgradle.verifyFile
import com.gtnewhorizons.retrofuturagradle.MinecraftExtension
import com.gtnewhorizons.retrofuturagradle.mcp.DeobfuscateTask
import com.gtnewhorizons.retrofuturagradle.minecraft.RunMinecraftTask
import org.gradle.kotlin.dsl.*

class FMLTweaks(ctx: ConfigurationContext): InitTask {
    private val project = ctx.project
    private val manifestAttributes = ctx.manifestAttributes

    override fun init() {
        coremodInit()
        accessTransformerInit()
    }

    override fun postInit() {
        coreModPostInit()
        accessTransformerPostInit()
        runArgs()
    }

    private fun runArgs() = with(project) {
        tasks.named<RunMinecraftTask>("runClient") {
            username = mc.run.username.get()
            if (mc.run.userUUID.isPresent)
                userUUID = mc.run.userUUID.get().toString()
        }
    }

    private fun coremodInit() = with(project) {
        manifestAttributes.putAll(provider {
            val res = HashMap<String, String>()
            with(mc) {
                if (!mc.core.containsMixinsAndOrCoreModOnly.get() && (mc.mixin.use || mc.core.coreModClass.isPresent)) {
                    res["FMLCorePluginContainsFMLMod"] = "true"
                }

                if (mc.core.coreModClass.isPresent) {
                    res["FMLCorePlugin"] = "${mod.rootPkg.get()}.${mc.core.coreModClass.get()}"
                }
            }
            res
        })
    }

    private fun coreModPostInit() = with(project) {
        if (!mc.core.coreModClass.isPresent)
            return
        verifyClass(mc.core.coreModClass.get(), "core -> coreModClass")
    }

    private fun accessTransformerInit() = with(project) {
        manifestAttributes.putAll(provider {
            val res = HashMap<String, String>()
            if (mc.core.accessTransformerFile.isPresent) {
                res["FMLAT"] = mc.core.accessTransformerFile.get()
            }
            res
        })
    }

    private fun accessTransformerPostInit() = with(project) {
        with(mc) {
            if (!core.accessTransformerFile.isPresent)
                return
            for (atFile in core.accessTransformerFile.get().split(" ")) {
                val targetFile = "src/main/resources/META-INF/${atFile.trim()}"
                verifyFile(targetFile, "core -> accessTransformerFile")

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
