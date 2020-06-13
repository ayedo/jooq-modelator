@file:Suppress("SpellCheckingInspection")

package ch.ayedo.jooqmodelator

import ch.ayedo.jooqmodelator.IntegrationTest.Database.MARIADB
import ch.ayedo.jooqmodelator.IntegrationTest.Database.POSTGRES
import ch.ayedo.jooqmodelator.core.configuration.Configuration
import ch.ayedo.jooqmodelator.core.configuration.DockerConfig
import ch.ayedo.jooqmodelator.core.configuration.HealthCheckConfig
import ch.ayedo.jooqmodelator.core.configuration.MigrationConfig
import ch.ayedo.jooqmodelator.core.configuration.MigrationEngine
import ch.ayedo.jooqmodelator.core.configuration.PortMapping
import org.gradle.internal.impldep.org.junit.Rule
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

private const val PG_DEFAULT_PORT = 5432
private const val MARIADB_DEFAULT_PORT = 3306

class IntegrationTest {

    @Rule
    private val tempDir = TemporaryFolder().also { it.create() }

    private val jooqPackageName = "ch.ayedo.jooqmodelator.test"

    private val jooqPackagePath = "/" + jooqPackageName.replace(".", "/")

    @Test
    fun flywayPostgres() {

        val database = POSTGRES
        val config = createJooqConfig(database)
            .asConfig(MigrationEngine.FLYWAY) {
                newPostgresConfig()
            }

        createBuildFile(config)

        assertBuildOutcome(SUCCESS)

        assertExistingTables(database, "Tab", "Tabtwo")

    }

    @Test
    fun liquibasePostgres() {
        val database = POSTGRES
        val config = createJooqConfig(database)
            .asConfig(MigrationEngine.LIQUIBASE) {
                newPostgresConfig()
            }

        createBuildFile(config)

        assertBuildOutcome(SUCCESS)

        assertExistingTables(database, "Tab", "Tabtwo")

    }

    @Test
    fun flywayMariaDb() {

        val database = MARIADB
        val config = createJooqConfig(database)
            .asConfig(MigrationEngine.FLYWAY) {
                newMariaDbConfig()
            }

        createBuildFile(config)

        assertBuildOutcome(SUCCESS)

        assertExistingTables(database, "Tab", "Tabtwo")

    }

    @Test
    fun liquibaseMariaDb() {

        val database = MARIADB
        val config = createJooqConfig(database)
            .asConfig(MigrationEngine.LIQUIBASE) {
                newMariaDbConfig()
            }

        createBuildFile(config)

        assertBuildOutcome(SUCCESS)

        assertExistingTables(database, "Tab", "Tabtwo")

    }



    @Test
    fun incrementalBuildTest() {

        val database = POSTGRES
        val config = createJooqConfig(database)
            .asConfig(MigrationEngine.FLYWAY) {
                newPostgresConfig()
            }

        createBuildFile(config)

        assertBuildOutcome(SUCCESS)

        assertBuildOutcome(UP_TO_DATE)

        assertExistingTables(database, "Tab", "Tabtwo")

    }

    @Test
    fun incrementalBuildChangeFilesTest() {

        val additionalMigrationsDir = tempDir.newFolder("migrationsC").toPath()

        val database = POSTGRES
        val config = createJooqConfig(database)
            .asConfig(MigrationEngine.FLYWAY, listOf(additionalMigrationsDir)) {
                newPostgresConfig()
            }

        createBuildFile(config)

        assertBuildOutcome(SUCCESS)

        assertBuildOutcome(UP_TO_DATE)

        assertExistingTables(database, "Tab")
        assertNotExistingTables(database, "Tabtwo")

        Files.copy(migrationsFromResources("/migrationsB/V2__flyway_test.sql").first(), additionalMigrationsDir.resolve("V2__flyway_test.sql"), REPLACE_EXISTING)

        assertBuildOutcome(SUCCESS)

        assertExistingTables(database, "Tabtwo")

    }

    private fun assertExistingTables(database : Database, vararg tableNames: String) =
        tableNames.forEach {
            Assertions.assertTrue(
                fileExists("${tempDir.root.absolutePath}$jooqPackagePath/${database.subdir}/tables/$it.java"),
                "Expected file does not exist."
            )
        }

    private fun assertNotExistingTables(database : Database, vararg tableNames: String) =
        tableNames.forEach {
            Assertions.assertFalse(
                fileExists("${tempDir.root.absolutePath}$jooqPackagePath/${database.subdir}/tables/$it.java"),
                "File was expected to not exist."
            )
        }

    @Test
    fun changePortsTest() {

        val firstPort = 2346
        val secondPort = PG_DEFAULT_PORT

        val config = createJooqConfig(POSTGRES, port = firstPort)
            .asConfig(MigrationEngine.FLYWAY) {
                newPostgresConfig(hostPort = firstPort)
            }

        createBuildFile(config)

        assertBuildOutcome(SUCCESS)


        val newConfig = createJooqConfig(POSTGRES, port = secondPort)
            .asConfig(MigrationEngine.FLYWAY) {
                newPostgresConfig(hostPort = secondPort)
            }

        createBuildFile(newConfig)

        assertBuildOutcome(SUCCESS)

    }

    private fun createJooqConfig(
        database: Database,
        databaseName: String? = null,
        port: Int? = null,
        user: String? = null,
        password: String? = null): File {

        File("${tempDir.root.absolutePath}/jooqConfig.xml").delete()

        return tempDir.newFile("jooqConfig.xml").also {
            val configFilePath = tempDir.root.absolutePath

            val content = when (database) {
                POSTGRES -> jooqPostgresConfig(configFilePath,
                    port ?: PG_DEFAULT_PORT,
                    databaseName ?: "postgres",
                    user ?: "postgres",
                    password ?: "secret")
                MARIADB -> jooqMariaDbConfig(configFilePath,
                    port ?: MARIADB_DEFAULT_PORT,
                    databaseName ?: "maria",
                    user ?: "root",
                    password ?: "pass")
            }

            it.writeText(content)
        }
    }

    /** The [subdir] is part of the path of the generated java classes */
    private enum class Database(val subdir : String) {
        POSTGRES(""),
        MARIADB("maria")
    }

    private fun jooqPostgresConfig(target: String, port: Int = PG_DEFAULT_PORT, database: String, user: String, password: String) = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <configuration>
            <jdbc>
                <driver>org.postgresql.Driver</driver>
                <url>jdbc:postgresql://localhost:$port/$database?loggerLevel=DEBUG</url>
                <user>$user</user>
                <password>$password</password>
            </jdbc>
            <generator>
                <database>
                    <name>org.jooq.meta.postgres.PostgresDatabase</name>
                    <inputSchema>public</inputSchema>
                </database>
                <target>
                    <packageName>$jooqPackageName</packageName>
                    <directory>$target</directory>
                </target>
            </generator>
        </configuration>
    """.trimIndent()

    private fun jooqMariaDbConfig(target: String, port: Int = MARIADB_DEFAULT_PORT, database: String, user: String, password: String) = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <configuration>
            <jdbc>
                <driver>org.mariadb.jdbc.Driver</driver>
                <url>jdbc:mariadb://localhost:$port/$database</url>
                <user>$user</user>
                <password>$password</password>
            </jdbc>
            <generator>
                <database>
                    <name>org.jooq.meta.mariadb.MariaDBDatabase</name>
                </database>
                <target>
                    <packageName>$jooqPackageName</packageName>
                    <directory>$target</directory>
                </target>
            </generator>
        </configuration>
    """.trimIndent()


    private fun migrationsFromResources(vararg paths: String): List<Path> = paths.map { path -> getResourcePath(path) }

    private fun fileExists(fileName: String) = File(fileName).exists()

    private fun getResourcePath(path: String): Path = Paths.get(this.javaClass.getResource(path).toURI())

    private fun createBuildFile(config: Configuration) {

        File("${tempDir.root.absolutePath}/build.gradle").delete()

        tempDir.newFile("build.gradle").also {
            val buildFileText = buildFileFromConfiguration(config, tempDir.root.absolutePath + jooqPackagePath)

            it.writeText(buildFileText)
        }
    }

    private fun assertBuildOutcome(targetOutcome: TaskOutcome) {

        val result = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(tempDir.root)
            .withArguments("generateJooqMetamodel", "--stacktrace")
            .withDebug(true)
            .build()

        Assertions.assertTrue(result.task(":generateJooqMetamodel")?.outcome == targetOutcome)

        println(result.output)
    }

    private fun newPostgresConfig(hostPort: Int = PG_DEFAULT_PORT): DockerConfig = DockerConfig(
        tag = "postgres:9.5",
        env = listOf("POSTGRES_DB=postgres", "POSTGRES_USER=postgres", "POSTGRES_PASSWORD=secret"),
        portMapping = PortMapping(hostPort, PG_DEFAULT_PORT))

    private fun newMariaDbConfig(): DockerConfig = DockerConfig(
        tag = "mariadb:10.2",
        env = listOf("MYSQL_DATABASE=maria", "MYSQL_ROOT_PASSWORD=pass", "MYSQL_PASSWORD=pass"),
        portMapping = PortMapping(MARIADB_DEFAULT_PORT, MARIADB_DEFAULT_PORT))

    private fun buildFileFromConfiguration(config: Configuration, jooqOutputPath: String, jooqVersion: String = "3.11.4", jooqEdition: String = "OSS") =
        """
            plugins {
                id 'ch.ayedo.jooqmodelator'
            }

            jooqModelator {

                jooqVersion = '$jooqVersion'

                jooqEdition = '$jooqEdition'

                jooqConfigPath = '${config.jooqConfigPath}'

                jooqOutputPath = '$jooqOutputPath'

                migrationsPaths = ${config.migrationConfig.migrationsPaths.joinToString(prefix = "[", postfix = "]") { "'$it'" }}

                dockerTag = '${config.dockerConfig.tag}'

                dockerEnv = ${config.dockerConfig.env.joinToString(prefix = "[", postfix = "]") { "'$it'" }}

                dockerHostPort = ${config.dockerConfig.portMapping.host}

                dockerContainerPort = ${config.dockerConfig.portMapping.container}

                labelKey = '${config.dockerConfig.labelKey}'

                migrationEngine = '${config.migrationConfig.engine}'

                delayMs = ${config.healthCheckConfig.delayMs}

                maxDurationMs = ${config.healthCheckConfig.maxDurationMs}

                sql = '${config.healthCheckConfig.sql}'

            }

            repositories {
                jcenter()
            }

            dependencies {
                jooqModelatorRuntime('org.postgresql:postgresql:42.2.4')
                jooqModelatorRuntime('org.mariadb.jdbc:mariadb-java-client:2.2.6')
            }

        """.trimIndent()

    private fun File.asConfig(
        migrationEngine: MigrationEngine,
        additionalMigrations : List<Path> = emptyList(),
        dockerConfigProvider: () -> DockerConfig
    ): Configuration =
        Configuration(
            dockerConfig = dockerConfigProvider(),
            healthCheckConfig = HealthCheckConfig(),
            migrationConfig = MigrationConfig(engine = migrationEngine, migrationsPaths = getMigrationPaths(additionalMigrations)),
            jooqConfigPath = toPath()
        )

    private fun getMigrationPaths(additionalMigrations : List<Path>) : List<Path> = if (additionalMigrations.isEmpty())
        migrationsFromResources("/migrations", "/migrationsB")
    else
        migrationsFromResources("/migrations") + additionalMigrations
}
