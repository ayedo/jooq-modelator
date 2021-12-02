package ch.ayedo.jooqmodelator.gradle

import ch.ayedo.jooqmodelator.core.Modelator
import ch.ayedo.jooqmodelator.core.configuration.Configuration
import ch.ayedo.jooqmodelator.core.configuration.DockerConfig
import ch.ayedo.jooqmodelator.core.configuration.HealthCheckConfig
import ch.ayedo.jooqmodelator.core.configuration.MigrationConfig
import ch.ayedo.jooqmodelator.core.configuration.MigrationEngine
import ch.ayedo.jooqmodelator.core.configuration.PortMapping
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path


@Suppress("SpellCheckingInspection")
open class JooqModelatorTask : DefaultTask() {

    @InputFile
    lateinit var jooqConfigPath: Path

    @OutputDirectory
    lateinit var jooqOutputPath: Path

    @Input
    lateinit var jooqEntitiesPath: String

    @InputFiles
    lateinit var migrationsPaths: List<Path>

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

    @Input
    lateinit var jooqClasspath: List<URL>

    @TaskAction
    fun generateMetamodel() {

        val dockerConfig =
            DockerConfig(dockerTag, dockerLabelKey, dockerEnv, PortMapping(dockerHostPort, dockerContainerPort))

        val healthCheckConfig = HealthCheckConfig(delayMs, maxDurationMs, sql)

        val migrationsConfig = MigrationConfig(MigrationEngine.valueOf(migrationEngine), migrationsPaths)

        val config = Configuration(dockerConfig, healthCheckConfig, migrationsConfig, jooqConfigPath, jooqEntitiesPath)

        val classLoader = URLClassLoader(jooqClasspath.toTypedArray(), this.javaClass.classLoader)

        Thread.currentThread().contextClassLoader = classLoader

        Modelator(config).generate()
    }
}