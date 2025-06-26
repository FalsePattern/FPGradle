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

import com.falsepattern.fpgradle.FPPlugin
import com.falsepattern.fpgradle.currentTimestamp
import com.falsepattern.fpgradle.getValueSource
import org.gradle.api.Project
import org.gradle.api.configuration.BuildFeatures
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.kotlin.dsl.assign
import javax.inject.Inject

abstract class GitPlugin: FPPlugin() {
    @get:Inject
    protected abstract val buildFeatures: BuildFeatures

    override fun Project.addTasks() = mapOf(
        Pair("extractGitIgnore", ExtractGitIgnoreTask::class),
        Pair("extractGitAttributes", ExtractGitAttributesTask::class),
    )

    override fun Project.onPluginInit() {
        version = ProviderStringifier(getValueSource(EnvVersionSource::class)
            .orElse(provider { if (hasProperty("versionOverride")) property("versionOverride")?.toString() else null })
            .orElse(autoVersion))
    }

    private val Project.autoVersion get() = gitTagVersion.orElse(ccSafeTimestamp("unknown"))

    private val Project.gitTagVersion: Provider<String>
        get() {
        var gitRepoDir = rootDir
        if (!gitRepoDir.resolve(".git").exists()) {
            gitRepoDir = gitRepoDir.parentFile!!
        }
        return getValueSource(GitTagVersionSource::class) {
            this.gitRepoDir = gitRepoDir
            this.dirtySuffix = ccSafeTimestamp(null)
        }
    }

    private fun Project.ccSafeTimestamp(otherwise: String?): Provider<String> {
        return buildFeatures.configurationCache.requested.orElse(false)
            .flatMap { configCache ->
                if (configCache || (hasProperty("fpgradle.timestamp-in-version") && property("fpgradle.timestamp-in-version") == "false")) {
                    provider { otherwise }
                } else {
                    currentTimestamp
                }
            }
    }

    class ProviderStringifier(val prov: Provider<*>) {
        private val strValue: String by lazy {
            prov.orNull.toString()
        }
        override fun toString(): String {
            return strValue
        }
    }

    abstract class EnvVersionSource: ValueSource<String, ValueSourceParameters.None> {
        override fun obtain(): String? {
            return System.getenv("RELEASE_VERSION")
        }
    }
}