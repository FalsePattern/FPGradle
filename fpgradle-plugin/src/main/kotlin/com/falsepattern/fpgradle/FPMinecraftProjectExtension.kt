package com.falsepattern.fpgradle

import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property

@Suppress("LeakingThis")
abstract class FPMinecraftProjectExtension(project: Project): ExtensionAware {
    abstract val javaVersion: Property<JavaVersion>

    abstract val modName: Property<String>
    abstract val modId: Property<String>
    abstract val modGroup: Property<String>

    abstract val apiPackage: Property<String>
    abstract val accessTransformersFile: Property<String>

    abstract val usesMixins: Property<Boolean>
    abstract val usesMixinsDebug: Property<Boolean>
    abstract val forceEnableMixins: Property<Boolean>
    abstract val mixinPlugin: Property<String>
    abstract val mixinsPackage: Property<String>

    abstract val coreModClass: Property<String>
    abstract val containsMixinsAndOrCoreModOnly: Property<Boolean>

    abstract val minimizeShadowedDependencies: Property<Boolean>
    abstract val relocateShadowedDependencies: Property<Boolean>

    abstract val generateGradleTokenClass: Property<String>
    abstract val gradleTokenVersion: Property<String>

    abstract val noPublishedSources: Property<Boolean>

    abstract val mavenPublishUrl: Property<String>
    abstract val mavenPublishRepoName: Property<String>

    init {
        javaVersion.convention(JavaVersion.VERSION_21)

        modName.unconfigured(project, "modName")
        modId.unconfigured(project, "modId")
        modGroup.unconfigured(project, "modGroup")

        usesMixins.convention(false)
        usesMixinsDebug.convention(false)
        forceEnableMixins.convention(false)
        mixinsPackage.unconfigured(project, "mixinsPackage")

        containsMixinsAndOrCoreModOnly.convention(false)

        minimizeShadowedDependencies.convention(false)
        relocateShadowedDependencies.convention(false)

        gradleTokenVersion.convention("VERSION")

        noPublishedSources.convention(false)
    }
}

private fun <T> Property<T>.unconfigured(project: Project, name: String) {
    this.convention(project.provider {
        throw GradleException("Property (MC -> $name) must be set!")
    })
}