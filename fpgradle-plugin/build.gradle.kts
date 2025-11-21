import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.accessors.runtime.addConfiguredDependencyTo
import org.jetbrains.kotlin.gradle.utils.extendsFrom

plugins {
    idea
    alias(libs.plugins.gradlePublish)
    `maven-publish`
    `kotlin-dsl`
    alias(libs.plugins.shadow)
}

val buildscriptVersion = "3.2.1"

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

// Lobotomize shadowJar to work in the inverse direction
run {
    val shadowImplementation = configurations.register("shadowImplementation")
    shadowImplementation.configure {
        isCanBeConsumed = false
        isCanBeResolved = true
    }
    listOf("compileClasspath", "runtimeClasspath", "testCompileClasspath", "testRuntimeClasspath").forEach {
        configurations.named(it)
            .extendsFrom(shadowImplementation)
    }
    tasks {
        val shadowJar = named<ShadowJar>("shadowJar")
        shadowJar.configure {
            configurations = listOf(shadowImplementation.get())
        }

        for (outgoingConfig in listOf("runtimeElements", "apiElements")) {
            configurations.named(outgoingConfig).configure {
                outgoing.artifacts.clear()
                outgoing.artifact(shadowJar)
            }
        }

        named<ShadowJar>("shadowJar").configure {
            archiveClassifier = ""
            exclude("META-INF/gradle-plugins/com.gtnewhorizons.*")
            exclude("META-INF/gradle-plugins/io.github.legacymoddingmc.*")
            exclude("META-INF/gradle-plugins/jtweaker.properties")
        }
    }
    // afterEvaluate because plugin-publish plugin messes with these
    afterEvaluate {
        components.named("java").configure {
            this as AdhocComponentWithVariants
            addVariantsFromConfiguration(configurations.getByName("apiElements")) {
                mapToMavenScope("runtime")
            }
            addVariantsFromConfiguration(configurations.getByName("runtimeElements")) {
                mapToMavenScope("runtime")
            }
            addVariantsFromConfiguration(configurations.getByName("shadowRuntimeElements"), ConfigurationVariantDetails::skip)
        }
    }
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
    compileOnly(libs.kotlinPlugin)
    implementation(libs.annotations)
    implementation(libs.lombokPlugin)
    implementation(libs.jgit)
    implementation(libs.commons.lang3)
    implementation(libs.commons.io)
    implementation(libs.commons.compress)
    implementation(libs.xz)
    implementation(libs.gson)
    add("shadowImplementation", libs.rfg)
    implementation(libs.shadow)
    addConfiguredDependencyTo(this, "shadowImplementation", libs.jtweaker) {
        isTransitive = false
    }
    implementation(libs.bcel)
    addConfiguredDependencyTo(this, "shadowImplementation", libs.mappingGenerator) {
        isTransitive = false
    }
    implementation(libs.jvmDowngraderPlugin)
    implementation(libs.gradleIdeaExt)
    implementation(libs.curseForgeGradle)
    implementation(libs.minotaur)
    implementation(libs.mavenRepoMetadata)
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