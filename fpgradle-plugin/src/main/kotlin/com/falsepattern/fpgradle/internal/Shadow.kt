package com.falsepattern.fpgradle.internal

import com.falsepattern.fpgradle.mc
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.gtnewhorizons.retrofuturagradle.mcp.ReobfuscatedJar
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.component.ConfigurationVariantDetails
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named

class Shadow(ctx: ConfigurationContext): InitTask {
    private val project = ctx.project
    private val manifestAttributes = ctx.manifestAttributes

    override fun init() {
        setupShadowJarTask()
    }

    override fun postInit() {
        if (project.configurations.getByName("shadowImplementation").dependencies.isEmpty())
            return

        setupConfigCoupling()
        setupArtifactRouting()
    }

    private fun setupShadowJarTask() = with(project) {
        configurations {
            create("shadowImplementation")
        }
        val shadowImplementation = configurations.named("shadowImplementation")
        val empty = shadowImplementation.map { it.dependencies.isEmpty() }
        tasks {
            named<ShadowJar>("shadowJar").configure {
                manifest {
                    attributes(manifestAttributes.get())
                }

                if (mc.shadow.minimize.get())
                    minimize()

                configurations {
                    add(shadowImplementation.get())
                }

                archiveClassifier = "dev"

                if (mc.shadow.relocate.get()) {
                    relocationPrefix = "${mc.mod.group.get()}.shadow"
                    isEnableRelocation = true
                }
                dependsOn("removeStub")

                onlyIf {
                    !empty.get()
                }
            }

            named<Jar>("jar").configure {
                dependsOn("removeStub")
                if (!empty.get())
                    archiveClassifier = "dev-preshadow"
            }

            named<ReobfuscatedJar>("reobfJar").configure {
                if (!empty.get()) {
                    inputJar = named<ShadowJar>("shadowJar").flatMap(AbstractArchiveTask::getArchiveFile)
                    dependsOn("shadowJar")
                }
            }
        }
    }

    private fun setupConfigCoupling() = with(project) {
        configurations {
            for (classpath in classpaths) {
                named(classpath).configure {
                    extendsFrom(getByName("shadowImplementation"))
                }
            }
        }
    }

    private fun setupArtifactRouting() = with(project) {
        configurations {
            getByName("runtimeElements").outgoing.artifacts.clear()
            getByName("apiElements").outgoing.artifacts.clear()
            getByName("runtimeElements").outgoing.artifact(tasks.named<ShadowJar>("shadowJar"))
            getByName("apiElements").outgoing.artifact(tasks.named<ShadowJar>("shadowJar"))

            val javaComponent = components.findByName("java")!! as AdhocComponentWithVariants
            javaComponent.withVariantsFromConfiguration(getByName("shadowRuntimeElements"), ConfigurationVariantDetails::skip)
        }
    }

    companion object {
        private val classpaths = listOf(
            "compileClasspath",
            "runtimeClasspath",
            "testCompileClasspath",
            "testRuntimeClasspath"
        )
    }
}