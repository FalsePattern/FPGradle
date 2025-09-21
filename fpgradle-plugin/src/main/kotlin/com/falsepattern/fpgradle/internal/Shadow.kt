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

package com.falsepattern.fpgradle.internal

import com.falsepattern.fpgradle.FPPlugin
import com.falsepattern.fpgradle.internal.Stubs.Companion.JAR_STUB_TASK
import com.falsepattern.fpgradle.manifestAttributes
import com.falsepattern.fpgradle.mc
import com.falsepattern.jtweaker.RemoveStubsJar
import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.component.ConfigurationVariantDetails
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*

class Shadow: FPPlugin() {

    override fun Project.addPlugins() = listOf(ShadowPlugin::class)

    override fun Project.onPluginInit() {
        setupShadowJarTask()
    }

    private fun Project.setupShadowJarTask() {
        val shadowImplementation = configurations.maybeCreate("shadowImplementation")
        shadowImplementation.isCanBeConsumed = false
        shadowImplementation.isCanBeResolved = true
        listOf("compileClasspath", "testCompileClasspath", "testRuntimeClasspath").forEach {
            configurations.getByName(it)
                .extendsFrom(shadowImplementation)
        }
        val empty = provider { shadowImplementation.dependencies.isEmpty() }
        tasks {
            val shadowJar = named<ShadowJar>("shadowJar")
            shadowJar.configure {
                manifest {
                    attributes(manifestAttributes.get())
                }

                if (mc.shadow.minimize.get())
                    minimize()

                configurations = listOf(shadowImplementation)

                archiveClassifier = "dev-prestub"

                if (mc.shadow.relocate.get()) {
                    relocationPrefix = "${mc.mod.rootPkg.get()}.shadow"
                    enableAutoRelocation = true
                }

                onlyIf {
                    !empty.get()
                }
            }

            named<Jar>("jar").configure {
                if (!empty.get())
                    archiveClassifier = "dev-preshadow"
            }

            named<RemoveStubsJar>(JAR_STUB_TASK).configure {
                if (!empty.get()) {
                    inputFile = shadowJar.flatMap(AbstractArchiveTask::getArchiveFile)
                    dependsOn("shadowJar")
                }
            }
        }
        afterEvaluate {
            val javaComponent = components.findByName("java") as AdhocComponentWithVariants
            val shadowRuntimeElements = configurations.getByName("shadowRuntimeElements")
            javaComponent.withVariantsFromConfiguration(shadowRuntimeElements, ConfigurationVariantDetails::skip)
        }
    }
}
