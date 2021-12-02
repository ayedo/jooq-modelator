package ch.ayedo.jooqmodelator.core.configuration

import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import java.nio.file.Path

data class Configuration(
    val dockerConfig: DockerConfig,
    val healthCheckConfig: HealthCheckConfig,
    val migrationConfig: MigrationConfig,
    val jooqConfigPath: Path,
    val jooqEntitiesPath: String
)

data class MigrationConfig(val engine: MigrationEngine, val migrationsPaths: List<Path>)

enum class MigrationEngine {
    FLYWAY,
    LIQUIBASE
}

@Suppress("SpellCheckingInspection")
data class DockerConfig(
    val tag: String,
    val labelKey: String = "ch.ayedo.jooqmodelator",
    val env: List<String>,
    val portMapping: PortMapping
) {

    val labelValue = "tag=$tag cport=${portMapping.container} hport=${portMapping.host} env=[${env.joinToString()}]"

    fun createHostConfig(): HostConfig {
        val portBinding = PortBinding(
            Ports.Binding.bindPort(portMapping.host),
            ExposedPort(portMapping.container)
        )
        return HostConfig.newHostConfig()
            .withPortBindings(portBinding)
    }
}

data class PortMapping(val host: Int, val container: Int)

data class HealthCheckConfig(val delayMs: Long = 500, val maxDurationMs: Long = 20000, val sql: String = "SELECT 1")
