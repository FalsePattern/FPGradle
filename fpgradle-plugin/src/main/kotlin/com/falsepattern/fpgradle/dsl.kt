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

package com.falsepattern.fpgradle

import com.gtnewhorizons.retrofuturagradle.MinecraftExtension
import com.gtnewhorizons.retrofuturagradle.minecraft.MinecraftTasks
import com.gtnewhorizons.retrofuturagradle.modutils.ModUtils
import com.gtnewhorizons.retrofuturagradle.modutils.ModUtils.RfgDependencyExtension
import com.modrinth.minotaur.ModrinthExtension
import io.freefair.gradle.plugins.lombok.LombokExtension
import io.github.legacymoddingmc.mappinggenerator.MappingGeneratorExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.*
import org.gradle.api.provider.MapProperty
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.plugins.ide.idea.model.IdeaModel

internal inline fun <reified T : Any> ExtensionAware.ext(): T = extensions.getByType<T>()
internal val Project.mc get() = ext<FPMinecraftProjectExtension>()
internal val Project.modrinth get() = ext<ModrinthExtension>()
internal val Project.minecraft get() = ext<MinecraftExtension>()
internal val Project.minecraftTasks get() = ext<MinecraftTasks>()
internal val Project.java get() = ext<JavaPluginExtension>()
internal val Project.mappingGenerator get() = ext<MappingGeneratorExtension>()
internal val Project.toolchains get() = ext<JavaToolchainService>()
internal val Project.manifestAttributes get() = extensions.getByName<MapProperty<String, String>>("fp_ctx_internal")
internal val Project.publishing get() = ext<PublishingExtension>()
internal val Project.base get() = ext<BasePluginExtension>()
internal val Project.modUtils get() = ext<ModUtils>()
internal val Project.sourceSets get() = ext<SourceSetContainer>()
internal val Project.lombok get() = ext<LombokExtension>()
internal val Project.idea get() = ext<IdeaModel>()
internal val DependencyHandler.rfg get() = ext<RfgDependencyExtension>()