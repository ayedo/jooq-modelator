package ch.ayedo.modelator

import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.HostConfig
import com.spotify.docker.client.messages.PortBinding
import java.nio.file.Path

data class Configuration(val dockerConfig: DockerConfig,
                         val migrationsPath: Path,
                         val jooqConfigPath: Path)

data class DockerConfig(val tag: String,
                        val labelKey: String = "ch.ayedo.JooqMetamodelTask.tag",
                        val env: List<String>,
                        val portMapping: PortMapping) {

    fun toContainerConfig(): ContainerConfig? {
        val hostConfig = createHostConfig(portMapping)

        return ContainerConfig.builder()
                .hostConfig(hostConfig)
                .image(tag)
                .env(env)
                .exposedPorts(portMapping.container.toString())
                .labels(mapOf(labelKey to tag))
                .build()
    }

    private fun createHostConfig(portMapping: PortMapping): HostConfig {

        val defaultPortBinding: PortBinding = PortBinding.of("0.0.0.0", portMapping.container)
        val portBindings = mapOf(portMapping.host.toString() to listOf(defaultPortBinding))

        return HostConfig.builder()
                .portBindings(portBindings)
                .build()
    }
}

data class PortMapping(val host: Int, val container: Int)