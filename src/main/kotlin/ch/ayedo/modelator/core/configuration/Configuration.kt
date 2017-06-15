package ch.ayedo.modelator.core.configuration

import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.HostConfig
import com.spotify.docker.client.messages.PortBinding
import java.nio.file.Path

data class Configuration(val dockerConfig: DockerConfig,
                         val healthCheckConfig: HealthCheckConfig,
                         val migrationConfig: MigrationConfig,
                         val jooqConfigPath: Path)

data class MigrationConfig(val engine: MigrationEngine, val migrationsPath: Path)

enum class MigrationEngine {
    FLYWAY,
    LIQUIBASE
}

data class DockerConfig(val tag: String,
                        val labelKey: String = "ch.ayedo.JooqMetamodelTask.tag",
                        val env: List<String>,
                        val portMapping: PortMapping) {

    fun toContainerConfig(): ContainerConfig? {
        val hostConfig = createHostConfig()

        return ContainerConfig.builder()
                .hostConfig(hostConfig)
                .image(tag)
                .env(env)
                .exposedPorts(portMapping.container.toString())
                .labels(mapOf(labelKey to tag))
                .build()
    }

    private fun createHostConfig(): HostConfig {

        val defaultPortBinding: PortBinding = PortBinding.of("0.0.0.0", portMapping.container)
        val portBindings = mapOf(portMapping.host.toString() to listOf(defaultPortBinding))

        return HostConfig.builder()
                .portBindings(portBindings)
                .build()
    }
}

data class PortMapping(val host: Int, val container: Int)

data class HealthCheckConfig(val delayMs: Long = 500, val maxDurationMs: Long = 20000, val sql: String = "SELECT 1")
