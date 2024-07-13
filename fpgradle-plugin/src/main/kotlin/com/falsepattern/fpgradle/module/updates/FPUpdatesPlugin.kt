/*
 * FPGradle
 *
 * Copyright (C) 2024 FalsePattern
 * All Rights Reserved
 *
 * The above copyright notice and this permission notice
 * shall be included in all copies or substantial portions of the Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.falsepattern.fpgradle.module.updates

import com.falsepattern.fpgradle.FPPlugin
import com.falsepattern.fpgradle.mc
import org.apache.maven.artifact.repository.metadata.Metadata
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader
import org.gradle.api.Project
import org.gradle.api.flow.FlowAction
import org.gradle.api.flow.FlowParameters
import org.gradle.api.flow.FlowProviders
import org.gradle.api.flow.FlowScope
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Input
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.lifecycle.BuildPhaseFinishEvent
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import javax.inject.Inject


@Suppress("UnstableApiUsage")
abstract class FPUpdatesPlugin: FPPlugin() {
    abstract class UpdateCheck: FlowAction<UpdateCheck.Parameters> {
        interface Parameters: FlowParameters {
            @Input
            fun getMetadata(): Property<Metadata?>
        }

        override fun execute(parameters: Parameters) {
            val result = parameters.getMetadata().orNull
            val releaseVersion = result?.versioning?.release ?: return
            val localProps = Properties()
            localProps.load(FPUpdatesPlugin::class.java.getResourceAsStream("/fpgradle/version.properties"))
            val localVersion = localProps.getProperty("version")!!
            if (localVersion != releaseVersion) {
                println("""
A new FPGradle version is available!
Current: $localVersion
Latest:  $releaseVersion
            """)
            }
        }
    }

    @Inject
    protected abstract fun getFlowScope(): FlowScope

    override fun Project.onPluginPostInitBeforeDeps() {
        if (!mc.updates.check.get())
            return
        val metadata = CompletableFuture.supplyAsync {
            HttpClient.newBuilder().build().use { client ->
                val response = client.send(
                    HttpRequest.newBuilder().GET().uri(Companion.metaURL).build(),
                    HttpResponse.BodyHandlers.ofInputStream()
                )
                if (response.statusCode() != 200)
                    null
                else
                    MetadataXpp3Reader().read(response.body())
            }
        }

        getFlowScope().always(UpdateCheck::class.java) {
            parameters.getMetadata().set(provider { metadata.get(2, TimeUnit.SECONDS) })
        }
    }

    companion object {
        private val metaURL = URI.create("https://mvn.falsepattern.com/fpgradle/com/falsepattern/fpgradle-plugin/maven-metadata.xml")
    }
}
