/*
 * FPGradle
 *
 * Copyright (C) 2024-2025 FalsePattern
 * All Rights Reserved
 *
 * The above copyright notice and this permission notice
 * shall be included in all copies or substantial portions of the Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
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

import com.falsepattern.fpgradle.FPInternalProjectExtension
import com.falsepattern.fpgradle.FPMinecraftProjectExtension
import com.falsepattern.fpgradle.FPPlugin
import com.falsepattern.fpgradle.internal.*
import com.falsepattern.fpgradle.mc
import com.falsepattern.fpgradle.module.git.GitPlugin
import com.falsepattern.fpgradle.module.jetbrains.JetBrainsPlugin
import com.falsepattern.fpgradle.module.lombok.FPLombokPlugin
import com.falsepattern.fpgradle.module.updates.FPUpdatesPlugin
import com.falsepattern.jtweaker.JTweakerPlugin
import com.gtnewhorizons.retrofuturagradle.UserDevPlugin
import io.github.legacymoddingmc.mappinggenerator.MappingGeneratorPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import kotlin.reflect.KClass

class MinecraftPlugin: FPPlugin() {
    override fun Project.addPlugins(): List<KClass<out Plugin<Project>>> {
        val result = ArrayList<KClass<out Plugin<Project>>>()
        if (project.name == "fpgradle-examplemod1") {
            result.add(MappingGeneratorPlugin::class)
        }
        result.addAll(listOf(
            Stubs::class,

            FPLombokPlugin::class,
            JetBrainsPlugin::class,
            GitPlugin::class,
            Kotlin::class,
            Scala::class,
            JarInJar::class,
            ReproducibleJars::class,

            NonPublishable::class,
            CommonDeps::class,

            ModernJavaTweaks::class,
            MinecraftTweaks::class,
            FMLTweaks::class,
            Mixins::class,

            SourcesPublish::class,
            ApiPackage::class,
            Shadow::class,
            JvmDG::class,

            LoggingTweaks::class,

            MavenPublish::class,
            CursePublish::class,
            ModrinthPublish::class,

            FPUpdatesPlugin::class,
        ))
        return result
    }

    override fun Project.onPluginApplyBeforeDeps() {
        pluginManager.apply(JavaPlugin::class)
        pluginManager.apply(UserDevPlugin::class)
        extensions.create("fp_ctx_internal", FPInternalProjectExtension::class)
        extensions.create("minecraft_fp", FPMinecraftProjectExtension::class, project)
    }

    override fun Project.onPluginPostInitAfterDeps() {
        if (!mc.mod.name.isPresent)
            System.err.println("Missing configuration: MC -> mod -> name")
        if (!mc.mod.modid.isPresent)
            System.err.println("Missing configuration: MC -> mod -> modid")
        if (!mc.mod.rootPkg.isPresent)
            System.err.println("Missing configuration: MC -> mod -> rootPkg")
    }
}
