package ch.ayedo.jooqmodelator.core

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.exception.NotFoundException

fun DockerClient.checkAndPullImage(tag: String) {
    try {
        this.inspectImageCmd(tag).exec()
    } catch (e: NotFoundException) {
        this.pullImageCmd(tag).exec(PullImageResultCallback()).awaitCompletion()
    }
}

fun <T> DockerClient.useContainer(containerId: String, fn: () -> T) =
    try {
        this.restartContainerCmd(containerId).exec()
        fn()
    } finally {
        val info = this.inspectContainerCmd(containerId).exec()
        if (info.state.running == true) {
            this.stopContainerCmd(containerId).exec()
        }
    }

fun DockerClient.findLabeledContainers(key: String, value: String) = this
    .listContainersCmd()
    .withShowAll(true)
    .withLabelFilter(mapOf(key to value))
    .exec()








