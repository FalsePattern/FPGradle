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
import com.matthewprenger.cursegradle.CurseExtension
import com.modrinth.minotaur.ModrinthExtension
import io.freefair.gradle.plugins.lombok.LombokExtension
import io.github.legacymoddingmc.mappinggenerator.MappingGeneratorExtension
import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.*
import org.gradle.api.provider.MapProperty
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.plugins.ide.idea.model.IdeaModel

private inline fun <reified T : Any> Project.ext(): T = extensions.getByType<T>()
val Project.mc get() = ext<FPMinecraftProjectExtension>()
val Project.curseforge get() = ext<CurseExtension>()
val Project.modrinth get() = ext<ModrinthExtension>()
val Project.minecraft get() = ext<MinecraftExtension>()
val Project.minecraftTasks get() = ext<MinecraftTasks>()
val Project.java get() = ext<JavaPluginExtension>()
val Project.mappingGenerator get() = ext<MappingGeneratorExtension>()
val Project.toolchains get() = ext<JavaToolchainService>()
val Project.manifestAttributes get() = extensions.getByName<MapProperty<String, String>>("fp_ctx_internal")
val Project.publishing get() = ext<PublishingExtension>()
val Project.base get() = ext<BasePluginExtension>()
val Project.modUtils get() = ext<ModUtils>()
val Project.sourceSets get() = ext<SourceSetContainer>()
val Project.lombok get() = ext<LombokExtension>()
val Project.idea get() = ext<IdeaModel>()