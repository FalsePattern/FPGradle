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

import org.eclipse.jgit.api.Git
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.io.File
import java.io.IOException

abstract class GitTagVersionSource: ValueSource<GitTagVersion, GitTagVersionSource.Params> {
    interface Params: ValueSourceParameters {
        val gitRepoDir: Property<File>
    }

    override fun obtain(): GitTagVersion? {
        val gitRepoDir = parameters.gitRepoDir.orNull ?: return null

        try {
            return Git.open(gitRepoDir).use { git ->
                val repo = git.repository

                val head = repo.exactRef("HEAD") ?: return@use null

                val currentHash = head.objectId

                val isClean = git.status().call().isClean

                if (isClean) {
                    val tags = git.tagList().call()
                    for (tag in tags) {
                        if (tag.objectId == currentHash) {
                            val version = tag.name.substring(10)

                            return@use GitTagVersion(version, true)
                        }
                    }
                }

                val branch = repo.branch
                val shortHash = currentHash.name().substring(0, 7)
                val version = "$branch-$shortHash"
                return@use GitTagVersion(version, isClean)
            }
        } catch (_: IOException) {}
        return null
    }
}