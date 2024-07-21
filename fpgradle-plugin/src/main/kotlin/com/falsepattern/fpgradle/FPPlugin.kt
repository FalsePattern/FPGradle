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

package com.falsepattern.fpgradle

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import kotlin.reflect.KClass

abstract class FPPlugin: Plugin<Project> {
    final override fun apply(project: Project) {
        project.afterEvaluate { project.onPluginPostInitBeforeDeps() }
        project.onPluginApplyBeforeDeps()

        project.addPlugins().map{it.java}.forEach(project.pluginManager::apply)
        project.addTasks().mapValues{it.value.java}.forEach { (s, clazz) -> project.tasks.create(s, clazz) }

        project.onPluginApplyAfterDeps()
        project.afterEvaluate { project.onPluginPostInitAfterDeps() }

        project.plugins.withType(javaClass, InternalOnPluginInitAction(project))
    }

    open fun Project.onPluginApplyBeforeDeps() {}

    open fun Project.onPluginApplyAfterDeps() {}

    open fun Project.onPluginInit() {}

    open fun Project.onPluginPostInitBeforeDeps() {}

    open fun Project.onPluginPostInitAfterDeps() {}

    open fun Project.addPlugins(): List<KClass<out Plugin<Project>>> = listOf()

    open fun Project.addTasks(): Map<String, KClass<out Task>> = mapOf()

    class InternalOnPluginInitAction(private val project: Project): Action<FPPlugin> {
        override fun execute(plugin: FPPlugin) = with(plugin) {
            project.onPluginInit()
        }

    }
}