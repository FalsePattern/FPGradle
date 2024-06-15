package com.falsepattern.fpgradle.module.lombok

import com.falsepattern.fpgradle.FPPlugin
import com.falsepattern.fpgradle.ext
import io.freefair.gradle.plugins.lombok.LombokExtension
import io.freefair.gradle.plugins.lombok.LombokPlugin
import org.gradle.api.Project
import org.gradle.api.Task
import kotlin.reflect.KClass

class FPLombokPlugin: FPPlugin() {
    override fun addPlugins() = listOf(LombokPlugin::class)

    override fun addTasks() = mapOf(Pair("extractLombokConfig", ExtractLombokConfigTask::class))

    override fun onPluginInit(project: Project): Unit = with(project) {
        ext<LombokExtension>().version.convention("1.18.32")
    }
}