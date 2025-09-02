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
import com.falsepattern.jtweaker.JTweakerPlugin
import com.falsepattern.jtweaker.RemoveStubsJar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import kotlin.reflect.KClass

class Stubs: FPPlugin() {
    override fun Project.addPlugins() = listOf(JTweakerPlugin::class)

    override fun Project.onPluginInit() {
        val jar = tasks.named<Jar>("jar") {
            archiveClassifier = "dev-prestub"
        }
        val jarRemoveStubs = tasks.register<RemoveStubsJar>(JAR_STUB_TASK) {
            inputFile = jar.flatMap { it.archiveFile }
            archiveClassifier = "dev"
        }


        for (outgoingConfig in listOf("runtimeElements", "apiElements")) {
            val outgoing = configurations.getByName(outgoingConfig)
            outgoing.outgoing.artifacts.clear()
            outgoing.outgoing.artifact(jarRemoveStubs)
        }
    }

    companion object {
        const val JAR_STUB_TASK = "jarRemoveStubs"
    }
}