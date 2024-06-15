package com.falsepattern.fpgradle.internal

import com.falsepattern.fpgradle.ext
import com.falsepattern.fpgradle.mc
import com.falsepattern.fpgradle.modrinth
import com.gtnewhorizons.retrofuturagradle.MinecraftExtension
import com.modrinth.minotaur.dependencies.DependencyType.REQUIRED
import com.modrinth.minotaur.dependencies.ModDependency
import org.gradle.kotlin.dsl.*

class ModrinthPublish(ctx: ConfigurationContext): InitTask {
    private val project = ctx.project

    override fun postInit() = with(project) {
        val projectId = mc.publish.modrinth.projectId
        val token = System.getenv(mc.publish.modrinth.tokenEnv.get())
        if (projectId.isPresent && token != null) {
            with(modrinth) {
                this.token = token
                this.projectId = projectId
                versionNumber = mc.mod.version
                versionType = mc.mod.version.map {
                    when {
                        it.contains("-a") -> "alpha"
                        it.contains("-b") -> "beta"
                        else -> "release"
                    }
                }
                changelog = mc.publish.changelog
                uploadFile.set(tasks.named("jar"))
                gameVersions.add(ext<MinecraftExtension>().mcVersion)
                loaders.add("forge")
                for (dep in mc.publish.modrinth.dependencies.get()) {
                    dependencies.add(dep())
                }
                if (mc.mixin.use)
                    dependencies.add(ModDependency("ghjoiQAl", REQUIRED))
            }
            tasks.named("modrinth").configure {
                dependsOn("build")
            }
            tasks.named("publish").configure {
                dependsOn("modrinth")
            }
        }
    }
}