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
import com.falsepattern.fpgradle.sourceSets
import com.falsepattern.fpgradle.verifyPackage
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.register

class ApiPackage: FPPlugin() {
    override fun Project.onPluginInit() {
        tasks.register<Jar>("apiJar").configure {
            val group = mc.api.ignoreRootPkg.flatMap { if (it) provider { "" } else mc.mod.rootPkg.map { pkg -> pkg.replace('.', '/') + "/" }}
            val pkgs = mc.api.packages.map { it.map { str -> str.replace('.', '/') } }
            val pkgsNoRecurse = mc.api.packagesNoRecurse.map { it.map { str -> str.replace('.', '/') } }

            from(sourceSets.getByName("main").allSource) {
                for (pkg in pkgs.get())
                    include("${group.get()}$pkg/**")
                for (pkg in pkgsNoRecurse.get())
                    include("${group.get()}$pkg/*")
            }

            from(sourceSets.getByName("main").output) {
                for (pkg in pkgs.get())
                    include("${group.get()}$pkg/**")
                for (pkg in pkgsNoRecurse.get())
                    include("${group.get()}$pkg/*")

            }
            from(sourceSets.getByName("main").resources.srcDirs) {
                include("LICENSE")
            }

            archiveClassifier = "api"
        }
    }

    override fun Project.onPluginPostInitAfterDeps() {
        if (mc.api.packages.get().isEmpty() &&
            mc.api.packagesNoRecurse.get().isEmpty())
            return
        for (pkg in mc.api.packages.get())
            verifyPackage(pkg, "apiPackages", mc.api.ignoreRootPkg.get())
        for (pkg in mc.api.packagesNoRecurse.get())
            verifyPackage(pkg, "apiPackagesNoRecurse", mc.api.ignoreRootPkg.get())
        artifacts {
            add("archives", tasks.named("apiJar"))
        }
    }
}
