package com.falsepattern.fpgradle

import com.matthewprenger.cursegradle.CurseExtension
import com.modrinth.minotaur.ModrinthExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.kotlin.dsl.*
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

inline fun <reified T : Any> Project.ext(): T = extensions.getByType<T>()

val Project.mc: FPMinecraftProjectExtension get() = ext<FPMinecraftProjectExtension>()

val Project.curseforge: CurseExtension get() = ext<CurseExtension>()

val Project.modrinth: ModrinthExtension get() = ext<ModrinthExtension>()

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
