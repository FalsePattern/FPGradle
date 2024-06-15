plugins {
    idea
    `maven-publish`
    `kotlin-dsl`
}

group = "com.falsepattern"
version = "1.0-SNAPSHOT"



repositories {
    maven {
        url = uri("https://mvn.falsepattern.com/fpgradle/")
        name = "fpgradle"
    }
}

dependencies {
    // JetBrains Java Annotations
    implementation("org.jetbrains:annotations:24.1.0")

    // Lombok Gradle Plugin
    implementation("io.freefair.lombok:io.freefair.lombok.gradle.plugin:8.6")

    // JGit
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.8.0.202311291450-r")

    // Apache Commons Lang
    implementation("org.apache.commons:commons-lang3:3.14.0")

    // Apache Commons IO
    implementation("commons-io:commons-io:2.15.1")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // RFG
    implementation("com.gtnewhorizons:retrofuturagradle:1.3.36-fp")

    // Shadow
    implementation("com.github.johnrengelman:shadow:8.1.1")

    // JTweaker (stubpackage)
    implementation("com.falsepattern:jtweaker:0.2.2")

    //MappingGenerator
    implementation("io.github.LegacyModdingMC:MappingGenerator:0.1.2")

    //IntelliJ
    implementation("gradle.plugin.org.jetbrains.gradle.plugin.idea-ext:gradle-idea-ext:1.1.8")
}

val add: NamedDomainObjectContainer<PluginDeclaration>.(pluginID: String, pluginClass: String) ->
Unit = { pluginID: String, pluginClass: String ->
    this.register(pluginID) {
        id = pluginID
        group = project.group
        version = project.version
        implementationClass = "com.falsepattern.fpgradle.plugin.$pluginClass"
    }
}

gradlePlugin {
    plugins.add("fpgradle-minecraft", "MinecraftPlugin")
}


kotlin {
    jvmToolchain(21)
}