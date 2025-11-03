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
import com.falsepattern.fpgradle.internal.Stubs.Companion.JAR_STUB_TASK
import com.gtnewhorizons.retrofuturagradle.mcp.ReobfuscatedJar
import org.gradle.api.Project
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.jar.Manifest
import javax.inject.Inject

abstract class JarInJar: FPPlugin() {
    @get:Inject
    abstract val archiveOperations: ArchiveOperations
    override fun Project.onPluginInit() {
        tasks.addRule("mergeJarPreReobf") {
            if (this != "mergeJarPreReobf") {
                return@addRule
            }
            val jarTask = tasks.named<Jar>(JAR_STUB_TASK)
            val srcJar = jarTask.flatMap { it.archiveFile }
            val specs = fp_ctx_internal.mergedJarExcludeSpecs
            val textResourceFactory = resources.text
            val mergedManifest = srcJar.map { jar ->
                val output = Manifest()
                val theFile = archiveOperations.zipTree(jar).find { file -> file.name == "MANIFEST.MF" }
                theFile?.inputStream()?.use { output.read(it) }
                val out = ByteArrayOutputStream()
                output.write(out)
                val str = String(out.toByteArray(), StandardCharsets.UTF_8)
                textResourceFactory.fromString(str)
            }
            val mergeJarTask = tasks.register<Jar>(this) {
                dependsOn(JAR_STUB_TASK)
                group = "falsepattern"
                description = "Merges nested jars before reobfuscation"
                archiveClassifier.set("merged-pre-reobf")
                destinationDirectory.set(layout.buildDirectory.dir("tmp/fpgradle-libs"))

                from(archiveOperations.zipTree(srcJar)) {
                    exclude { file ->
                        specs.get().all { spec -> spec.apply(file) }
                    }
                }
                manifest {
                    from(mergedManifest)
                }
            }
            tasks.named<ReobfuscatedJar>("reobfJar") {
                @Suppress("UNCHECKED_CAST")
                inputJar = mergeJarTask.flatMap { it.archiveFile }
                dependsOn(mergeJarTask)
            }
        }
    }
}