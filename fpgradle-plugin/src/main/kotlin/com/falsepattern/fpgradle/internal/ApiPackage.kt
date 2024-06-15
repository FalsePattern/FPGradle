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

package com.falsepattern.fpgradle.internal

import com.falsepattern.fpgradle.*
import com.falsepattern.fpgradle.verifyPackage
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*

class ApiPackage: FPPlugin() {
    override fun Project.onPluginInit() {
        tasks.register<Jar>("apiJar").configure {
            val group = mc.mod.rootPkg.map { it.replace('.', '/') }.get()
            val pkgs = mc.api.packages.map { it.map { str -> str.replace('.', '/') } }.get()
            val pkgsNoRecurse = mc.api.packagesNoRecurse.map { it.map { str -> str.replace('.', '/') } }.get()

            from(sourceSets.getByName("main").allSource) {
                for (pkg in pkgs)
                    include("$group/$pkg/**")
                for (pkg in pkgsNoRecurse)
                    include("$group/$pkg/*")
            }

            from(sourceSets.getByName("main").output) {
                for (pkg in pkgs)
                    include("$group/$pkg/**")
                for (pkg in pkgsNoRecurse)
                    include("$group/$pkg/*")

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
            verifyPackage(pkg, "apiPackages")
        for (pkg in mc.api.packagesNoRecurse.get())
            verifyPackage(pkg, "apiPackagesNoRecurse")
        artifacts {
            add("archives", tasks.named("apiJar"))
        }
    }
}
