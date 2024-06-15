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
        val mvn = mc.publish.maven
        with(publishing) {
            publications {
                create<MavenPublication>("maven") {
                    from(components.getByName("java"))

                    if (mc.api.packages.get().isNotEmpty() || mc.api.packagesNoRecurse.get().isNotEmpty())
                        artifact(tasks.named("apiJar"))

                    groupId = mvn.group.get()
                    artifactId = mvn.artifact.get()
                    version = mvn.version.get()
                }
            }
            repositories {
                if (!mvn.repoUrl.isPresent)
                    return@repositories

                maven {
                    url = mvn.repoUrl.get()
                    name = mvn.repoName.get()
                    val user = System.getenv(mvn.userEnv.get())
                    val pass = System.getenv(mvn.passEnv.get())
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