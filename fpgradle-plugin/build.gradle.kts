import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.named

plugins {
    idea
    id("com.gradle.plugin-publish") version "1.3.1"
    `kotlin-dsl`
    id("com.gradleup.shadow") version "9.0.0-beta13"
}

val buildscriptVersion = "0.15.0"

group = "com.falsepattern"
version = buildscriptVersion

kotlin {
    jvmToolchain(21)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(JavaVersion.VERSION_21.majorVersion)
        vendor = JvmVendorSpec.ADOPTIUM
    }
    withSourcesJar()
    withJavadocJar()
}

tasks.processResources.configure {
    inputs.property("version", version)
    filesMatching("fpgradle/version.properties") {
        expand("version" to version)
    }
}

run {
    val shadowRuntimeElements = configurations.getByName("shadowRuntimeElements")
    val shadowImplementation = configurations.create("shadowImplementation")
    shadowImplementation.isCanBeConsumed = false
    shadowImplementation.isCanBeResolved = true
    listOf("compileClasspath", "runtimeClasspath", "testCompileClasspath", "testRuntimeClasspath").forEach {
        configurations.getByName(it)
            .extendsFrom(shadowImplementation)
    }
    tasks {
        val shadowJar = named<ShadowJar>("shadowJar")
        shadowJar.configure {
            configurations = listOf(shadowImplementation)
        }

        for (outgoingConfig in listOf("runtimeElements", "apiElements")) {
            val outgoing = configurations.getByName(outgoingConfig)
            outgoing.outgoing.artifacts.clear()
            outgoing.outgoing.artifact(shadowJar)
        }

        named<Jar>("jar").configure {
            finalizedBy("shadowJar")
        }

        named<ShadowJar>("shadowJar").configure {
            archiveClassifier = ""
            exclude("META-INF/gradle-plugins/com.gtnewhorizons.*")
            exclude("META-INF/gradle-plugins/io.github.legacymoddingmc.*")
            exclude("META-INF/gradle-plugins/jtweaker.properties")
        }
    }
    val javaComponent = components.findByName("java") as AdhocComponentWithVariants
    javaComponent.withVariantsFromConfiguration(shadowRuntimeElements, ConfigurationVariantDetails::skip)
}
repositories {
    maven {
        url = uri("https://mvn.falsepattern.com/fpgradle/")
        name = "fpgradle"
        content {
            includeModule("com.gtnewhorizons", "retrofuturagradle")
        }
    }
    maven {
        url = uri("https://mvn.falsepattern.com/releases/")
        name = "mavenpattern"
        content {
            includeGroup("com.falsepattern")
        }
    }
    maven {
        url = uri("https://mvn.falsepattern.com/jitpack/")
        name = "jitpack"
        content {
            includeModule("io.github.LegacyModdingMC.MappingGenerator", "MappingGenerator")
        }
    }
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Kotlin Plugin
    compileOnly("org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:2.1.20")

    // JetBrains Java Annotations
    implementation("org.jetbrains:annotations:26.0.2")

    // Lombok Gradle Plugin
    implementation("io.freefair.lombok:io.freefair.lombok.gradle.plugin:8.13.1")

    // JGit
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.2.0.202503040940-r")

    // Apache Commons Lang
    implementation("org.apache.commons:commons-lang3:3.17.0")

    // Apache Commons IO
    implementation("commons-io:commons-io:2.19.0")

    // Apache Commons Compress
    implementation("org.apache.commons:commons-compress:1.27.1")

    // XZ
    implementation("org.tukaani:xz:1.10")

    // Gson
    implementation("com.google.code.gson:gson:2.13.0")

    // RFG
    add("shadowImplementation", "com.gtnewhorizons:retrofuturagradle:1.4.5-fp")

    // Shadow
    implementation("com.gradleup.shadow:com.gradleup.shadow.gradle.plugin:9.0.0-beta13")

    // JTweaker (stubpackage)
    add("shadowImplementation", "com.falsepattern:jtweaker:0.5.0") {
        isTransitive = false
    }
    implementation("org.apache.bcel:bcel:6.10.0")

    // MappingGenerator
    add("shadowImplementation", "io.github.LegacyModdingMC.MappingGenerator:MappingGenerator:0.1.2") {
        isTransitive = false
    }

    // IntelliJ
    implementation("gradle.plugin.org.jetbrains.gradle.plugin.idea-ext:gradle-idea-ext:1.1.10")

    // CurseForgeGradle
    implementation("net.darkhax.curseforgegradle:CurseForgeGradle:1.1.26")

    // Minotaur
    implementation("com.modrinth.minotaur:Minotaur:2.8.7")

    // Maven metadata
    implementation("org.apache.maven:maven-repository-metadata:3.9.9")
}

gradlePlugin {
    website.set("https://github.com/FalsePattern/FPGradle")
    vcsUrl.set("https://github.com/FalsePattern/FPGradle")
    plugins {
        create("fpgradle-mc") {
            id = "com.falsepattern.fpgradle-mc"
            implementationClass = "com.falsepattern.fpgradle.project.MinecraftPlugin"
            displayName = "FPGradle Mod Development Plugin"
            description = "A gradle plugin for Minecraft 1.7.10 mod development with a declarative-like configuration system."
            tags.set(listOf("minecraft", "forge", "minecraftforge", "java", "mod"))
        }
    }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}