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

import com.falsepattern.fpgradle.ext
import com.falsepattern.fpgradle.mc
import org.gradle.api.credentials.PasswordCredentials
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.*

class MavenPublish(ctx: ConfigurationContext): InitTask {
    private val project = ctx.project
    private val publishing = project.ext<PublishingExtension>()

    override fun postInit() = with(project) {
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
                    val user = System.getenv(mvn.userEnv.get())
                    val pass = System.getenv(mvn.passEnv.get())
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