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
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.JavaPlugin
import java.net.URI

fun RepositoryHandler.maven(name: String, url: URI, action: MavenArtifactRepository.() -> Unit = {}) {
    action.invoke(maven {
        this.name = name
        this.url = url
    })
}

fun DependencyHandler.implementationSplit(dependencyNotation: String, action: Dependency?.() -> Unit = {}) {
    action.invoke(add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, "$dependencyNotation:api"))
    action.invoke(add(JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME, "$dependencyNotation:dev"))
}
fun DependencyHandler.apiSplit(dependencyNotation: String, action: Dependency?.() -> Unit = {}) {
    action.invoke(add(JavaPlugin.COMPILE_ONLY_API_CONFIGURATION_NAME, "$dependencyNotation:api"))
    action.invoke(add(JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME, "$dependencyNotation:dev"))
}

fun DependencyHandler.deobfCurse(dependencyNotation: String): Any {
    return rfg.deobf("curse.maven:$dependencyNotation")
}

fun ModuleDependency.excludeDeps() {
    isTransitive = false
}