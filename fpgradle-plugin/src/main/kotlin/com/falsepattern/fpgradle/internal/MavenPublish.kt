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
import org.gradle.api.Project
import org.gradle.api.credentials.PasswordCredentials
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.kotlin.dsl.*

class MavenPublish: FPPlugin() {
    override fun addPlugins() = listOf(MavenPublishPlugin::class)

    override fun Project.onPluginPostInitBeforeDeps() {
        val mvn = mc.publish.maven
        with(publishing) {
            publications {
                create<MavenPublication>("maven") {
                    from(components.getByName("java"))

                    if (mc.api.packages.get().isNotEmpty() || mc.api.packagesNoRecurse.get().isNotEmpty())
                        artifact(tasks.named("apiJar"))

                    groupId = mvn.group.get()
                    artifactId = mvn.artifact.get()
                    version = mvn.version.get()
                }
            }
            repositories {
                if (!mvn.repoUrl.isPresent)
                    return@repositories

                maven {
                    url = mvn.repoUrl.get()
                    name = mvn.repoName.get()
                    val user = mvn.userEnv.map { System.getenv(it) }.orNull
                    val pass = mvn.passEnv.map { System.getenv(it) }.orNull
                    if (user != null && pass != null) {
                        credentials {
                            username = user
                            password = pass
                        }
                    } else {
                        credentials(PasswordCredentials::class)
                    }
                }
            }
        }
    }
}