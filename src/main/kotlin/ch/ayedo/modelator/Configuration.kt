package ch.ayedo.modelator

import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.HostConfig
import com.spotify.docker.client.messages.PortBinding
import java.nio.file.Path

data class PortMapping(val host: Int, val container: Int)

data class Configuration(val dockerConfig: DockerConfig,
                         val migrationsPath: Path,
                         val jooqGeneratorConfigPath: Path,
                         val outputPath: Path)

data class DockerConfig(val tag: String,
                        val labelKey: String = "ch.ayedo.JooqMetamodelTask.tag",
                        val env: Env,
                        val portMapping: PortMapping) {

    fun toContainerConfig(): ContainerConfig? {
        val hostConfig = createHostConfig(portMapping)

        return ContainerConfig.builder()
                .hostConfig(hostConfig)
                .image(tag)
                .env(env.asStringList())
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

class Env(private val keysToValues: Map<String, String>) {
    fun asStringList(): List<String> {
        return keysToValues.entries.map({ (key, value) -> "$key=$value" })
    }
}
