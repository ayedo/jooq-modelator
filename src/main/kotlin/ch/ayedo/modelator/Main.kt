package ch.ayedo.modelator

import ch.ayedo.modelator.configuration.Configuration
import ch.ayedo.modelator.configuration.DatabaseConfig
import com.spotify.docker.client.DefaultDockerClient
import org.jooq.util.GenerationTool


// TODO: main method which takes a path to a configuration file

class MetamodelGenerator(configuration: Configuration) {

    private val dockerConfig = configuration.dockerConfig

    private val healthCheckConfig = configuration.healthCheckConfig

    private val jooqConfigPath = configuration.jooqConfigPath

    private val databaseConfig = DatabaseConfig.fromJooqConfig(jooqConfigPath)

    private val migrationConfig = configuration.migrationConfig

    private fun connectToDocker() = DefaultDockerClient.fromEnv().build()!!

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
                        // TODO: warn if more than one found
                        existingContainers.sortedBy({ it.created() }).map({ it.id() }).first()
                    }

            docker.useContainer(containerId) {
                waitForDatabase()
                migrateDatabase()
                runJooqGenerator()
            }
        }
    }

    private fun waitForDatabase() {

        val healthChecker = HealthChecker.getDefault(databaseConfig, healthCheckConfig)

        healthChecker.waitForDatabase()
    }

    private fun migrateDatabase() {

        val migrator = Migrator.fromConfig(migrationConfig, databaseConfig)

        with(migrator) {
            clean()
            migrate()
        }
    }

    private fun runJooqGenerator() {

        val jooqConfig = jooqConfigPath.toFile().readText()

        GenerationTool.generate(jooqConfig)
    }

}