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

import com.falsepattern.fpgradle.FPMinecraftProjectExtension.Java.Compatibility
import com.falsepattern.fpgradle.FPMinecraftProjectExtension.Java.JvmDowngraderShade
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.jvm.toolchain.JvmVendorSpec
import javax.inject.Inject

abstract class JarInJarConfigSpec @Inject constructor(project: Project) {
    abstract val javaCompatibility: Property<Compatibility>
    abstract val javaVersion: Property<JavaVersion>
    abstract val javaVendor: Property<JvmVendorSpec>
    abstract val jvmDowngraderShade: Property<JvmDowngraderShade>
    abstract val jvmDowngraderShadePackage: Property<String>
    abstract val artifactGroup: Property<String>
    abstract val artifactName: Property<String>
    abstract val artifactVersion: Property<String>
    var dependsOnMinecraft: Boolean
    var dependsOnMain: Boolean
    init {
        with(project) {
            val mc = project.mc
            javaCompatibility.convention(mc.java.compatibility)
            val javaGenerated = javaCompatibility.map { when(it) {
                Compatibility.LegacyJava -> JavaVersion.VERSION_1_8
                Compatibility.JvmDowngrader -> JavaVersion.VERSION_21
                Compatibility.ModernJava -> JavaVersion.VERSION_21
            } }
            javaVersion.convention(javaCompatibility.flatMap { thisComp ->
                mc.java.compatibility.flatMap { mcComp ->
                    if (mcComp == thisComp) {
                        mc.java.version
                    } else {
                        javaGenerated
                    }
                }
            })
            javaVendor.convention(mc.java.vendor)
            jvmDowngraderShade.convention(mc.java.jvmDowngraderShade)
            jvmDowngraderShadePackage.convention(mc.java.jvmDowngraderShadePackage)
            artifactGroup.convention(mc.publish.maven.group)
            artifactName.convention(mc.publish.maven.artifact)
            artifactVersion.convention(mc.publish.maven.version)
            dependsOnMinecraft = true
            dependsOnMain = true
        }
    }

    val legacy = Compatibility.LegacyJava
    val jvmDowngrader = Compatibility.JvmDowngrader
    val modern = Compatibility.ModernJava

    val doNotShade = JvmDowngraderShade.DoNotShade
    val projectIsLgpl21PlusCompatible = JvmDowngraderShade.ProjectIsLgpl21PlusCompatible
    val iWillPublishTheUnshadedJarForLgpl21PlusCompliance = JvmDowngraderShade.IWillPublishTheUnshadedJarForLgpl21PlusCompliance
}