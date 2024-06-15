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

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.kotlin.dsl.*
import java.io.InputStream
import java.lang.StringBuilder
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass

private val TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

fun resolvePath(basePath: String, vararg classPaths: String): String {
    val result = StringBuilder(basePath)
    for (classPath in classPaths) {
        result.append('/').append(classPath.replace('.', '/'))
    }
    return result.toString()
}

val currentTimestamp get() = currentTime.formatted

val currentTime: LocalDateTime get() = LocalDateTime.now(ZoneOffset.UTC)

val LocalDateTime.formatted: String get() = this.format(TIMESTAMP_FORMAT)

fun <T, P: ValueSourceParameters> Project.getValueSource(sourceType: KClass<out ValueSource<T, P>>, configuration: P.() -> Unit) =
    providers.of(sourceType.java) {
        configuration(parameters)
    }

private val javaSourceDir = "src/main/java"

fun Project.verifyPackage(thePackage: String, propName: String) {
    val targetPackageJava = resolvePath(javaSourceDir, mc.mod.rootPkg.get(), thePackage)
    if (!file(targetPackageJava).exists())
        throw GradleException("Could not resolve \"$propName\"! Could not find $targetPackageJava")
}

fun Project.verifyClass(theClass: String, propName: String) {
    val targetClassJava = resolvePath(javaSourceDir, mc.mod.rootPkg.get(), theClass) + ".java"
    if (!file(targetClassJava).exists())
        throw GradleException("Could not resolve \"$propName\"! Could not find $targetClassJava")

}

fun Project.verifyFile(theFile: String, propName: String) {
    if (!file(theFile).exists())
        throw GradleException("Could not resolve \"$propName\"! Could not find $theFile")
}

fun getJarResource(resourceName: String): InputStream? {
    return FPPlugin::class.java.classLoader.getResourceAsStream("fpgradle/$resourceName")
}