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

package org.gradle.kotlin.dsl

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.InclusiveRepositoryContentDescriptor
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository

fun RepositoryHandler.maven(name: String, url: Any, action: MavenArtifactRepository.() -> Unit = {}) =
    maven {
        this.name = name
        setUrl(url)
        action.invoke(this)
    }
fun RepositoryHandler.ivy(url: Any, pattern: String, action: IvyArtifactRepository.() -> Unit = {}) =
    ivy {
        setUrl(url)
        patternLayout {
            artifact(pattern)
        }
        metadataSources {
            artifact()
        }
        action()
    }

fun RepositoryHandler.cursemaven(action: MavenArtifactRepository.() -> Unit = {}) = maven("cursemaven", "https://mvn.falsepattern.com/cursemaven/", action)
fun RepositoryHandler.cursemavenEX(vararg extraGroups: String, extraFilter: InclusiveRepositoryContentDescriptor.() -> Unit = {}) = exclusive(cursemaven(), "curse.maven", *extraGroups, theFilter = extraFilter)
fun RepositoryHandler.modrinth(action: MavenArtifactRepository.() -> Unit = {}) = maven("modrinth", "https://mvn.falsepattern.com/modrinth/", action)
fun RepositoryHandler.modrinthEX(vararg extraGroups: String, extraFilter: InclusiveRepositoryContentDescriptor.() -> Unit = {}) = exclusive(modrinth(), "maven.modrinth", *extraGroups, theFilter = extraFilter)
fun RepositoryHandler.mavenpattern(action: MavenArtifactRepository.() -> Unit = {}) = maven("mavenpattern", "https://mvn.falsepattern.com/releases/", action)
fun RepositoryHandler.jitpack(action: MavenArtifactRepository.() -> Unit = {}) = maven("jitpack", "https://mvn.falsepattern.com/jitpack/", action)
fun RepositoryHandler.mega(action: MavenArtifactRepository.() -> Unit = {}) = maven("mega", "https://mvn.falsepattern.com/gtmega_releases/", action)

fun RepositoryHandler.exclusive(repo: ArtifactRepository, vararg groups: String, theFilter: InclusiveRepositoryContentDescriptor.() -> Unit = {}) {
    exclusiveContent {
        forRepositories(repo)
        filter {
            includeGroups(*groups)
            theFilter()
        }
    }
}

fun InclusiveRepositoryContentDescriptor.includeGroups(vararg groups: String) {
    for (group in groups) {
        includeGroup(group)
    }
}
