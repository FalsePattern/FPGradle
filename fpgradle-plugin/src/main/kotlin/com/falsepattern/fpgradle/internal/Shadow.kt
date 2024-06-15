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

import com.falsepattern.fpgradle.mc
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.gtnewhorizons.retrofuturagradle.mcp.ReobfuscatedJar
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.component.ConfigurationVariantDetails
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named

class Shadow(ctx: ConfigurationContext): InitTask {
    private val project = ctx.project
    private val manifestAttributes = ctx.manifestAttributes

    override fun init() {
        setupShadowJarTask()
    }

    override fun postInit() {
        if (project.configurations.getByName("shadowImplementation").dependencies.isEmpty())
            return

        setupConfigCoupling()
        setupArtifactRouting()
    }

    private fun setupShadowJarTask() = with(project) {
        configurations {
            create("shadowImplementation")
        }
        val shadowImplementation = configurations.named("shadowImplementation")
        val empty = shadowImplementation.map { it.dependencies.isEmpty() }
        tasks {
            named<ShadowJar>("shadowJar").configure {
                manifest {
                    attributes(manifestAttributes.get())
                }

                if (mc.shadow.minimize.get())
                    minimize()

                configurations {
                    add(shadowImplementation.get())
                }

                archiveClassifier = "dev"

                if (mc.shadow.relocate.get()) {
                    relocationPrefix = "${mc.mod.rootPkg.get()}.shadow"
                    isEnableRelocation = true
                }
                dependsOn("removeStub")

                onlyIf {
                    !empty.get()
                }
            }

            named<Jar>("jar").configure {
                dependsOn("removeStub")
                if (!empty.get())
                    archiveClassifier = "dev-preshadow"
            }

            named<ReobfuscatedJar>("reobfJar").configure {
                if (!empty.get()) {
                    inputJar = named<ShadowJar>("shadowJar").flatMap(AbstractArchiveTask::getArchiveFile)
                    dependsOn("shadowJar")
                }
            }
        }
    }

    private fun setupConfigCoupling() = with(project) {
        configurations {
            for (classpath in classpaths) {
                named(classpath).configure {
                    extendsFrom(getByName("shadowImplementation"))
                }
            }
        }
    }

    private fun setupArtifactRouting() = with(project) {
        configurations {
            getByName("runtimeElements").outgoing.artifacts.clear()
            getByName("apiElements").outgoing.artifacts.clear()
            getByName("runtimeElements").outgoing.artifact(tasks.named<ShadowJar>("shadowJar"))
            getByName("apiElements").outgoing.artifact(tasks.named<ShadowJar>("shadowJar"))

            val javaComponent = components.findByName("java")!! as AdhocComponentWithVariants
            javaComponent.withVariantsFromConfiguration(getByName("shadowRuntimeElements"), ConfigurationVariantDetails::skip)
        }
    }

    companion object {
        private val classpaths = listOf(
            "compileClasspath",
            "runtimeClasspath",
            "testCompileClasspath",
            "testRuntimeClasspath"
        )
    }
}
