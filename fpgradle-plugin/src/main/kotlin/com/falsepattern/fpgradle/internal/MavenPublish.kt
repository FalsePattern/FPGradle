package com.falsepattern.fpgradle.internal

import com.falsepattern.fpgradle.ext
import com.falsepattern.fpgradle.mc
import org.gradle.api.credentials.PasswordCredentials
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.*

class MavenPublish(ctx: ConfigurationContext): InitTask {
    private val project = ctx.project
    private val publishing = project.ext<PublishingExtension>()

    override fun postInit() = with(project) {
        with(publishing) {
            publications {
                create<MavenPublication>("maven") {
                    from(components.getByName("java"))

                    if (mc.api.packages.get().isNotEmpty() || mc.api.packagesNoRecurse.get().isNotEmpty())
                        artifact(tasks.named("apiJar"))

                    groupId = mc.publish.group.get()
                    artifactId = mc.publish.artifact.get()
                    version = mc.publish.version.get()
                }
            }
            repositories {
                if (!mc.publish.repoUrl.isPresent)
                    return@repositories

                maven {
                    url = mc.publish.repoUrl.get()
                    name = mc.publish.repoName.get()
                    val user = System.getenv(mc.publish.userEnv.get())
                    val pass = System.getenv(mc.publish.passEnv.get())
                    if (user != null && pass != null) {
                        credentials {
                            username = user
                            password = pass
                        }
                    } else {
                        credentials(PasswordCredentials::class)
                    }
                }
            }
        }
    }
}