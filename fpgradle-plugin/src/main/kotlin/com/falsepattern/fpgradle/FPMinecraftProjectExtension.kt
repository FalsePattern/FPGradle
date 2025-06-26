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

package com.falsepattern.fpgradle

import com.modrinth.minotaur.dependencies.Dependency
import com.modrinth.minotaur.dependencies.DependencyType
import com.modrinth.minotaur.dependencies.ModDependency
import com.modrinth.minotaur.dependencies.VersionDependency
import net.darkhax.curseforgegradle.UploadArtifact
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import org.gradle.jvm.toolchain.JvmVendorSpec
import java.util.*

@Suppress("unused", "LeakingThis", "PropertyName")
abstract class FPMinecraftProjectExtension(project: Project): ExtensionAware {
    init {
        java.compatibility.convention(Java.Compatibility.LegacyJava)
        java.version.convention(java.compatibility.map { when(it) {
            Java.Compatibility.LegacyJava -> JavaVersion.VERSION_1_8
            Java.Compatibility.Jabel -> JavaVersion.VERSION_17
            Java.Compatibility.ModernJava -> JavaVersion.VERSION_21
        } })
        java.vendor.convention(JvmVendorSpec.ADOPTIUM)
        java.modernRuntimeVersion.convention(java.compatibility.map { when(it) {
            Java.Compatibility.LegacyJava -> JavaVersion.VERSION_21
            Java.Compatibility.Jabel -> JavaVersion.VERSION_21
            Java.Compatibility.ModernJava -> java.version.get()
        } })

        mod.version.convention(project.provider { project.version.toString() })

        run.username.convention("Developer")

        api.classes.convention(listOf())
        api.packages.convention(listOf())
        api.packagesNoRecurse.convention(listOf())
        api.ignoreRootPkg.convention(false)
        api.includeSources.convention(true)

        mixin.debug.convention(false)
        mixin.hasMixinDeps.convention(java.compatibility.map { it == Java.Compatibility.ModernJava })
        mixin.extraConfigs.convention(listOf())
        mixin.ignoreRootPkg.convention(false)

        kotlin.forgelinVersion.convention(null)

        core.coreModIgnoreRootPkg.convention(false)
        core.containsMixinsAndOrCoreModOnly.convention(false)

        shadow.minimize.convention(false)
        shadow.relocate.convention(false)

        tokens.tokenClassIgnoreRootPkg.convention(false)
        tokens.modid.convention("MOD_ID")
        tokens.name.convention("MOD_NAME")
        tokens.version.convention("MOD_VERSION")
        tokens.rootPkg.convention("ROOT_PKG")

        logging.level.convention(Logging.Level.INFO)

        publish.changelog.convention(project.provider {
            val changelogFile = project.file(System.getenv("CHANGELOG_FILE") ?: "CHANGELOG.md")
            if (changelogFile.exists())
                changelogFile.readText()
            else
                "No changelog was provided."
        })

        publish.reproducibleJars.convention(true)
        publish.maven.sources.convention(true)
        publish.maven.group.convention(project.provider { project.group.toString() })
        publish.maven.artifact.convention(mod.modid.map { "$it-mc1.7.10" })
        publish.maven.version.convention(mod.version)
        publish.maven.userEnv.convention("MAVEN_DEPLOY_USER")
        publish.maven.passEnv.convention("MAVEN_DEPLOY_PASSWORD")

        publish.curseforge.tokenEnv.convention("CURSEFORGE_TOKEN")
        publish.modrinth.tokenEnv.convention("MODRINTH_TOKEN")

        updates.check.convention(true)
    }

    //region java
    abstract class Java: ExtensionAware {
        abstract val version: Property<JavaVersion>
        abstract val vendor: Property<JvmVendorSpec>
        abstract val modernRuntimeVersion: Property<JavaVersion>
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

    //region run
    abstract class Run: ExtensionAware {
        abstract val username: Property<String>
        abstract val userUUID: Property<UUID>
    }
    @get:Nested
    abstract val run: Run
    fun run(action: Run.() -> Unit) {
        action(run)
    }
    //endregion

    //region api
    abstract class Api: ExtensionAware {
        abstract val classes: ListProperty<String>
        abstract val packages: ListProperty<String>
        abstract val packagesNoRecurse: ListProperty<String>
        abstract val ignoreRootPkg: Property<Boolean>
        abstract val includeSources: Property<Boolean>
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
        abstract val extraConfigs: ListProperty<String>
        abstract val debug: Property<Boolean>
        abstract val hasMixinDeps: Property<Boolean>
        abstract val ignoreRootPkg: Property<Boolean>

        val use get() = pkg.isPresent
    }
    @get:Nested
    abstract val mixin: Mixins
    fun mixin(action: Mixins.() -> Unit) {
        action(mixin)
    }
    //endregion
    abstract class Kotlin: ExtensionAware {
        abstract val forgelinVersion: Property<String>
    }
    @get:Nested
    abstract val kotlin: Kotlin
    fun kotlin(action: Kotlin.() -> Unit) {
        action(kotlin)
    }

    //region core
    abstract class Core: ExtensionAware {
        abstract val coreModClass: Property<String>
        abstract val coreModIgnoreRootPkg: Property<Boolean>
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
        abstract val tokenClassIgnoreRootPkg: Property<Boolean>
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

    //region logging
    abstract class Logging: ExtensionAware {
        abstract val level: Property<Level>

        val OFF = Level.OFF
        val FATAL = Level.FATAL
        val ERROR = Level.ERROR
        val WARN = Level.WARN
        val INFO = Level.INFO
        val DEBUG = Level.DEBUG
        val TRACE = Level.TRACE
        val ALL = Level.ALL

        enum class Level {
            OFF,
            FATAL,
            ERROR,
            WARN,
            INFO,
            DEBUG,
            TRACE,
            ALL
        }
    }
    @get:Nested
    abstract val logging: Logging
    fun logging(action: Logging.() -> Unit) {
        action(logging)
    }
    //endregion

    //region publish
    abstract class Publish: ExtensionAware {
        abstract val reproducibleJars: Property<Boolean>
        abstract val changelog: Property<String>
        abstract class Maven: ExtensionAware {
            abstract val repoUrl: Property<String>
            abstract val repoName: Property<String>
            abstract val sources: Property<Boolean>
            abstract val group: Property<String>
            abstract val artifact: Property<String>
            abstract val version: Property<String>
            abstract val userEnv: Property<String>
            abstract val passEnv: Property<String>
        }
        @get:Nested
        abstract val maven: Maven
        fun maven(action: Maven.() -> Unit) {
            action(maven)
        }

        abstract class CurseForge: ExtensionAware {
            abstract val projectId: Property<String>
            abstract val tokenEnv: Property<String>
            abstract val relations: ListProperty<UploadArtifact.() -> Unit>

            inner class Dependencies {
                fun required(id: String) = relations.add { addRequirement(id) }
                fun optional(id: String) = relations.add { addOptional(id) }
                fun incompatible(id: String) = relations.add { addIncompatibility(id) }
                fun embedded(id: String) = relations.add { addEmbedded(id) }
                fun tool(id: String) = relations.add { addTool(id) }
            }
            fun dependencies(action: Dependencies.() -> Unit) {
                action(Dependencies())
            }
        }
        @get:Nested
        abstract val curseforge: CurseForge
        fun curseforge(action: CurseForge.() -> Unit) {
            action(curseforge)
        }

        abstract class Modrinth: ExtensionAware {
            abstract val projectId: Property<String>
            abstract val tokenEnv: Property<String>
            abstract val dependencies: ListProperty<() -> Dependency>

            inner class Dependencies(val variant: (String, DependencyType) -> Dependency) {
                fun required(id: String) = dependencies.add { variant(id, DependencyType.REQUIRED) }
                fun optional(id: String) = dependencies.add { variant(id, DependencyType.OPTIONAL) }
                fun incompatible(id: String) = dependencies.add { variant(id, DependencyType.INCOMPATIBLE) }
                fun embedded(id: String) = dependencies.add { variant(id, DependencyType.EMBEDDED) }
            }
            fun dependencies(action: Dependencies.() -> Unit) {
                action(Dependencies(::ModDependency))
            }

            fun versionDependencies(action: Dependencies.() -> Unit) {
                action(Dependencies(::VersionDependency))
            }
        }
        @get:Nested
        abstract val modrinth: Modrinth
        fun modrinth(action: Modrinth.() -> Unit) {
            action(modrinth)
        }
    }
    @get:Nested
    abstract val publish: Publish
    fun publish(action: Publish.() -> Unit) {
        action(publish)
    }
    //endregion

    //region updates
    abstract class Updates: ExtensionAware {
        abstract val check: Property<Boolean>
    }
    @get:Nested
    abstract val updates: Updates
    fun updates(action: Updates.() -> Unit) {
        action(updates)
    }
    //endregion

    //region DSL
    @JvmName("assignStringToUUID")
    fun Property<UUID>.assign(value: String) {
        this.set(UUID.fromString(value))
    }
    //endregion
}
