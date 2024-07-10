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

package com.falsepattern.fpgradle.dsl

import com.falsepattern.fpgradle.rfg
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.InclusiveRepositoryContentDescriptor
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaPlugin
import org.gradle.kotlin.dsl.create
import java.net.URI

fun RepositoryHandler.maven(name: String, url: URI, action: MavenArtifactRepository.() -> Unit = {}) =
    maven {
        this.name = name
        this.url = url
        action.invoke(this)
    }

fun RepositoryHandler.ivy(url: URI, pattern: String, action: IvyArtifactRepository.() -> Unit = {}) =
    ivy {
        this.url = url
        patternLayout {
            artifact(pattern)
        }
        metadataSources {
            artifact()
        }
        action()
    }

fun DependencyHandler.implementationSplit(dependencyNotation: String, action: Dependency?.() -> Unit = {}) {
    action.invoke(add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, "$dependencyNotation:api"))
    action.invoke(add(JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME, "$dependencyNotation:dev"))
}

fun DependencyHandler.apiSplit(dependencyNotation: String, action: Dependency?.() -> Unit = {}) {
    action.invoke(add(JavaPlugin.COMPILE_ONLY_API_CONFIGURATION_NAME, "$dependencyNotation:api"))
    action.invoke(add(JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME, "$dependencyNotation:dev"))
}

fun DependencyHandler.deobfCurse(dependencyNotation: String) = rfg.deobf("curse.maven:$dependencyNotation")

fun DependencyHandler.deobfModrinth(dependencyNotation: String) = rfg.deobf("maven.modrinth:$dependencyNotation")

fun ModuleDependency.excludeDeps() {
    isTransitive = false
}

fun RepositoryHandler.exclusive(repo: ArtifactRepository, theFilter: InclusiveRepositoryContentDescriptor.() -> Unit = {}) {
    exclusiveContent {
        forRepositories(repo)
        filter {
            theFilter()
        }
    }
}

fun RepositoryHandler.exclusiveGroups(repo: ArtifactRepository, vararg groups: String, theFilter: InclusiveRepositoryContentDescriptor.() -> Unit = {}) = exclusive(repo) {
    includeGroups(*groups)
    theFilter()
}

fun InclusiveRepositoryContentDescriptor.includeGroups(vararg groups: String) {
    for (group in groups) {
        includeGroup(group)
    }
}

open class FPExt(val project: Project) {
    fun RepositoryHandler.maven(name: String, url: String, action: MavenArtifactRepository.() -> Unit = {}) = maven(name, project.uri(url), action)

    fun RepositoryHandler.ivy(url: String, pattern: String, action: IvyArtifactRepository.() -> Unit = {}) = ivy(project.uri(url), pattern, action)

    fun RepositoryHandler.cursemaven(extraFilter: InclusiveRepositoryContentDescriptor.() -> Unit = {}) = exclusiveGroups(maven("cursemaven", "https://mvn.falsepattern.com/cursemaven/"), "curse.maven", theFilter = extraFilter)

    fun RepositoryHandler.modrinth(extraFilter: InclusiveRepositoryContentDescriptor.() -> Unit = {}) = exclusiveGroups(maven("modrinth", "https://mvn.falsepattern.com/modrinth/"), "maven.modrinth", theFilter = extraFilter)
}

internal fun Project.registerFalsePatternDSL() {
    extensions.create("fp", FPExt::class, this)
}