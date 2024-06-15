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

package com.falsepattern.fpgradle.module.git

import com.falsepattern.fpgradle.FPPlugin
import com.falsepattern.fpgradle.currentTimestamp
import com.falsepattern.fpgradle.getValueSource
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.assign
import kotlin.reflect.KClass

class GitPlugin: FPPlugin() {
    override fun addTasks() = mapOf(
        Pair("extractGitIgnore", ExtractGitIgnoreTask::class),
        Pair("extractGitAttributes", ExtractGitAttributesTask::class),
    )

    private fun getGitTagVersion(project: Project): GitTagVersion? {
        var gitRepoDir = project.rootDir
        if (!gitRepoDir.resolve(".git").exists()) {
            gitRepoDir = gitRepoDir.parentFile!!
        }
        return project.getValueSource(GitTagVersionSource::class) {
            this.gitRepoDir = gitRepoDir
        }.orNull
    }

    override fun onPluginInit(project: Project) {
        project.version = if (project.hasProperty("versionOverride"))
            project.property("versionOverride")!!
        else
            getVersion(project)
    }

    private fun getVersion(project: Project) = getGitTagVersion(project)?.toString() ?: currentTimestamp
}