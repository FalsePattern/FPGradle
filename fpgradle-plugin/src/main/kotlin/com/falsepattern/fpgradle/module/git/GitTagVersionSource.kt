package com.falsepattern.fpgradle.module.git

import org.eclipse.jgit.api.Git
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.io.File

abstract class GitTagVersionSource: ValueSource<GitTagVersion, GitTagVersionSource.Params> {
    interface Params: ValueSourceParameters {
        val gitRepoDir: Property<File>
    }

    override fun obtain(): GitTagVersion? {
        val gitRepoDir = parameters.gitRepoDir.orNull ?: return null

        return runCatching { Git.open(gitRepoDir).use { git ->
            val repo = git.repository

            val head = repo.exactRef("HEAD") ?: return@use null

            val currentHash = head.objectId

            val isClean = git.status().call().isClean

            if (isClean) {
                val tags = git.tagList().call()
                for (tag in tags) {
                    if (tag.objectId == currentHash) {
                        val version = tag.name.substring(10)

                        return@use GitTagVersion(version, true)
                    }
                }
            }

            val branch = repo.branch
            val shortHash = currentHash.name().substring(0, 7)
            val version = "$branch-$shortHash"
            return@use GitTagVersion(version, isClean)
        } }.getOrNull()
    }
}