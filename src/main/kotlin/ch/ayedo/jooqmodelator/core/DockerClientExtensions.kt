package ch.ayedo.jooqmodelator.core

import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.DockerClient.ListContainersParam.allContainers
import com.spotify.docker.client.DockerClient.ListImagesParam.byName

fun DockerClient.imageExists(tag: String) = this.listImages(byName(tag)).isNotEmpty()

// like the ".use(...)" extension function on Closable, but for running a container
fun <T> DockerClient.useContainer(containerId: String, fn: () -> T) =
    try {
        this.restartContainer(containerId)
        fn()
    } finally {
        val info = this.inspectContainer(containerId)
        if (info.state().running() == true) {
            this.stopContainer(containerId, 10)
        }
    }

fun DockerClient.findLabeledContainers(key: String, value: String) =
    this.listContainers(allContainers())
        .filter { container ->
            container.labels()?.get(key) == value
        }