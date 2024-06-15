package com.falsepattern.fpgradle.internal

import com.falsepattern.fpgradle.ext
import com.falsepattern.fpgradle.mc
import com.falsepattern.fpgradle.verifyPackage
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*

class ApiPackage(ctx: ConfigurationContext): InitTask {
    private val project = ctx.project

    override fun init(): Unit = with(project) {
        tasks.register<Jar>("apiJar").configure {
            val sourceSets = ext<SourceSetContainer>()
            val group = mc.mod.rootPkg.map { it.replace('.', '/') }.get()
            val pkgs = mc.api.packages.map { it.map { str -> str.replace('.', '/') } }.get()
            val pkgsNoRecurse = mc.api.packagesNoRecurse.map { it.map { str -> str.replace('.', '/') } }.get()

            from(sourceSets.getByName("main").allSource) {
                for (pkg in pkgs)
                    include("$group/$pkg/**")
                for (pkg in pkgsNoRecurse)
                    include("$group/$pkg/*")
            }

            from(sourceSets.getByName("main").output) {
                for (pkg in pkgs)
                    include("$group/$pkg/**")
                for (pkg in pkgsNoRecurse)
                    include("$group/$pkg/*")

    }
            from(sourceSets.getByName("main").resources.srcDirs) {
                include("LICENSE")
            }

            archiveClassifier = "api"
        }
    }

    override fun postInit() = with(project) {
        if (mc.api.packages.get().isEmpty() &&
            mc.api.packagesNoRecurse.get().isEmpty())
            return
        for (pkg in mc.api.packages.get())
            verifyPackage(pkg, "apiPackages")
        for (pkg in mc.api.packagesNoRecurse.get())
            verifyPackage(pkg, "apiPackagesNoRecurse")
        artifacts {
            add("archives", tasks.named("apiJar"))
        }
    }
}
