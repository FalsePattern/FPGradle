package com.falsepattern.fpgradle.internal

import org.gradle.kotlin.dsl.*
import org.gradle.api.plugins.JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME as COMPILE_ONLY

class CommonDeps(ctx: ConfigurationContext): InitTask {
    private val project = ctx.project

    override fun init() = with(project) {
        dependencies {
            addProvider(COMPILE_ONLY, provider { "org.jetbrains:annotations:24.1.0" })
        }
    }
}