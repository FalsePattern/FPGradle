package com.falsepattern.fpgradle.internal

import com.falsepattern.fpgradle.ext
import com.falsepattern.fpgradle.mc
import org.gradle.api.plugins.JavaPluginExtension

class SourcesPublish(ctx: ConfigurationContext): InitTask {
    private val project = ctx.project

    override fun postInit() = with(project) {
        if (!mc.publish.maven.sources.get())
            return

         val java = ext<JavaPluginExtension>()
        java.withSourcesJar()

        artifacts {
            add("archives", tasks.named("sourcesJar"))
        }
    }
}