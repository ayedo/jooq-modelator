package ch.ayedo.modelator

import com.spotify.docker.client.DefaultDockerClient
import net.jodah.failsafe.RetryPolicy
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

fun main(args: Array<String>) = println("\n\nHello Kotlin world from Kobalt\n\n")


class MetamodelGenerator(configuration: Configuration) {

    private val dockerConfig = configuration.dockerConfig

    companion object {
        private val retryPolicy = RetryPolicy()
                .withDelay(500, MILLISECONDS)
                .withMaxDuration(20, SECONDS)
    }

    fun connectToDocker() = DefaultDockerClient.fromEnv().build()!!

    fun generate() {

        connectToDocker().use { docker ->
            val tag = dockerConfig.tag

            if (!docker.imageExists(tag)) {
                docker.pull(tag)
            }

            val existingContainers = docker.findLabeledContainers(key = dockerConfig.labelKey, value = tag)

            val containerId =
                    if (existingContainers.isEmpty()) {
                        docker.createContainer(dockerConfig.toContainerConfig()).id()!!
                    } else {
                        existingContainers.sortedBy({ it.created() }).map({ it.id() }).first()
                    }

            docker.useContainer(containerId) {

            }
        }
    }
}