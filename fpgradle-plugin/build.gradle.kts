plugins {
    idea
    `maven-publish`
    `kotlin-dsl`
}

group = "com.falsepattern"
version = "0.6.1"

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
    // JetBrains Java Annotations
    implementation("org.jetbrains:annotations:24.1.0")

    // Lombok Gradle Plugin
    implementation("io.freefair.lombok:io.freefair.lombok.gradle.plugin:8.6")

    // JGit
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.10.0.202406032230-r")

    // Apache Commons Lang
    implementation("org.apache.commons:commons-lang3:3.14.0")

    // Apache Commons IO
    implementation("commons-io:commons-io:2.16.1")

    // Gson
    implementation("com.google.code.gson:gson:2.11.0")

    // RFG
    implementation("com.gtnewhorizons:retrofuturagradle:1.4.0-fp")

    // Shadow
    implementation("com.github.johnrengelman:shadow:8.1.1")

    // JTweaker (stubpackage)
    implementation("com.falsepattern:jtweaker:0.3.4")

    // MappingGenerator
    implementation("io.github.LegacyModdingMC.MappingGenerator:MappingGenerator:0.1.2")

    // IntelliJ
    implementation("gradle.plugin.org.jetbrains.gradle.plugin.idea-ext:gradle-idea-ext:1.1.8")

    // CurseForgeGradle
    implementation("net.darkhax.curseforgegradle:CurseForgeGradle:1.1.24")

    // Minotaur
    implementation("com.modrinth.minotaur:Minotaur:2.8.7")

    // Maven metadata
    implementation("org.apache.maven:maven-repository-metadata:3.9.7")
}

val add: NamedDomainObjectContainer<PluginDeclaration>.(pluginID: String, pluginClass: String) ->
Unit = { pluginID: String, pluginClass: String ->
    this.register(pluginID) {
        id = pluginID
        group = project.group
        version = project.version
        implementationClass = "com.falsepattern.fpgradle.project.$pluginClass"
    }
}

gradlePlugin {
    plugins.add("fpgradle-minecraft", "MinecraftPlugin")
}

publishing {
    repositories {
        maven {
            name = "fpgradle"
            url = uri("https://mvn.falsepattern.com/fpgradle/")
            val user = System.getenv("MAVEN_DEPLOY_USER")
            val pass = System.getenv("MAVEN_DEPLOY_PASSWORD")
            if (user != null && pass != null) {
                credentials {
                    username = user
                    password = pass
                }
            } else {
                credentials(PasswordCredentials::class)
            }
        }
    }
}