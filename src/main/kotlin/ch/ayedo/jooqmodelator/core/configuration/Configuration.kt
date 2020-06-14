package ch.ayedo.jooqmodelator.core.configuration

import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.HostConfig
import com.spotify.docker.client.messages.PortBinding
import java.nio.file.Path

data class Configuration(val dockerConfig: DockerConfig,
    val healthCheckConfig: HealthCheckConfig,
    val migrationConfig: MigrationConfig,
    val jooqConfigPath: Path)

data class MigrationConfig(val engine: MigrationEngine, val migrationsPaths: List<Path>)

enum class MigrationEngine {
    FLYWAY,
    LIQUIBASE
}

@Suppress("SpellCheckingInspection")
data class DockerConfig(val tag: String,
    val labelKey: String = "ch.ayedo.jooqmodelator",
    val env: List<String>,
    val portMapping: PortMapping) {

    val labelValue = "tag=$tag cport=${portMapping.container} hport=${portMapping.host} env=[${env.joinToString()}]"

    fun toContainerConfig(): ContainerConfig? {
        val hostConfig = createHostConfig()

        return ContainerConfig.builder()
            .hostConfig(hostConfig)
            .image(tag)
            .env(env)
            .exposedPorts(portMapping.container.toString())
            .labels(mapOf(labelKey to labelValue))
            .build()
    }

    private fun createHostConfig(): HostConfig {

        val defaultPortBinding: PortBinding = PortBinding.of("0.0.0.0", portMapping.host)
        val portBindings = mapOf(portMapping.container.toString() to listOf(defaultPortBinding))

        return HostConfig.builder()
            .portBindings(portBindings)
            .build()
    }
}

data class PortMapping(val host: Int, val container: Int)

data class HealthCheckConfig(val delayMs: Long = 500, val maxDurationMs: Long = 20000, val sql: String = "SELECT 1")
