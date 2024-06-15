package com.falsepattern.fpgradle.plugin

import com.falsepattern.fpgradle.FPMinecraftProjectExtension
import com.falsepattern.fpgradle.FPPlugin
import com.falsepattern.fpgradle.internal.*
import com.falsepattern.fpgradle.module.git.GitPlugin
import com.falsepattern.fpgradle.module.jetbrains.JetBrainsPlugin
import com.falsepattern.jtweaker.JTweakerPlugin
import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.gtnewhorizons.retrofuturagradle.UserDevPlugin
import io.github.legacymoddingmc.mappinggenerator.MappingGeneratorPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.kotlin.dsl.*

class MinecraftPlugin: FPPlugin() {
    private lateinit var tasks: List<InitTask>

    override fun addPlugins() = listOf(
        JavaPlugin::class,
        UserDevPlugin::class,
        ShadowPlugin::class,
        MavenPublishPlugin::class,
        MappingGeneratorPlugin::class,
        JTweakerPlugin::class,
        GitPlugin::class,
        JetBrainsPlugin::class
    )

    override fun onPluginInit(project: Project): Unit {
        val ctx = ConfigurationContext(project)

        project.extensions.create("MC", FPMinecraftProjectExtension::class, project)

        tasks = listOf(RFGTweaks(ctx), MinecraftTweaks(ctx), FMLTweaks(ctx), Mixins(ctx), NonPublishable(ctx))

        tasks.forEach(InitTask::init)
    }

    override fun onPluginPostInit(project: Project) {
        tasks.forEach(InitTask::postInit)
    }
}