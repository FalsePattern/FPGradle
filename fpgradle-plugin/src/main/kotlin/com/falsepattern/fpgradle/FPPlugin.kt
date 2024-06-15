package com.falsepattern.fpgradle

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import kotlin.reflect.KClass

abstract class FPPlugin: Plugin<Project> {
    final override fun apply(project: Project) {
        internalOnPluginApply(project)
        project.plugins.withType(javaClass, InternalOnPluginInitAction(project))
        project.afterEvaluate(::internalOnPluginPostInit)
    }

    open fun onPluginApply(project: Project) {}

    open fun onPluginInit(project: Project) {}

    open fun onPluginPostInit(project: Project) {}

    open fun addPlugins(): List<KClass<out Plugin<Project>>> = listOf()

    open fun addTasks(): Map<String, KClass<out Task>> = mapOf()

    private fun internalOnPluginApply(project: Project) = with(project) {
        addPlugins().map{it.java}.forEach(pluginManager::apply)
        addTasks().mapValues{it.value.java}.forEach { (s, clazz) -> tasks.create(s, clazz) }

        onPluginApply(project)
    }

    private fun internalOnPluginInit(project: Project) {
        onPluginInit(project)
    }

    private fun internalOnPluginPostInit(project: Project) {
        onPluginPostInit(project)
    }

    class InternalOnPluginInitAction(private val project: Project): Action<FPPlugin> {
        override fun execute(plugin: FPPlugin) {
            plugin.internalOnPluginInit(project)
        }

    }
}