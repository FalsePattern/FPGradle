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

package com.falsepattern.fpgradle

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.jvm.toolchain.JavaCompiler
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import javax.inject.Inject

abstract class JarInJarConfigSpec @Inject constructor(project: Project) {
    abstract val javaCompiler: Property<JavaCompiler>
    abstract val javaVendor: Property<JvmVendorSpec>
    abstract val artifactGroup: Property<String>
    abstract val artifactName: Property<String>
    abstract val artifactVersion: Property<String>
    var dependsOnMinecraft: Boolean
    var dependsOnMain: Boolean
    init {
        with(project) {
            val mc = project.mc
            javaCompiler.convention(javaToolchains.compilerFor {
                languageVersion.set(mc.java.version.map { JavaLanguageVersion.of(it.majorVersion) })
                vendor.set(mc.java.vendor)
            })
            javaVendor.convention(mc.java.vendor)
            artifactGroup.convention(mc.publish.maven.group)
            artifactName.convention(mc.publish.maven.artifact)
            artifactVersion.convention(mc.publish.maven.version)
            dependsOnMinecraft = true
            dependsOnMain = true
        }
    }
}