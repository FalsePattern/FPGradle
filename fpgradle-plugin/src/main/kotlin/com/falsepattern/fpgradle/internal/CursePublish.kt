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

package com.falsepattern.fpgradle.internal

import com.falsepattern.fpgradle.FPPlugin
import com.falsepattern.fpgradle.*
import com.gtnewhorizons.retrofuturagradle.mcp.ReobfuscatedJar
import net.darkhax.curseforgegradle.Constants
import net.darkhax.curseforgegradle.CurseForgeGradlePlugin
import net.darkhax.curseforgegradle.TaskPublishCurseForge
import org.gradle.api.Project
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.kotlin.dsl.*

class CursePublish: FPPlugin() {
    override fun addPlugins() = listOf(CurseForgeGradlePlugin::class)

    override fun Project.onPluginPostInitBeforeDeps() {
        val projectId = mc.publish.curseforge.projectId
        val token = mc.publish.curseforge.tokenEnv.map { System.getenv(it) }
        if (projectId.isPresent) {
            val publishCurseForge = tasks.register<TaskPublishCurseForge>("curseforge") {
                group = PublishingPlugin.PUBLISH_TASK_GROUP
                description = "Publish the mod to CurseForge"
                dependsOn("build")
                val theFile = tasks.named<ReobfuscatedJar>("reobfJar").flatMap { it.archiveFile }
                apiToken = token
                disableVersionDetection()
                upload(projectId.get(), theFile) {
                    changelogType = Constants.CHANGELOG_MARKDOWN
                    changelog = mc.publish.changelog
                    val version = mc.mod.version
                    displayName = version
                    releaseType = version.map { when {
                        it.contains("-a") -> Constants.RELEASE_TYPE_ALPHA
                        it.contains("-b") -> Constants.RELEASE_TYPE_BETA
                        else -> Constants.RELEASE_TYPE_RELEASE
                    } }
                    addGameVersion(minecraft.mcVersion, "Forge")
                    addModLoader("Forge")
                    for (relation in mc.publish.curseforge.relations.get())
                        relation()

                    if (mc.mixin.use) {
                        addRequirement("unimixins")
                    }
                }
            }
            if (token.isPresent) {
                tasks.named(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME).configure {
                    dependsOn(publishCurseForge)
                }
            }
        }
    }
}