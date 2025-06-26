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

package com.falsepattern.fpgradle.internal

import com.falsepattern.fpgradle.FPPlugin
import com.falsepattern.fpgradle.mc
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.kotlin.dsl.*

class ReproducibleJars: FPPlugin() {
    override fun Project.onPluginInit() {
        val reproducibleJars = mc.publish.reproducibleJars
        tasks.withType<AbstractArchiveTask>().configureEach {
            if (reproducibleJars.get()) {
                isPreserveFileTimestamps = false
                isReproducibleFileOrder = true
            }
        }
    }
}