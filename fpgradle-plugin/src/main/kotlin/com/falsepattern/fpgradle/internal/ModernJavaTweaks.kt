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

import com.falsepattern.fpgradle.*
import com.falsepattern.fpgradle.FPMinecraftProjectExtension.Java.Compatibility
import com.falsepattern.fpgradle.FPMinecraftProjectExtension.Java.Compatibility.*
import com.gtnewhorizons.retrofuturagradle.ObfuscationAttribute
import com.gtnewhorizons.retrofuturagradle.mcp.MCPTasks
import com.gtnewhorizons.retrofuturagradle.mcp.RemapSourceJarTask
import com.gtnewhorizons.retrofuturagradle.minecraft.RunMinecraftTask
import com.gtnewhorizons.retrofuturagradle.util.Distribution
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.kotlin.dsl.*
import java.nio.charset.StandardCharsets
import org.gradle.api.plugins.JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME as ANNOTATION_PROCESSOR
import org.gradle.api.plugins.JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME as COMPILE_ONLY

class ModernJavaTweaks: FPPlugin() {
    override fun Project.onPluginInit() {
        with(configurations) {
            create(MODERN_PATCH_DEPS) {
                attributes.attribute(ObfuscationAttribute.OBFUSCATION_ATTRIBUTE, ObfuscationAttribute.getMcp(objects))
            }
            create(MODERN_PATCH_DEPS_OBF) {
                attributes.attribute(ObfuscationAttribute.OBFUSCATION_ATTRIBUTE, ObfuscationAttribute.getSrg(objects))
            }
        }

        tweakMappingGenerator()
        minecraft.injectMissingGenerics = true
    }

    override fun Project.onPluginPostInitBeforeDeps() {
        val compat = mc.java.compatibility.get()
        injectLwjgl3ifyForSetInternal(compat, mc.java.version, mc.java.vendor, null)
        when(compat) {
            LegacyJava -> {
                McRun.standardNonObf().forEach { createModernCloneFor(tasks.named<RunMinecraftTask>(it.taskName), it.side) }
                for (it in McRun.modern()) {
                    modifyMinecraftRunTaskModern(it)
                }
            }
            Jabel -> {
                McRun.standardNonObf().forEach { createModernCloneFor(tasks.named<RunMinecraftTask>(it.taskName), it.side) }
                for (it in McRun.modern()) {
                    modifyMinecraftRunTaskModern(it)
                }
            }
            ModernJava -> {
                for (it in McRun.standard()) {
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
            val unpack = tasks.register<Copy>("unpackExtraMappings") {
                val pluginJar = provider {FPPlugin::class.java.getResource("")!!.file.split('!')[0]}
                from(pluginJar.map { jar -> project.resources.text.fromArchiveEntry(jar, "/fpgradle/parameters.csv").asFile()})
                into(project.layout.buildDirectory.dir("extra-mappings"))
            }
            project.minecraft.extraParamsCsvs.from(paramsFile)
            tasks.named<RemapSourceJarTask>("remapDecompiledJar").configure {
                dependsOn(unpack)
            }
        }
    }

    private fun Project.modifyMinecraftRunTaskModern(mcRun: McRun) {
        tasks.named<RunMinecraftTask>(mcRun.taskName).configure {
            lwjglVersion = 3
            javaLauncher = toolchains.launcherFor {
                languageVersion = mc.java.modernRuntimeVersion.map { JavaLanguageVersion.of(it.majorVersion) }
                vendor = mc.java.vendor
            }
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

            val modernJavaPatchDependencies = configurations.getByName(MODERN_PATCH_DEPS)
            val modernJavaPatchDependenciesObf = configurations.getByName(MODERN_PATCH_DEPS_OBF)

            val oldClasspath = classpath
            setClasspath(files())
            if (mcRun.obfuscated) {
                classpath(modernJavaPatchDependenciesObf)
            } else {
                classpath(modernJavaPatchDependencies)
            }
            if (mcRun.side == Distribution.CLIENT) {
                classpath(minecraftTasks.lwjgl3Configuration)
            }
            classpath(oldClasspath)
        }
    }

    private fun Project.createModernCloneFor(oldTaskProvider: TaskProvider<RunMinecraftTask>, distribution: Distribution) {
        val newTask = project.tasks.register<RunMinecraftTask>(oldTaskProvider.name + "ModernJava", distribution)
        newTask.configure {
            val oldTask = oldTaskProvider.get()
            setup(project)
            group = "Modded Minecraft"
            description = oldTask.description + " with modern java"
            dependsOn(*oldTask.dependsOn.toTypedArray())
            username = oldTask.username
            userUUID = oldTask.userUUID
            classpath += oldTask.classpath
            mainClass = oldTask.mainClass
            tweakClasses.addAll(oldTask.tweakClasses)
        }
    }

    companion object {
        private val OPENED_PACKAGES = listOf(
            "java.base/java.io=ALL-UNNAMED",
            "java.base/java.lang.invoke=ALL-UNNAMED",
            "java.base/java.lang.ref=ALL-UNNAMED",
            "java.base/java.lang.reflect=ALL-UNNAMED",
            "java.base/java.lang=ALL-UNNAMED",
            "java.base/java.net.spi=ALL-UNNAMED",
            "java.base/java.net=ALL-UNNAMED",
            "java.base/java.nio.channels=ALL-UNNAMED",
            "java.base/java.nio.charset=ALL-UNNAMED",
            "java.base/java.nio.file=ALL-UNNAMED",
            "java.base/java.nio=ALL-UNNAMED",
            "java.base/java.text=ALL-UNNAMED",
            "java.base/java.time.chrono=ALL-UNNAMED",
            "java.base/java.time.format=ALL-UNNAMED",
            "java.base/java.time.temporal=ALL-UNNAMED",
            "java.base/java.time.zone=ALL-UNNAMED",
            "java.base/java.time=ALL-UNNAMED",
            "java.base/java.util.concurrent.atomics=ALL-UNNAMED",
            "java.base/java.util.concurrent.locks=ALL-UNNAMED",
            "java.base/java.util.jar=ALL-UNNAMED",
            "java.base/java.util.zip=ALL-UNNAMED",
            "java.base/java.util=ALL-UNNAMED",
            "java.base/jdk.internal.loader=ALL-UNNAMED",
            "java.base/jdk.internal.misc=ALL-UNNAMED",
            "java.base/jdk.internal.ref=ALL-UNNAMED",
            "java.base/jdk.internal.reflect=ALL-UNNAMED",
            "java.base/sun.nio.ch=ALL-UNNAMED",
            "java.desktop/com.sun.imageio.plugins.png=ALL-UNNAMED",
            "java.desktop/sun.awt.image=ALL-UNNAMED",
            "java.desktop/sun.awt=ALL-UNNAMED",
            "java.sql.rowset/javax.sql.rowset.serial=ALL-UNNAMED",
            "jdk.dynalink/jdk.dynalink.beans=ALL-UNNAMED",
            "jdk.naming.dns/com.sun.jndi.dns=ALL-UNNAMED,java.naming",
        )

        private val ADDED_MODULES = listOf<String>(

        )

        private val MODERN_JAVA_ARGS_EXTRA = listOf(
            "-XX:+UseZGC"
        )

        private val javaArgs = MODERN_JAVA_ARGS_EXTRA +
                ADDED_MODULES.flatMap { listOf("--add-modules", it) } +
                OPENED_PACKAGES.flatMap { listOf("--add-opens", it) }

        val MODERN_PATCH_DEPS = "modernJavaPatchDeps"
        val MODERN_PATCH_DEPS_OBF = "modernJavaPatchDepsObf"
        private val MODERN_PATCH_DEPS_COMPILE_ONLY = "modernJavaPatchDepsCompileOnly"

        private val JABEL = "com.github.bsideup.jabel:jabel-javac-plugin:1.0.1"

        private val BLACKLISTED_TASKS = listOf("compileMcLauncherJava", "compilePatchedMcJava")

        private fun <T: Task> TaskCollection<T>.configureEachFiltered(action: T.() -> Unit) {
            this.configureEach {
                if (!BLACKLISTED_TASKS.contains(name))
                    action(this)
            }
        }


        fun Project.injectLwjgl3ifyForSet(compat: Compatibility, javaVersion: Provider<JavaVersion>, javaVendor: Provider<JvmVendorSpec>, set: SourceSet) {
            injectLwjgl3ifyForSetInternal(compat, javaVersion, javaVendor, set)
        }

        private fun Project.injectLwjgl3ifyForSetInternal(compat: Compatibility, javaVersion: Provider<JavaVersion>, javaVendor: Provider<JvmVendorSpec>, set: SourceSet?) {
            when(compat) {
                LegacyJava -> {
                    setToolchainVersionLegacy(javaVersion, javaVendor, set)
                    setupJavaConfigsModern(false, set)
                    injectLWJGL3ifyModern(false, set)
                }
                Jabel -> {
                    setToolchainVersionJabel(javaVersion, javaVendor, set)
                    setupJavaConfigsModern(false, set)
                    injectLWJGL3ifyModern(false, set)
                }
                ModernJava -> {
                    setToolchainVersionModern(javaVersion, javaVendor, set)
                    setupJavaConfigsModern(true, set)
                    swapLWJGLVersionModern(set)
                    injectLWJGL3ifyModern(true, set)
                    if (set == null) {
                        manifestAttributes.put("Lwjgl3ify-Aware", "true")
                    } else {
                        tasks.named<Jar>(set.jarTaskName) {
                            manifest {
                                attributes("Lwjgl3ify-Aware" to "true")
                            }
                        }
                    }
                }
            }
        }

        private fun Project.setToolchainVersionLegacy(javaVersion: Provider<JavaVersion>, javaVendor: Provider<JvmVendorSpec>, set: SourceSet?) {
            if (set == null) {
                java.toolchain.languageVersion = mc.java.version.map { JavaLanguageVersion.of(it.majorVersion) }
                java.toolchain.vendor = mc.java.vendor
                val legacyCompiler = toolchains.compilerFor(java.toolchain)
                tasks.withType<JavaCompile>().configureEachFiltered {
                    javaCompiler = legacyCompiler
                }
            } else {
                val legacyCompiler = toolchains.compilerFor {
                    languageVersion.set(javaVersion.map { JavaLanguageVersion.of(it.majorVersion) })
                    vendor.set(javaVendor)
                }
                tasks.named<JavaCompile>(set.compileJavaTaskName) {
                    javaCompiler = legacyCompiler
                }
            }
        }

        private fun Project.setToolchainVersionJabel(javaVersion: Provider<JavaVersion>, javaVendor: Provider<JvmVendorSpec>, set: SourceSet?) {
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
                add(set?.annotationProcessorConfigurationName ?: ANNOTATION_PROCESSOR, JABEL)
                add(set?.compileOnlyConfigurationName ?: COMPILE_ONLY, JABEL) {
                    isTransitive = false
                }
                // Workaround for https://github.com/bsideup/jabel/issues/174
                add(set?.annotationProcessorConfigurationName ?: ANNOTATION_PROCESSOR, "net.java.dev.jna:jna-platform:5.13.0")
                // Allow using jdk.unsupported classes like sun.misc.Unsafe in the compiled code, working around
                // JDK-8206937.
                if (set == null) {
                    add(MCPTasks.PATCHED_MINECRAFT_CONFIGURATION_NAME, "me.eigenraven.java8unsupported:java-8-unsupported-shim:1.0.0")
                }
            }
            if (set == null) {
                java.toolchain.languageVersion = javaVersion.map { JavaLanguageVersion.of(it.majorVersion) }
                java.toolchain.vendor = javaVendor

                val jabelCompiler = toolchains.compilerFor(java.toolchain)
                tasks.withType<JavaCompile>().configureEachFiltered {
                    sourceCompatibility = javaVersion.map { it.majorVersion }.get()
                    options.release = 8
                    javaCompiler = jabelCompiler
                }
            } else {
                val jabelCompiler = toolchains.compilerFor {
                    languageVersion.set(javaVersion.map { JavaLanguageVersion.of(it.majorVersion) })
                    vendor.set(javaVendor)
                }
                tasks.named<JavaCompile>(set.compileJavaTaskName) {
                    sourceCompatibility = mc.java.version.map { it.majorVersion }.get()
                    options.release = 8
                    javaCompiler = jabelCompiler
                }
            }
        }

        private fun Project.setToolchainVersionModern(javaVersion: Provider<JavaVersion>, javaVendor: Provider<JvmVendorSpec>, set: SourceSet?) {
            if (set == null) {
                java.toolchain.languageVersion = javaVersion.map { JavaLanguageVersion.of(it.majorVersion) }
                java.toolchain.vendor = javaVendor
                val modernCompiler = toolchains.compilerFor(java.toolchain)
                tasks.withType<JavaCompile>().configureEachFiltered {
                    sourceCompatibility = javaVersion.map { it.majorVersion }.get()
                    targetCompatibility = javaVersion.map { it.majorVersion }.get()
                    javaCompiler = modernCompiler
                }
            } else {
                val modernCompiler = toolchains.compilerFor {
                    languageVersion.set(javaVersion.map { JavaLanguageVersion.of(it.majorVersion) })
                    vendor.set(javaVendor)
                }
                tasks.named<JavaCompile>(set.compileJavaTaskName) {
                    sourceCompatibility = javaVersion.map { it.majorVersion }.get()
                    targetCompatibility = javaVersion.map { it.majorVersion }.get()
                    javaCompiler = modernCompiler
                }
            }
        }

        private fun Project.setupJavaConfigsModern(modernCompile: Boolean, set: SourceSet?) {
            with(configurations) {
                if (modernCompile) {
                    val modernJavaPatchDependencies = create((set?.name ?: "") + MODERN_PATCH_DEPS_COMPILE_ONLY) {
                        attributes.attribute(ObfuscationAttribute.OBFUSCATION_ATTRIBUTE, ObfuscationAttribute.getMcp(objects))
                    }
                    getByName(set?.compileOnlyConfigurationName ?: "compileOnly") {
                        extendsFrom(modernJavaPatchDependencies)
                    }
                }
            }
        }

        private fun Project.swapLWJGLVersionModern(set: SourceSet?) {
            with(configurations) {
                getByName(set?.compileOnlyConfigurationName ?: "compileOnly") {
                    extendsFrom(minecraftTasks.lwjgl3Configuration)
                }
                getByName(set?.compileClasspathConfigurationName ?: "compileClasspath") {
                    exclude(mapOf(Pair("group", "org.lwjgl.lwjgl")))
                }
            }
        }

        private fun Project.injectLWJGL3ifyModern(modernCompile: Boolean, set: SourceSet?) {
            if (set == null) {
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
            }

            val asmVersion = provider { PackageRegistry.MODERN_JAVA_ASM_VERSION }
            val rfbVersion = provider { PackageRegistry.RFB_VERSION }
            val lwjgl3ifyVersion = provider { PackageRegistry.LWJGL3IFY_VERSION }

            val lwjgl3ify = lwjgl3ifyVersion.map { "com.github.GTNewHorizons:lwjgl3ify:$it" }

            dependencies {
                if (modernCompile) {
                    addProvider(set?.compileOnlyConfigurationName ?: "devOnlyNonPublishable", lwjgl3ify.map { "$it:dev" })

                    val pDeps = (set?.name ?: "") + MODERN_PATCH_DEPS_COMPILE_ONLY
                    addProvider<_, ExternalModuleDependency>(
                        pDeps,
                        rfbVersion.map { "com.gtnewhorizons.retrofuturabootstrap:RetroFuturaBootstrap:$it" }) { isTransitive = false }
                    addProvider(pDeps, asmVersion.map { "org.ow2.asm:asm:$it" })
                    addProvider(pDeps, asmVersion.map { "org.ow2.asm:asm-commons:$it" })
                    addProvider(pDeps, asmVersion.map { "org.ow2.asm:asm-tree:$it" })
                    addProvider(pDeps, asmVersion.map { "org.ow2.asm:asm-analysis:$it" })
                    addProvider(pDeps, asmVersion.map { "org.ow2.asm:asm-util:$it" })
                    addProvider(pDeps, provider { "org.ow2.asm:asm-deprecated:7.1" })
                    addProvider(pDeps, provider { PackageRegistry.MODERN_JAVA_COMMONS_LANG })
                    addProvider(pDeps, provider { PackageRegistry.MODERN_JAVA_COMMONS_COMPRESS })
                    addProvider(pDeps, provider { PackageRegistry.MODERN_JAVA_COMMONS_IO })
                } else if (set == null) {
                    addProvider(MODERN_PATCH_DEPS, lwjgl3ify.map { "$it:dev" })
                }
                if (set == null) {
                    addProvider<_, ExternalModuleDependency>(MODERN_PATCH_DEPS, lwjgl3ify.map { "$it:forgePatches" }) { isTransitive = false }
                    addProvider<_, ExternalModuleDependency>(MODERN_PATCH_DEPS_OBF, lwjgl3ify.map { "$it:forgePatches" }) { isTransitive = false }
                }
            }
        }
    }
}
