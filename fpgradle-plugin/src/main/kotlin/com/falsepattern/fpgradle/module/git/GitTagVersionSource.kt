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

package com.falsepattern.fpgradle.module.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.io.File
import java.io.IOException

abstract class GitTagVersionSource: ValueSource<String, GitTagVersionSource.Params> {
    interface Params: ValueSourceParameters {
        val gitRepoDir: Property<File>
        val dirtySuffix: Property<String>
    }

    override fun obtain(): String? {
        val gitRepoDir = parameters.gitRepoDir.orNull ?: return null

        try {
            Git.open(gitRepoDir).use { git ->
                val repo = git.repository
                val status = git.status().call()
                val isClean = status.isClean

                val describe = runCatching {
                    git.describe().setTags(true).call()
                }.getOrNull() ?: runCatching {
                    repo.branch + "-" + git.describe().setAlways(true).call()
                }.getOrNull() ?: (repo.branch + "-unknown")

                if (isClean) {
                    return describe
                }

                return "$describe-${parameters.dirtySuffix.getOrElse("dirty")}"
            }
        } catch (_: IOException) {} catch (_: GitAPIException) {}
        return null
    }
}