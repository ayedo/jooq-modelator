package ch.ayedo.modelator

import ch.ayedo.modelator.configuration.*
import ch.ayedo.modelator.configuration.MigrationEngine.FLYWAY
import org.testng.annotations.Test
import java.nio.file.Path
import java.nio.file.Paths

class IntegrationTest {

    @Test
    fun flywayPostgres() {
        MetamodelGenerator(Configuration(
                dockerConfig = DockerConfig(
                        tag = "postgres:9.6",
                        env = listOf("POSTGRES_DB=postgres", "POSTGRES_USER=postgres", "POSTGRES_PASSWORD=secret"),
                        portMapping = PortMapping(5432, 5432)),
                healthCheckConfig = HealthCheckConfig(),
                migrationConfig = MigrationConfig(engine = FLYWAY, migrationsPath = getResourcePath("/migrations")),
                jooqConfigPath = getResourcePath("/jooqConfiguration.xml")
        )).generate()
    }

    private fun getResourcePath(path: String): Path = Paths.get(this.javaClass.getResource(path).toURI())
}