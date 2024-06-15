package com.falsepattern.fpgradle

import org.gradle.api.Project
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass

private val TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

fun resolvePath(className: String) = "src/main/java/${className.replace('.', '/')}"

val currentTimestamp get() = currentTime.formatted

val currentTime: LocalDateTime get() = LocalDateTime.now(ZoneOffset.UTC)

val LocalDateTime.formatted: String get() = this.format(TIMESTAMP_FORMAT)

fun <T, P: ValueSourceParameters> Project.getValueSource(sourceType: KClass<out ValueSource<T, P>>, configuration: P.() -> Unit) =
    providers.of(sourceType.java) {
        configuration(parameters)
    }