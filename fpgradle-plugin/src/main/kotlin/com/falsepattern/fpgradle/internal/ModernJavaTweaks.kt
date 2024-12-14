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

import com.falsepattern.fpgradle.*
import com.falsepattern.fpgradle.FPMinecraftProjectExtension.Java.Compatibility.*
import com.gtnewhorizons.retrofuturagradle.ObfuscationAttribute
import com.gtnewhorizons.retrofuturagradle.mcp.MCPTasks
import com.gtnewhorizons.retrofuturagradle.mcp.RemapSourceJarTask
import com.gtnewhorizons.retrofuturagradle.minecraft.RunMinecraftTask
import com.gtnewhorizons.retrofuturagradle.shadow.de.undercouch.gradle.tasks.download.Download
import com.gtnewhorizons.retrofuturagradle.util.Distribution
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.kotlin.dsl.*
import java.nio.charset.StandardCharsets
import org.gradle.api.plugins.JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME as ANNOTATION_PROCESSOR
import org.gradle.api.plugins.JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME as COMPILE_ONLY

class ModernJavaTweaks: FPPlugin() {
    override fun Project.onPluginInit() {
        tweakMappingGenerator()
        minecraft.injectMissingGenerics = true
    }

    override fun Project.onPluginPostInitBeforeDeps() {
        when(mc.java.compatibility.get()) {
            LegacyJava -> {
                setToolchainVersionLegacy()
            }
            Jabel -> {
                setToolchainVersionJabel()
            }
            ModernJava -> {
                setToolchainVersionModern()
                setupJavaConfigsModern()
                swapLWJGLVersionModern()
                injectLWJGL3ifyModern()
                for (it in McRun.values()) {
                    modifyMinecraftRunTaskModern(it)
                }
            }
        }
    }

    override fun Project.onPluginPostInitAfterDeps() {
        tasks.withType<JavaCompile>().configureEach {
            options.encoding = StandardCharsets.UTF_8.name()
        }
    }

    private fun Project.setToolchainVersionLegacy() {
        java.toolchain.languageVersion = JavaLanguageVersion.of(JavaVersion.VERSION_1_8.majorVersion)
        java.toolchain.vendor = JvmVendorSpec.ADOPTIUM
    }

    private fun Project.setToolchainVersionJabel() {
        repositories {
            exclusiveContent {
                forRepositories(repositories.mavenCentral {
                    name = "mavenCentral_java8Unsupported"
                })
                filter {
                    includeGroup("me.eigenraven.java8unsupported")
                }
            }
            maven {
                name = "horizon"
                url = uri("https://mvn.falsepattern.com/horizon/")
                content {
                    includeModule("com.github.bsideup.jabel", "jabel-javac-plugin")
                }
            }
        }
        dependencies {
            add(ANNOTATION_PROCESSOR, JABEL)
            add(COMPILE_ONLY, JABEL) {
                isTransitive = false
            }
            // Workaround for https://github.com/bsideup/jabel/issues/174
            add(ANNOTATION_PROCESSOR, "net.java.dev.jna:jna-platform:5.13.0")
            // Allow using jdk.unsupported classes like sun.misc.Unsafe in the compiled code, working around
            // JDK-8206937.
            add(MCPTasks.PATCHED_MINECRAFT_CONFIGURATION_NAME, "me.eigenraven.java8unsupported:java-8-unsupported-shim:1.0.0")
        }
        java.toolchain.languageVersion = mc.java.version.map { JavaLanguageVersion.of(it.majorVersion) }
        java.toolchain.vendor = JvmVendorSpec.ADOPTIUM

        val jabelCompiler = toolchains.compilerFor(java.toolchain)
        tasks.withType<JavaCompile>().configureEachFiltered {
            sourceCompatibility = mc.java.version.map { it.majorVersion }.get()
            options.release = 8
            javaCompiler = jabelCompiler
        }
    }

    private fun Project.setToolchainVersionModern() {
        java.toolchain.languageVersion = mc.java.version.map { JavaLanguageVersion.of(it.majorVersion) }
        java.toolchain.vendor = JvmVendorSpec.ADOPTIUM
        val modernCompiler = toolchains.compilerFor(java.toolchain)
        project.tasks.withType<JavaCompile>().configureEachFiltered {
            sourceCompatibility = mc.java.version.map { it.majorVersion }.get()
            targetCompatibility = mc.java.version.map { it.majorVersion }.get()
            javaCompiler = modernCompiler
        }
    }

    private fun Project.setupJavaConfigsModern() {
        with(configurations) {
            create(MODERN_PATCH_DEPS) {
                attributes.attribute(ObfuscationAttribute.OBFUSCATION_ATTRIBUTE, ObfuscationAttribute.getMcp(objects))
            }
            val modernJavaPatchDependencies = create(MODERN_PATCH_DEPS_COMPILE_ONLY) {
                attributes.attribute(ObfuscationAttribute.OBFUSCATION_ATTRIBUTE, ObfuscationAttribute.getMcp(objects))
            }
            getByName("compileOnly") {
                extendsFrom(modernJavaPatchDependencies)
            }
        }
    }

    private fun Project.swapLWJGLVersionModern() {
        with(configurations) {
            getByName("compileOnly") {
                extendsFrom(minecraftTasks.lwjgl3Configuration)
            }
            getByName("compileClasspath") {
                exclude(mapOf(Pair("group", "org.lwjgl.lwjgl")))
            }
        }
    }

    private fun Project.injectLWJGL3ifyModern() {
        repositories {
            maven {
                name = "horizon_3ify"
                url = uri("https://mvn.falsepattern.com/horizon/")
                content {
                    includeGroup("com.gtnewhorizons")
                    includeGroup("com.gtnewhorizons.retrofuturabootstrap")
                    includeGroup("com.github.GTNewHorizons")
                }
            }
        }

        val asmVersion = provider { PackageRegistry.MODERN_JAVA_ASM_VERSION }
        val rfbVersion = provider { PackageRegistry.RFB_VERSION }
        val lwjgl3ifyVersion = provider { PackageRegistry.LWJGL3IFY_VERSION }

        val lwjgl3ify = lwjgl3ifyVersion.map { "com.github.GTNewHorizons:lwjgl3ify:$it" }

        dependencies {
            addProvider("devOnlyNonPublishable", lwjgl3ify.map { "$it:dev" })
            addProvider("obfuscatedRuntimeClasspath", lwjgl3ify)

            addProvider<_, ExternalModuleDependency>(MODERN_PATCH_DEPS_COMPILE_ONLY, rfbVersion.map { "com.gtnewhorizons.retrofuturabootstrap:RetroFuturaBootstrap:$it" }) { isTransitive = false }
            addProvider(MODERN_PATCH_DEPS_COMPILE_ONLY, asmVersion.map { "org.ow2.asm:asm:$it" })
            addProvider(MODERN_PATCH_DEPS_COMPILE_ONLY, asmVersion.map { "org.ow2.asm:asm-commons:$it" })
            addProvider(MODERN_PATCH_DEPS_COMPILE_ONLY, asmVersion.map { "org.ow2.asm:asm-tree:$it" })
            addProvider(MODERN_PATCH_DEPS_COMPILE_ONLY, asmVersion.map { "org.ow2.asm:asm-analysis:$it" })
            addProvider(MODERN_PATCH_DEPS_COMPILE_ONLY, asmVersion.map { "org.ow2.asm:asm-util:$it" })
            addProvider(MODERN_PATCH_DEPS_COMPILE_ONLY, provider { "org.ow2.asm:asm-deprecated:7.1" })
            addProvider(MODERN_PATCH_DEPS_COMPILE_ONLY, provider { PackageRegistry.MODERN_JAVA_COMMONS_LANG })
            addProvider(MODERN_PATCH_DEPS_COMPILE_ONLY, provider { PackageRegistry.MODERN_JAVA_COMMONS_COMPRESS })
            addProvider(MODERN_PATCH_DEPS_COMPILE_ONLY, provider { PackageRegistry.MODERN_JAVA_COMMONS_IO })
            addProvider<_, ExternalModuleDependency>(MODERN_PATCH_DEPS, lwjgl3ify.map { "$it:forgePatches" }) { isTransitive = false }
        }
    }

    private fun Project.tweakMappingGenerator() {
        if (project.name == "fpgradle-examplemod1") {
            mappingGenerator.sources = listOf(
                listOf("yarn", PackageRegistry.LEGACY_YARN_VERSION),
                listOf("mcp", "1.8.9", "stable_22", "parameters"),
                listOf("mcp", "1.12", "stable_39", "parameters"),
                listOf("mcp", "1.7.10", "stable_12", "methodComments"),
                listOf("csv", "https://raw.githubusercontent.com/LegacyModdingMC/ExtraMappings/master/params.csv"),
                listOf("csv", "https://raw.githubusercontent.com/FalsePattern/srgmap/master/params.csv"),
            )
        } else {
            val paramsFile = project.layout.buildDirectory.file("extra-mappings/parameters.csv")
            val dl = tasks.register<Download>("downloadExtraMappings")
            dl.configure {
                src("https://mvn.falsepattern.com/filedrop/parameters.csv")
                dest(paramsFile)
                onlyIfModified(true)
                overwrite(true)
                quiet(true)
            }
            project.minecraft.extraParamsCsvs.from(paramsFile)
            tasks.named<RemapSourceJarTask>("remapDecompiledJar").configure {
                dependsOn(dl)
            }
        }
    }

    private fun Project.modifyMinecraftRunTaskModern(mcRun: McRun) {
        tasks.named<RunMinecraftTask>(mcRun.taskName).configure {
            lwjglVersion = 3
            javaLauncher = toolchains.launcherFor(java.toolchain)
            extraJvmArgs.addAll(javaArgs)
            if (mcRun.obfuscated) {
                mainClass = when(mcRun.side) {
                    Distribution.CLIENT -> "com.gtnewhorizons.retrofuturabootstrap.Main"
                    Distribution.DEDICATED_SERVER -> "me.eigenraven.lwjgl3ify.rfb.entry.ServerMain"
                }
            } else {
                when(mcRun.side) {
                    Distribution.CLIENT -> systemProperty("gradlestart.bouncerClient", "com.gtnewhorizons.retrofuturabootstrap.Main")
                    Distribution.DEDICATED_SERVER -> systemProperty("gradlestart.bouncerServer", "com.gtnewhorizons.retrofuturabootstrap.Main")
                }
            }
            systemProperty("java.system.class.loader", "com.gtnewhorizons.retrofuturabootstrap.RfbSystemClassLoader")
            systemProperty("file.encoding", "UTF-8")
            systemProperty("java.security.manager", "allow")

            val modernJavaPatchDependencies = configurations.getByName(MODERN_PATCH_DEPS)

            val oldClasspath = classpath
            setClasspath(files())
            classpath(modernJavaPatchDependencies)
            if (mcRun.side == Distribution.CLIENT) {
                classpath(minecraftTasks.lwjgl3Configuration)
            }
            classpath(oldClasspath)
        }
    }

    companion object {
        private val OPENED_PACKAGES = listOf(
            "java.base/jdk.internal.loader=ALL-UNNAMED",
            "java.base/java.net=ALL-UNNAMED",
            "java.base/java.nio=ALL-UNNAMED",
            "java.base/java.io=ALL-UNNAMED",
            "java.base/java.lang=ALL-UNNAMED",
            "java.base/java.lang.ref=ALL-UNNAMED",
            "java.base/java.util.concurrent.locks=ALL-UNNAMED",
            "java.base/java.lang.reflect=ALL-UNNAMED",
            "java.base/java.text=ALL-UNNAMED",
            "java.base/java.util=ALL-UNNAMED",
            "java.base/jdk.internal.reflect=ALL-UNNAMED",
            "java.base/sun.nio.ch=ALL-UNNAMED",
            "jdk.naming.dns/com.sun.jndi.dns=ALL-UNNAMED,java.naming",
            "java.desktop/sun.awt=ALL-UNNAMED",
            "java.desktop/sun.awt.image=ALL-UNNAMED",
            "java.desktop/com.sun.imageio.plugins.png=ALL-UNNAMED",
            "jdk.dynalink/jdk.dynalink.beans=ALL-UNNAMED",
            "java.sql.rowset/javax.sql.rowset.serial=ALL-UNNAMED",
        )

        private val ADDED_MODULES = listOf<String>(

        )

        private val MODERN_JAVA_ARGS_EXTRA = listOf(
            "-XX:+UseZGC",
            "-XX:+ZGenerational"
        )

        private val javaArgs = MODERN_JAVA_ARGS_EXTRA +
                ADDED_MODULES.flatMap { listOf("--add-modules", it) } +
                OPENED_PACKAGES.flatMap { listOf("--add-opens", it) }

        private val MODERN_PATCH_DEPS = "modernJavaPatchDeps"
        private val MODERN_PATCH_DEPS_COMPILE_ONLY = "modernJavaPatchDepsCompileOnly"

        private val JABEL = "com.github.bsideup.jabel:jabel-javac-plugin:1.0.1"

        private val BLACKLISTED_TASKS = listOf("compileMcLauncherJava", "compilePatchedMcJava")

        private fun <T: Task> TaskCollection<T>.configureEachFiltered(action: T.() -> Unit) {
            this.configureEach {
                if (!BLACKLISTED_TASKS.contains(name))
                    action(this)
            }
        }
    }
}
