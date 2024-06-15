package com.falsepattern.fpgradle.module.git

import com.falsepattern.fpgradle.currentTimestamp

class GitTagVersion(private val version: String, private val isClean: Boolean) {
    override fun toString(): String {
        return if (isClean)
            version
        else
            "$version-$currentTimestamp"
    }
}