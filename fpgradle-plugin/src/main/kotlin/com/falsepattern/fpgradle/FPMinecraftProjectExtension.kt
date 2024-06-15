package com.falsepattern.fpgradle

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import org.intellij.lang.annotations.Language
import java.net.URI

@Suppress("unused")
abstract class FPMinecraftProjectExtension(val project: Project): ExtensionAware {
    init { with(project) {
        java.compatibility.convention(Java.Compatibility.LegacyJava)
        java.version.convention(java.compatibility.map { when(it) {
            Java.Compatibility.LegacyJava, null -> JavaVersion.VERSION_1_8
            Java.Compatibility.Jabel -> JavaVersion.VERSION_17
            Java.Compatibility.ModernJava -> JavaVersion.VERSION_21
        } })

        mod.version.convention(provider { version.toString() })

        api.packages.convention(listOf())
        api.packagesNoRecurse.convention(listOf())

        mixin.debug.convention(false)
        mixin.hasMixinDeps.convention(false)

        core.containsMixinsAndOrCoreModOnly.convention(false)

        shadow.minimize.convention(false)
        shadow.relocate.convention(false)

        tokens.modid.convention("MOD_ID")
        tokens.name.convention("MOD_NAME")
        tokens.version.convention("MOD_VERSION")
        tokens.rootPkg.convention("ROOT_PKG")

        publish.sources.convention(true)
        publish.group.convention(provider { group.toString() })
        publish.artifact.convention(provider { "$name-mc1.7.10" })
        publish.version.convention(mod.version)
        publish.userEnv.convention("MAVEN_DEPLOY_USER")
        publish.passEnv.convention("MAVEN_DEPLOY_PASSWORD")
    } }

    //region java
    abstract class Java: ExtensionAware {
        abstract val version: Property<JavaVersion>
        abstract val compatibility: Property<Compatibility>

        enum class Compatibility {
            LegacyJava,
            Jabel,
            ModernJava
        }

        val legacy = Compatibility.LegacyJava
        val jabel = Compatibility.Jabel
        val modern = Compatibility.ModernJava
    }
    @get:Nested
    abstract val java: Java
    fun java(action: Java.() -> Unit) {
        action(java)
    }
    //endregion

    //region mod
    abstract class Mod: ExtensionAware {
        abstract val modid: Property<String>
        abstract val name: Property<String>
        abstract val version: Property<String>
        abstract val rootPkg: Property<String>
    }
    @get:Nested
    abstract val mod: Mod
    fun mod(action: Mod.() -> Unit) {
        action(mod)
    }
    //endregion

    //region api
    abstract class Api: ExtensionAware {
        abstract val packages: ListProperty<String>
        abstract val packagesNoRecurse: ListProperty<String>
    }
    @get:Nested
    abstract val api: Api
    fun api(action: Api.() -> Unit) {
        action(api)
    }
    //endregion

    //region mixins
    abstract class Mixins: ExtensionAware {
        abstract val pkg: Property<String>
        abstract val pluginClass: Property<String>
        abstract val debug: Property<Boolean>
        abstract val hasMixinDeps: Property<Boolean>

        val use get() = pkg.isPresent
    }
    @get:Nested
    abstract val mixin: Mixins
    fun mixin(action: Mixins.() -> Unit) {
        action(mixin)
    }
    //endregion

    //region core
    abstract class Core: ExtensionAware {
        abstract val coreModClass: Property<String>
        abstract val accessTransformerFile: Property<String>
        abstract val containsMixinsAndOrCoreModOnly: Property<Boolean>
    }
    @get:Nested
    abstract val core: Core
    fun core(action: Core.() -> Unit) {
        action(core)
    }
    //endregion

    //region shadow
    abstract class Shadow: ExtensionAware {
        abstract val minimize: Property<Boolean>
        abstract val relocate: Property<Boolean>
    }
    @get:Nested
    abstract val shadow: Shadow
    fun shadow(action: Shadow.() -> Unit) {
        action(shadow)
    }
    //endregion

    //region tokens
    abstract class Tokens: ExtensionAware {
        abstract val tokenClass: Property<String>
        abstract val modid: Property<String>
        abstract val name: Property<String>
        abstract val version: Property<String>
        abstract val rootPkg: Property<String>
    }
    @get:Nested
    abstract val tokens: Tokens
    fun tokens(action: Tokens.() -> Unit) {
        action(tokens)
    }
    //endregion

    //region publish
    abstract class Publish: ExtensionAware {
        abstract val repoUrl: Property<URI>
        abstract val repoName: Property<String>
        abstract val sources: Property<Boolean>
        abstract val group: Property<String>
        abstract val artifact: Property<String>
        abstract val version: Property<String>
        abstract val userEnv: Property<String>
        abstract val passEnv: Property<String>
    }
    @get:Nested
    abstract val publish: Publish
    fun publish(action: Publish.() -> Unit) {
        action(publish)
    }
    //endregion

    //region DSL
    fun Property<URI>.assign(@Language("http-url-reference") value: String) {
        this.set(project.uri(value))
    }
    //endregion
}
