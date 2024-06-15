package com.falsepattern.fpgradle.project

import com.falsepattern.fpgradle.FPMinecraftProjectExtension
import com.falsepattern.fpgradle.FPPlugin
import com.falsepattern.fpgradle.internal.*
import com.falsepattern.fpgradle.mc
import com.falsepattern.fpgradle.module.git.GitPlugin
import com.falsepattern.fpgradle.module.jetbrains.JetBrainsPlugin
import com.falsepattern.fpgradle.module.lombok.FPLombokPlugin
import com.falsepattern.jtweaker.JTweakerPlugin
import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.gtnewhorizons.retrofuturagradle.UserDevPlugin
import com.matthewprenger.cursegradle.CurseGradlePlugin
import com.modrinth.minotaur.Minotaur
import io.github.legacymoddingmc.mappinggenerator.MappingGeneratorPlugin
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
        FPLombokPlugin::class,
        JetBrainsPlugin::class,
        CurseGradlePlugin::class,
        Minotaur::class,
    )

    override fun onPluginInit(project: Project): Unit {
        val ctx = ConfigurationContext(project)

        project.extensions.create("minecraft_fp", FPMinecraftProjectExtension::class, project)

        tasks = listOf(NonPublishable(ctx), ModernJavaTweaks(ctx), MinecraftTweaks(ctx), FMLTweaks(ctx), Mixins(ctx),
            ApiPackage(ctx), Shadow(ctx), SourcesPublish(ctx), MavenPublish(ctx), CursePublish(ctx), ModrinthPublish(ctx))

        tasks.forEach(InitTask::init)
    }

    override fun onPluginPostInit(project: Project) = with(project) {
        this@MinecraftPlugin.tasks.forEach(InitTask::postInit)
        if (!mc.mod.name.isPresent)
            System.err.println("Missing configuration: MC -> mod -> name")
        if (!mc.mod.modid.isPresent)
            System.err.println("Missing configuration: MC -> mod -> id")
        if (!mc.mod.rootPkg.isPresent)
            System.err.println("Missing configuration: MC -> mod -> group")
    }
}
