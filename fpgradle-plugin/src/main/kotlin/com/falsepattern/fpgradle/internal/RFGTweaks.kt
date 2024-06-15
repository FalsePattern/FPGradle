package com.falsepattern.fpgradle.internal

import com.falsepattern.fpgradle.FPMinecraftProjectExtension
import com.gtnewhorizons.retrofuturagradle.MinecraftExtension
import com.gtnewhorizons.retrofuturagradle.minecraft.MinecraftTasks
import com.gtnewhorizons.retrofuturagradle.minecraft.RunMinecraftTask
import com.gtnewhorizons.retrofuturagradle.util.Distribution
import io.github.legacymoddingmc.mappinggenerator.MappingGeneratorExtension
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.*

class RFGTweaks(ctx: ConfigurationContext): InitTask {
    private val project = ctx.project
    private val minecraft = project.extensions.getByType<MinecraftExtension>()
    private val mcTasks = project.extensions.getByType<MinecraftTasks>()
    private val mc = project.extensions.getByType<FPMinecraftProjectExtension>()
    private val java = project.extensions.getByType<JavaPluginExtension>()
    private val mGen = project.extensions.getByType<MappingGeneratorExtension>()

    override fun init() {
        setToolchainVersion()
        setupModernJavaConfigs()
        swapLWJGLVersion()
        injectLWJGL3ify()
        tweakMappingGenerator()
        modifyMinecraftRunTask("runClient", Distribution.CLIENT)
        modifyMinecraftRunTask("runServer", Distribution.DEDICATED_SERVER)
        minecraft.injectMissingGenerics = true
    }

    private fun setToolchainVersion() {
        java.toolchain.languageVersion = mc.javaVersion.map { JavaLanguageVersion.of(it.majorVersion) }
        val javaToolchains = project.extensions.getByType<JavaToolchainService>()
        project.tasks.named<JavaCompile>("compileMcLauncherJava").configure {
            javaCompiler = javaToolchains.compilerFor(java.toolchain)
        }
    }

    private fun setupModernJavaConfigs() {
        with(project.configurations) {
            val modernJavaDependencies = create(MODERN_DEPS)
            create(MODERN_PATCH_DEPS)
            val modernJavaPatchDependencies = create(MODERN_PATCH_DEPS_COMPILE_ONLY)
            create(MODERN_DEPS_COMBINED) {
                extendsFrom(getByName("runtimeClasspath"), modernJavaDependencies)
            }
            getByName("compileOnly") {
                extendsFrom(modernJavaDependencies, modernJavaPatchDependencies)
            }
        }
    }

    private fun swapLWJGLVersion() = with(project) {
        with(configurations) {
            getByName("compileOnly") {
                extendsFrom(mcTasks.lwjgl3Configuration)
            }
            getByName("compileClasspath") {
                exclude(mapOf(Pair("group", "org.lwjgl.lwjgl")))
            }
        }
    }

    private fun injectLWJGL3ify() = with(project) {
        repositories {
            maven {
                name = "fpgradle"
                url = uri("https://mvn.falsepattern.com/fpgradle/")
            }
            maven {
                name = "horizon"
                url = uri("https://mvn.falsepattern.com/horizon/")
                content {
                    includeGroupAndSubgroups("com.gtnewhorizons")
                    includeGroupAndSubgroups("com.github.GTNewHorizons")
                    includeModule("org.jetbrains", "intellij-fernflower")
                }
            }
            maven {
                name = "jitpack"
                url = uri("https://mvn.falsepattern.com/jitpack/")
            }
        }

        val asmVersion = provider { "9.7" }
        val rfbVersion = provider { "1.0.2" }
        val lwjgl3ifyVersion = provider { "2.0.9" }

        val lwjgl3ify = lwjgl3ifyVersion.map { "com.github.GTNewHorizons:lwjgl3ify:$it" }

        dependencies {
            addProvider(MODERN_DEPS, lwjgl3ify)

            addProvider<_, ExternalModuleDependency>(MODERN_PATCH_DEPS_COMPILE_ONLY, rfbVersion.map { "com.gtnewhorizons.retrofuturabootstrap:RetroFuturaBootstrap:$it" }) { isTransitive = false }
            addProvider(MODERN_PATCH_DEPS_COMPILE_ONLY, asmVersion.map { "org.ow2.asm:asm:$it" })
            addProvider(MODERN_PATCH_DEPS_COMPILE_ONLY, asmVersion.map { "org.ow2.asm:asm-commons:$it" })
            addProvider(MODERN_PATCH_DEPS_COMPILE_ONLY, asmVersion.map { "org.ow2.asm:asm-tree:$it" })
            addProvider(MODERN_PATCH_DEPS_COMPILE_ONLY, asmVersion.map { "org.ow2.asm:asm-analysis:$it" })
            addProvider(MODERN_PATCH_DEPS_COMPILE_ONLY, asmVersion.map { "org.ow2.asm:asm-util:$it" })
            addProvider(MODERN_PATCH_DEPS_COMPILE_ONLY, provider { "org.ow2.asm:asm-deprecated:7.1" })
            addProvider(MODERN_PATCH_DEPS_COMPILE_ONLY, provider { "org.apache.commons:commons-lang3:3.14.0" })
            addProvider(MODERN_PATCH_DEPS_COMPILE_ONLY, provider { "org.apache.commons:commons-compress:1.26.0" })
            addProvider(MODERN_PATCH_DEPS_COMPILE_ONLY, provider { "commons-io:commons-io:2.15.1" })
            addProvider<_, ExternalModuleDependency>(MODERN_PATCH_DEPS, lwjgl3ify.map { "$it:forgePatches" }) { isTransitive = false }
        }
    }

    private fun tweakMappingGenerator() {
        mGen.sources = listOf(
            listOf("yarn", "1.7.10+build.533"),
            listOf("mcp", "1.8.9", "stable_22", "parameters"),
            listOf("mcp", "1.12", "stable_39", "parameters"),
            listOf("mcp", "1.7.10", "stable_12", "methodComments"),
            listOf("csv", "https://raw.githubusercontent.com/LegacyModdingMC/ExtraMappings/master/params.csv"),
            listOf("csv", "https://raw.githubusercontent.com/FalsePattern/srgmap/10613b1ed10c3e1b8f6da47b5b25d0dd83037849/params.csv"),
        )
    }

    private fun modifyMinecraftRunTask(taskName: String, side: Distribution) = with(project) {
        tasks.named<RunMinecraftTask>(taskName).configure {
            lwjglVersion = 3
            val javaToolchains = project.extensions.getByType<JavaToolchainService>()
            val java = project.extensions.getByType<JavaPluginExtension>()
            javaLauncher = javaToolchains.launcherFor(java.toolchain)
            extraJvmArgs.addAll(javaArgs)
            systemProperty("gradlestart.bouncerClient", "com.gtnewhorizons.retrofuturabootstrap.Main")
            systemProperty("java.system.class.loader", "com.gtnewhorizons.retrofuturabootstrap.RfbSystemClassLoader")

            val modernJavaDependenciesCombined = configurations.getByName(MODERN_DEPS_COMBINED)
            val modernJavaPatchDependencies = configurations.getByName(MODERN_PATCH_DEPS)

            val oldClasspath = classpath
            setClasspath(files())
            classpath(modernJavaPatchDependencies)
            if (side == Distribution.CLIENT) {
                val minecraftTasks = project.extensions.getByType<MinecraftTasks>()
                classpath(minecraftTasks.lwjgl3Configuration)
            }
            classpath(modernJavaDependenciesCombined)
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
            "java.base/java.lang.reflect=ALL-UNNAMED",
            "java.base/java.text=ALL-UNNAMED",
            "java.base/java.util=ALL-UNNAMED",
            "java.base/jdk.internal.reflect=ALL-UNNAMED",
            "java.base/sun.nio.ch=ALL-UNNAMED",
            "jdk.naming.dns/com.sun.jndi.dns=ALL-UNNAMED,java.naming",
            "java.desktop/sun.awt.image=ALL-UNNAMED",
            "jdk.dynalink/jdk.dynalink.beans=ALL-UNNAMED",
            "java.sql.rowset/javax.sql.rowset.serial=ALL-UNNAMED"
        )

        private val ADDED_MODULES = listOf<String>(

        )

        private val MODERN_JAVA_ARGS_EXTRA = listOf(
            "-Dfile.encoding=UTF-8",
            "-Djava.security.manager=allow",
            "-XX:+UseZGC",
            "-XX:+ZGenerational"
        )

        private val javaArgs = MODERN_JAVA_ARGS_EXTRA +
                ADDED_MODULES.flatMap { listOf("--add-modules", it) } +
                OPENED_PACKAGES.flatMap { listOf("--add-opens", it) }

        private val MODERN_DEPS = "modernJavaDeps"
        private val MODERN_DEPS_COMBINED = "modernJavaDepsCombined"
        private val MODERN_PATCH_DEPS = "modernJavaPatchDeps"
        private val MODERN_PATCH_DEPS_COMPILE_ONLY = "modernJavaPatchDepsCompileOnly"
    }
}