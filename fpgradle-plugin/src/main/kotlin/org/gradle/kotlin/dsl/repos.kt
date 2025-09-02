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

package org.gradle.kotlin.dsl

import org.gradle.api.GradleException
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.InclusiveRepositoryContentDescriptor
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository

private typealias MavenAction = MavenArtifactRepository.() -> Unit
private typealias IvyAction = IvyArtifactRepository.() -> Unit
private typealias ExFilter = InclusiveRepositoryContentDescriptor.() -> Unit

inline fun RepositoryHandler.maven(name: String, url: Any, crossinline action: MavenAction = {}) =
    maven {
        this.name = name
        setUrl(url)
        action()
    }

inline fun RepositoryHandler.mavenNamed(name: String, ifNotPresent: (name: String, action: MavenAction) -> MavenArtifactRepository, crossinline action: MavenAction = {}): MavenArtifactRepository {
    val repo = findByName(name)
    if (repo != null) {
        if (repo !is MavenArtifactRepository) {
            throw GradleException("Repo with name $name is not a maven repository!")
        }
        action.invoke(repo)
        return repo
    }
    return ifNotPresent(name) {
        action.invoke(this@ifNotPresent)
    }
}

inline fun RepositoryHandler.ivy(name: String, url: Any, pattern: String, crossinline action: IvyAction = {}) =
    ivy {
        this.name = name
        setUrl(url)
        patternLayout {
            artifact(pattern)
        }
        metadataSources {
            artifact()
        }
        action()
    }

inline fun RepositoryHandler.ivyNamed(name: String, ifNotPresent: (name: String, action: IvyAction) -> IvyArtifactRepository, crossinline action: IvyAction = {}): IvyArtifactRepository {
    val repo = findByName(name)
    if (repo != null) {
        if (repo !is IvyArtifactRepository) {
            throw GradleException("Repo with name $name is not an ivy repository!")
        }
        action.invoke(repo)
        return repo
    }
    return ifNotPresent(name) {
        action.invoke(this@ifNotPresent)
    }
}

inline fun RepositoryHandler.exclusive(repo: ArtifactRepository, vararg groups: String, crossinline theFilter: ExFilter = {}) {
    exclusiveContent {
        forRepositories(repo)
        filter {
            includeGroups(*groups)
            theFilter()
        }
    }
}

inline fun RepositoryHandler.wellKnownMaven(name: String, url: Any, crossinline action: MavenAction = {}) = mavenNamed(name, { theName, theAction -> maven(theName, url, theAction)}, action)
inline fun RepositoryHandler.wellKnownIvy(name: String, url: Any, pattern: String, crossinline action: IvyAction = {}) = ivyNamed(name, {theName, theAction -> ivy(theName, url, pattern, theAction)}, action)

inline fun RepositoryHandler.cursemaven(crossinline action: MavenAction = {}) = wellKnownMaven("cursemaven", "https://mvn.falsepattern.com/cursemaven/", action)
inline fun RepositoryHandler.cursemavenEX(vararg extraGroups: String, crossinline extraFilter: ExFilter = {}) = exclusive(cursemaven(), "curse.maven", *extraGroups, theFilter = extraFilter)

inline fun RepositoryHandler.modrinth(crossinline action: MavenAction = {}) = wellKnownMaven("modrinth", "https://mvn.falsepattern.com/modrinth/", action)
inline fun RepositoryHandler.modrinthEX(vararg extraGroups: String, crossinline extraFilter: ExFilter = {}) = exclusive(modrinth(), "maven.modrinth", *extraGroups, theFilter = extraFilter)

inline fun RepositoryHandler.mavenpattern(crossinline action: MavenAction = {}) = wellKnownMaven("mavenpattern", "https://mvn.falsepattern.com/releases/", action)
inline fun RepositoryHandler.venmaven(crossinline action: MavenAction = {}) = wellKnownMaven("venmaven", "https://mvn.ventooth.com/releases", action)

inline fun RepositoryHandler.jitpack(crossinline action: MavenAction = {}) = wellKnownMaven("jitpack", "https://mvn.falsepattern.com/jitpack/", action)

inline fun RepositoryHandler.mega(crossinline action: MavenAction = {}) = wellKnownMaven("mega", "https://mvn.falsepattern.com/gtmega_releases/", action)

inline fun RepositoryHandler.mega_uploads(crossinline action: MavenAction = {}) = wellKnownMaven("mega_uploads", "https://mvn.falsepattern.com/gtmega_uploads/", action)

inline fun RepositoryHandler.horizon(crossinline action: MavenAction = {}) = wellKnownMaven("horizon", "https://mvn.falsepattern.com/horizon/", action)

inline fun RepositoryHandler.fp_mirror(crossinline action: IvyAction = {}) = wellKnownIvy("fp_mirror", "https://mvn.falsepattern.com/releases/mirror/", "[orgPath]/[artifact]-[revision].[ext]", action)

inline fun RepositoryHandler.ic2(crossinline action: MavenAction = {}) = wellKnownMaven("ic2", "https://mvn.falsepattern.com/ic2/") {
    metadataSources {
        artifact()
    }
    action()
}

inline fun RepositoryHandler.ic2EX(vararg extraGroups: String, crossinline extraFilter: ExFilter = {}) = exclusive(ic2(), "net.industrial-craft", *extraGroups, theFilter = extraFilter)


fun InclusiveRepositoryContentDescriptor.includeGroups(vararg groups: String) {
    for (group in groups) {
        includeGroup(group)
    }
}

val DependencyHandlerScope.ic2 get() = "net.industrial-craft:industrialcraft-2:2.2.828-experimental:dev"
