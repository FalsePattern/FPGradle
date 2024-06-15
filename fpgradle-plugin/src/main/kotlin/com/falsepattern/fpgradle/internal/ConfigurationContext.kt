package com.falsepattern.fpgradle.internal

import org.gradle.api.Project
import org.gradle.kotlin.dsl.mapProperty

class ConfigurationContext(val project: Project) {
    val manifestAttributes = project.objects.mapProperty<String, String>()
}