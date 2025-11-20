/*
 * FPGradle
 *
 * Copyright (C) 2024-2025 FalsePattern
 * All Rights Reserved
 *
 * The above copyright notice and this permission notice
 * shall be included in all copies or substantial portions of the Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
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
import org.gradle.api.flow.FlowScope
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.inject.Inject


@Suppress("UnstableApiUsage")
abstract class FPUpdatesPlugin: FPPlugin() {
    abstract class UpdateCheck: FlowAction<UpdateCheck.Parameters> {
        interface Parameters: FlowParameters {
            @Input
            fun getMetadata(): Property<Metadata>
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
Current:   $localVersion
Latest:    $releaseVersion
Changelog: https://github.com/FalsePattern/FPGradle/blob/master/CHANGELOG.MD
            """)
            }
        }
    }

    @Inject
    protected abstract fun getFlowScope(): FlowScope

    override fun Project.onPluginPostInitBeforeDeps() {
        if (!mc.updates.check.get() || gradle.startParameter.isOffline)
            return
        val metadata = CompletableFuture.supplyAsync {
            try {
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
            } catch (_: Exception) {
                null
            }
        }

        getFlowScope().always(UpdateCheck::class.java) {
            parameters.getMetadata().set(provider {
                try {
                    metadata.get(2, TimeUnit.SECONDS)
                } catch(_: TimeoutException) {
                    null
                }
            })
        }
    }

    companion object {
        private val metaURL = URI.create("https://plugins.gradle.org/m2/com/falsepattern/fpgradle-plugin/maven-metadata.xml")
    }
}
