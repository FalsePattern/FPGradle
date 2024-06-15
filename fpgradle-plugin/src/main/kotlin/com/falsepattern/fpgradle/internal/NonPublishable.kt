package com.falsepattern.fpgradle.internal

class NonPublishable(ctx: ConfigurationContext): InitTask {
    val project = ctx.project

    override fun init(): Unit = with(project) {
        with(configurations) {
            val devOnlyNonPublishable = create("devOnlyNonPublishable") {
                description = "Runtime and compiletime dependencies that are not published alongside the jar (compileOnly + runtimeOnlyNonPublishable)"
                isCanBeConsumed = false
                isCanBeResolved = false
            }

            val runtimeOnlyNonPublishable = create("runtimeOnlyNonPublishable") {
                description = "Runtime only dependencies that are not published alongside the jar"
                isCanBeConsumed = false
                isCanBeResolved = false
                extendsFrom(devOnlyNonPublishable)
            }


            named("compileOnly") {
                extendsFrom(devOnlyNonPublishable)
            }
            named("runtimeClasspath") {
                extendsFrom(runtimeOnlyNonPublishable)
            }
            named("testRuntimeClasspath") {
                extendsFrom(runtimeOnlyNonPublishable)
            }
        }
    }
}