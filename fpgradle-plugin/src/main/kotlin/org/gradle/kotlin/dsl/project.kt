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

package org.gradle.kotlin.dsl

import com.falsepattern.fpgradle.FPMinecraftProjectExtension
import com.falsepattern.fpgradle.JarInJarConfigSpec
import com.falsepattern.fpgradle.fp_ctx_internal
import com.falsepattern.fpgradle.internal.JvmDG.Companion.setupDowngradeTasksForSet
import com.falsepattern.fpgradle.internal.ModernJavaTweaks.Companion.injectLwjgl3ifyForSet
import com.falsepattern.fpgradle.internal.Stubs
import com.falsepattern.fpgradle.sourceSets
import com.falsepattern.fpgradle.toolchains
import com.falsepattern.jtweaker.RemoveStubsJar
import com.gtnewhorizons.retrofuturagradle.mcp.ReobfuscatedJar
import org.gradle.api.Project
import org.gradle.api.file.FileTreeElement
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.language.jvm.tasks.ProcessResources
import java.util.function.Function

fun Project.jarInJar_fp(
    sourceSetName: String,
    configureAction: JarInJarConfigSpec.() -> Unit = {},
): SourceSet {
    val spec = objects.newInstance<JarInJarConfigSpec>(this)
    configureAction.invoke(spec)
    val dependsOnMinecraft = spec.dependsOnMinecraft
    val dependsOnMain = spec.dependsOnMain
    val sourceSet = sourceSets.create(sourceSetName) {
        if (dependsOnMinecraft) {
            compileClasspath += sourceSets["patchedMc"].output
        }
        if (dependsOnMain) {
            compileClasspath += sourceSets["main"].output
        }
    }

    tasks.named<JavaCompile>(sourceSet.compileJavaTaskName) {
        javaCompiler = toolchains.compilerFor {
            languageVersion.set(spec.javaVersion.map { JavaLanguageVersion.of(it.majorVersion) })
            vendor.set(spec.javaVendor)
        }
    }

    afterEvaluate {
        injectLwjgl3ifyForSet(spec.javaVersion, spec.javaCompatibility.get(), sourceSet)
    }

    configurations.named(sourceSet.compileClasspathConfigurationName) {
        if (dependsOnMain) {
            extendsFrom(configurations.getByName("compileClasspath"))
        }
    }
    configurations.named(sourceSet.runtimeClasspathConfigurationName) {
        if (dependsOnMain) {
            extendsFrom(configurations.getByName("runtimeClasspath"))
        }
    }
    configurations.named(sourceSet.annotationProcessorConfigurationName) {
        if (dependsOnMain) {
            extendsFrom(configurations.getByName("annotationProcessor"))
        }
    }

    val thisJar = tasks.register<Jar>(sourceSet.jarTaskName) {
        from(sourceSet.output)
        archiveBaseName = spec.artifactName
        archiveVersion = spec.artifactVersion
        archiveClassifier = if(dependsOnMinecraft) "dev-prestub" else "prestub"
        destinationDirectory.set(layout.buildDirectory.dir("tmp/fpgradle-libs"))
    }

    val removeStubsTaskName = "${sourceSet.jarTaskName}RemoveStubs"

    val stubJar = tasks.register<RemoveStubsJar>(removeStubsTaskName) {
        dependsOn(thisJar)
        inputFile = thisJar.flatMap { it.archiveFile }
        archiveBaseName = spec.artifactName
        archiveVersion = spec.artifactVersion
        archiveClassifier = if (dependsOnMinecraft) "dev" else ""
    }

    val artifactFileName = stubJar.flatMap { it.archiveFileName }

    val artifactPath = spec.artifactGroup.flatMap { group -> spec.artifactName.flatMap { name -> spec.artifactVersion.map { version ->
        "META-INF/falsepatternlib_repo/${group.replace('.', '/')}/$name/$version"
    } } }

    val artifactFullPath = artifactPath.flatMap { path -> artifactFileName.map { name -> "$path/$name" }}

    val isJvmDG = spec.javaCompatibility.map { it == FPMinecraftProjectExtension.Java.Compatibility.JvmDowngrader }

    if (dependsOnMinecraft) {
        val reobfInputs = configurations.register("reobf${sourceSetName}Inputs") {
            extendsFrom(configurations.getByName(sourceSet.compileClasspathConfigurationName))
        }

        val name = "reobf${sourceSet.jarTaskName}"

        val reobfThisJar = tasks.named<ReobfuscatedJar>(name) {
            dependsOn(stubJar, tasks.named(Stubs.JAR_STUB_TASK))
            referenceClasspath.from(sourceSets.named("main").map { it.output }, reobfInputs)
            @Suppress("UNCHECKED_CAST")
            setInputJarFromTask(stubJar as TaskProvider<org.gradle.jvm.tasks.Jar>)
        }

        val reobfOutputJar = reobfThisJar.flatMap { it.archiveFile }

        tasks.named<Jar>("mergeJarPreReobf") {
            dependsOn(name)
            from(reobfOutputJar) {
                into(artifactPath)
            }
        }

        fp_ctx_internal.mergedJarExcludeSpecs.add(artifactFullPath.map { theFullPath -> Function<FileTreeElement, Boolean> { it.relativePath.pathString == theFullPath } })
    }


    val outputTask = if (isJvmDG.get())
        setupDowngradeTasksForSet(spec.javaCompatibility, spec.jvmDowngraderShade, spec.artifactName, spec.artifactVersion, if (spec.dependsOnMinecraft) "dev" else "", spec.jvmDowngraderShadePackage, spec.dependsOnMinecraft, sourceSet)
    else stubJar
    tasks.named<ProcessResources>("processResources") {
        dependsOn(outputTask)
        from(outputTask.map { it.archiveFile }) {
            into(artifactPath)
        }
    }



    return sourceSet
}