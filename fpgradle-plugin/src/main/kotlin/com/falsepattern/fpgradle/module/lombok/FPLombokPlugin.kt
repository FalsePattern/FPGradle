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

package com.falsepattern.fpgradle.module.lombok

import com.falsepattern.fpgradle.FPPlugin
import com.falsepattern.fpgradle.lombok
import io.freefair.gradle.plugins.lombok.LombokPlugin
import org.gradle.api.Project

class FPLombokPlugin: FPPlugin() {
    override fun Project.addPlugins() = listOf(LombokPlugin::class)

    override fun Project.addTasks() = mapOf(Pair("extractLombokConfig", ExtractLombokConfigTask::class))

    override fun Project.onPluginInit() {
        lombok.version.convention("1.18.32")
    }
}