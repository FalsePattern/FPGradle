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
import com.falsepattern.fpgradle.fp_ctx_internal
import com.gtnewhorizons.retrofuturagradle.mcp.ReobfuscatedJar
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.jar.Manifest

class JarInJar: FPPlugin() {
    override fun Project.onPluginInit() {
        tasks.addRule("mergeJarPreReobf") {
            if (this != "mergeJarPreReobf") {
                return@addRule
            }
            val jarTask = tasks.named<Jar>("jar")
            val srcJar = jarTask.map { it.outputs.files.map { it2 -> zipTree(it2) } }
            val specs = fp_ctx_internal.mergedJarExcludeSpecs
            val textResourceFactory = resources.text
            val mergedManifest = srcJar.map { trees ->
                val output = Manifest()
                trees.forEach { tree ->
                    val file = tree.find { file -> file.name == "MANIFEST.MF" } ?: return@forEach
                    file.inputStream().use { output.read(it) }
                }
                val out = ByteArrayOutputStream()
                output.write(out)
                val str = String(out.toByteArray(), StandardCharsets.UTF_8)
                textResourceFactory.fromString(str)
            }
            val mergeJarTask = tasks.register<Jar>(this) {
                dependsOn(jarTask)
                group = "falsepattern"
                description = "Merges nested jars before reobfuscation"
                archiveClassifier.set("merged-pre-reobf")

                from(srcJar) {
                    exclude { file ->
                        println(file.relativePath.pathString)
                        specs.get().all { spec -> spec.apply(file) }
                    }
                }
                manifest {
                    from(mergedManifest)
                }
            }
            tasks.named<ReobfuscatedJar>("reobfJar") {
                @Suppress("UNCHECKED_CAST")
                setInputJarFromTask(mergeJarTask as TaskProvider<org.gradle.jvm.tasks.Jar>)
                dependsOn(mergeJarTask)
            }
        }
    }
}