package com.falsepattern.fpgradle.module.git

import com.falsepattern.fpgradle.FPPlugin
import com.falsepattern.fpgradle.currentTimestamp
import com.falsepattern.fpgradle.getValueSource
import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign

class GitPlugin: FPPlugin() {
    private fun getGitTagVersion(project: Project): GitTagVersion? {
        var gitRepoDir = project.rootDir
        if (!gitRepoDir.resolve(".git").exists()) {
            gitRepoDir = gitRepoDir.parentFile!!
        }
        return project.getValueSource(GitTagVersionSource::class) {
            this.gitRepoDir = gitRepoDir
        }.orNull
    }

    override fun onPluginInit(project: Project) {
        project.version = if (project.hasProperty("versionOverride"))
            project.property("versionOverride")!!
        else
            getVersion(project)
    }

    private fun getVersion(project: Project) = getGitTagVersion(project)?.toString() ?: currentTimestamp
}