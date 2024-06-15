/*
 * FPGradle
 *
 * Copyright (C) 2024 FalsePattern
 * All Rights Reserved
 *
 * The above copyright notice and this permission notice
 * shall be included in all copies or substantial portions of the Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

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
            ApiPackage(ctx), Shadow(ctx), SourcesPublish(ctx), MavenPublish(ctx), CursePublish(ctx), ModrinthPublish(ctx),
            CommonDeps(ctx))

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
