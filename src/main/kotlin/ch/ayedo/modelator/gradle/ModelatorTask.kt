package ch.ayedo.modelator.gradle

import ch.ayedo.modelator.core.Modelator
import ch.ayedo.modelator.core.configuration.Configuration
import ch.ayedo.modelator.core.configuration.DockerConfig
import ch.ayedo.modelator.core.configuration.HealthCheckConfig
import ch.ayedo.modelator.core.configuration.MigrationConfig
import ch.ayedo.modelator.core.configuration.MigrationEngine
import ch.ayedo.modelator.core.configuration.PortMapping
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import java.nio.file.Path

open class ModelatorTask : DefaultTask() {

    @InputFile
    lateinit var jooqConfigPath: Path

    @InputDirectory
    lateinit var migrationsPath: Path

    @Input
    lateinit var dockerLabelKey: String

    @Input
    lateinit var dockerTag: String

    @Input
    lateinit var dockerEnv: List<String>

    @Input
    var dockerHostPort: Int = 5432

    @Input
    var dockerContainerPort: Int = 5432

    @Input
    lateinit var migrationEngine: String

    @Input
    var delayMs: Long = 500

    @Input
    var maxDurationMs: Long = 20000

    @Input
    lateinit var sql: String


    @TaskAction
    fun generateMetamodel() {

        val dockerConfig = DockerConfig(dockerTag, dockerLabelKey, dockerEnv, PortMapping(dockerHostPort, dockerContainerPort))

        val healthCheckConfig = HealthCheckConfig(delayMs, maxDurationMs, sql)

        val migrationsConfig = MigrationConfig(MigrationEngine.valueOf(migrationEngine), migrationsPath)

        val config = Configuration(dockerConfig, healthCheckConfig, migrationsConfig, jooqConfigPath)

        Modelator(config).generate()
    }
}