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

import com.falsepattern.fpgradle.*
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.provider.Provider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.register

class ApiPackage: FPPlugin() {
    override fun Project.onPluginInit() {
        tasks.register<Jar>("apiJar").configure {
            val ignoreRootPkg = mc.api.ignoreRootPkg
            val rootPkg = mc.mod.rootPkg
            val group = ignoreRootPkg.flatMap { if (it) provider { "" } else rootPkg.map { pkg -> pkg.replace('.', '/') + "/" } }
            val classes = mc.api.classes.asPathWithPrefix(group)
            val pkgs = mc.api.packages.asPathWithPrefix(group)
            val pkgsNoRecurse = mc.api.packagesNoRecurse.asPathWithPrefix(group)
            val includeSources = mc.api.includeSources
            val config = fun CopySpec.() {
                for (klass in classes.get())
                    include("$klass.*")
                for (pkg in pkgs.get())
                    include("$pkg/**")
                for (pkg in pkgsNoRecurse.get())
                    include("$pkg/*")
            }
            from(sourceSets.getByName("main").allSource) {
                if (includeSources.get()) {
                    config.invoke(this)
                } else {
                    include { false }
                }
            }
            from(sourceSets.getByName("main").output, config)
            from(sourceSets.getByName("main").resources.srcDirs) {
                include("LICENSE")
            }

            archiveClassifier = "api"
        }
    }

    override fun Project.onPluginPostInitAfterDeps() {
        val classes = mc.api.classes.get()
        val packages = mc.api.packages.get()
        val packagesNoRecurse = mc.api.packagesNoRecurse.get()
        val ignoreRootPkg = mc.api.ignoreRootPkg.get()
        if (classes.isEmpty() && packages.isEmpty() && packagesNoRecurse.isEmpty())
            return
        for (klass in classes)
            verifyClass(klass, "api -> classes", ignoreRootPkg)
        for (pkg in packages)
            verifyPackage(pkg, "api -> packages", ignoreRootPkg)
        for (pkg in packagesNoRecurse)
            verifyPackage(pkg, "api -> packagesNoRecurse", ignoreRootPkg)
        artifacts {
            add("archives", tasks.named("apiJar"))
        }
    }
}

private fun Provider<List<String>>.asPathWithPrefix(prefix: Provider<String>): Provider<List<String>> {
    return prefix.flatMap { pfx -> this@asPathWithPrefix.map { it.map { str -> pfx + str.replace('.', '/') } } }
}