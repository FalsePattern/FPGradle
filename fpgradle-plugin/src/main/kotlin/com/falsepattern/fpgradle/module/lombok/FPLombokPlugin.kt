package com.falsepattern.fpgradle.module.lombok

import com.falsepattern.fpgradle.FPPlugin
import io.freefair.gradle.plugins.lombok.LombokPlugin
import org.gradle.api.Project
import org.gradle.api.Task
import kotlin.reflect.KClass

class FPLombokPlugin: FPPlugin() {
    override fun addPlugins() = listOf(LombokPlugin::class)

    override fun addTasks() = mapOf(Pair("extractLombokConfig", ExtractLombokConfigTask::class))
}