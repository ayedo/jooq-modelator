@file:Suppress("SpellCheckingInspection")

package ch.ayedo.jooqmodelator

import ch.ayedo.jooqmodelator.IntegrationTest.Database.MARIADB
import ch.ayedo.jooqmodelator.IntegrationTest.Database.POSTGRES
import ch.ayedo.jooqmodelator.core.configuration.Configuration
import ch.ayedo.jooqmodelator.core.configuration.DockerConfig
import ch.ayedo.jooqmodelator.core.configuration.HealthCheckConfig
import ch.ayedo.jooqmodelator.core.configuration.MigrationConfig
import ch.ayedo.jooqmodelator.core.configuration.MigrationEngine
import ch.ayedo.jooqmodelator.core.configuration.MigrationEngine.FLYWAY
import ch.ayedo.jooqmodelator.core.configuration.MigrationEngine.LIQUIBASE
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

private const val DEFAULT_JOOQ_VERSION = "3.13.2"
private const val PG_DRIVER_VERSION = "42.2.14"
private const val MARIADB_DRIVER_VERSION = "2.6.0"

class IntegrationTest {

    private enum class Database(
        val version: String,
        val defaultPort: Int,
        val db: String,
        val user: String,
        val password: String,
        val rootPassword: String = "",
        private val dialectVersion: String? = version.replace(".", "_"),
        private val subdir: String = ""
    ) {
        POSTGRES(
            version = "12.3",
            defaultPort = 5432,
            db = "postgres",
            user = "postgres",
            password = "secret",
            dialectVersion = null
        ),
        MARIADB(
            version = "10.2",
            defaultPort = 3306,
            db = "maria",
            user = "root",
            password = "pass",
            rootPassword = "pass",
            subdir = "maria"
        );

        val subdirPath get() = "/$subdir"
        val dialect get() = dialectVersion?.let { "name_$dialectVersion" } ?: name
    }

    @Rule
    private val tempDir = TemporaryFolder().also { it.create() }

    private val jooqPackageName = "ch.ayedo.jooqmodelator.test"

    private val jooqPackagePath = "/" + jooqPackageName.replace(".", "/")

    @Test
    fun flywayPostgres() {

        val database = POSTGRES
        val config = createJooqConfig(database)
            .asConfig(FLYWAY) {
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
            .asConfig(LIQUIBASE) {
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
            .asConfig(FLYWAY) {
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
            .asConfig(LIQUIBASE) {
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
            .asConfig(FLYWAY) {
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
            .asConfig(FLYWAY, migrationPaths = migrationsFromResources("/migrations") + listOf(additionalMigrationsDir)) {
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

    private fun assertExistingTables(database: Database, vararg tableNames: String) =
        tableNames.forEach {
            Assertions.assertTrue(
                fileExists("${tempDir.root.absolutePath}$jooqPackagePath${database.subdirPath}/tables/$it.java"),
                "Expected file does not exist."
            )
        }

    private fun assertNotExistingTables(database: Database, vararg tableNames: String) =
        tableNames.forEach {
            Assertions.assertFalse(
                fileExists("${tempDir.root.absolutePath}$jooqPackagePath${database.subdirPath}/tables/$it.java"),
                "File was expected to not exist."
            )
        }

    @Test
    fun changePortsTest() {

        val firstPort = 2346
        val secondPort = POSTGRES.defaultPort

        val config = createJooqConfig(POSTGRES, port = firstPort)
            .asConfig(FLYWAY) {
                newPostgresConfig(hostPort = firstPort)
            }

        createBuildFile(config)

        assertBuildOutcome(SUCCESS)


        val newConfig = createJooqConfig(POSTGRES, port = secondPort)
            .asConfig(FLYWAY) {
                newPostgresConfig(hostPort = secondPort)
            }

        createBuildFile(newConfig)

        assertBuildOutcome(SUCCESS)

    }

    @Test
    fun liquibaseYamlTest() {

        val config = createJooqConfig(POSTGRES)
            .asConfig(LIQUIBASE, migrationPaths = migrationsFromResources("/liquibase-yml-migrations")) {
                newPostgresConfig()
            }

        createBuildFile(config)

        assertBuildOutcome(SUCCESS)


    }

    private fun createJooqConfig(
        database: Database,
        databaseName: String = database.db,
        port: Int = database.defaultPort,
        user: String = database.user,
        password: String = database.password,
        dialect: String = database.dialect
    ): File {

        File("${tempDir.root.absolutePath}/jooqConfig.xml").delete()

        return tempDir.newFile("jooqConfig.xml").also {
            val configFilePath = tempDir.root.absolutePath

            val content = when (database) {
                POSTGRES -> jooqPostgresConfig(configFilePath, port, databaseName, user, password, dialect)
                MARIADB -> jooqMariaDbConfig(configFilePath, port, databaseName, user, password, dialect)
            }

            it.writeText(content)
        }
    }

    private fun jooqPostgresConfig(target: String, port: Int = POSTGRES.defaultPort, database: String, user: String, password: String, dialect: String) = """
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
                <properties>
                    <property>
                        key = 'dialect'
                        value = '$dialect'
                    </property>
                </properties>
            </generator>
        </configuration>
    """.trimIndent()

    private fun jooqMariaDbConfig(target: String, port: Int = MARIADB.defaultPort, database: String, user: String, password: String, dialect: String) = """
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
                <properties>
                    <property>
                        key = 'dialect'
                        value = '$dialect'
                    </property>
                </properties>
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

    private fun newPostgresConfig(hostPort: Int = POSTGRES.defaultPort): DockerConfig = DockerConfig(
        tag = "postgres:${POSTGRES.version}",
        env = listOf("POSTGRES_DB=${POSTGRES.db}", "POSTGRES_USER=${POSTGRES.user}", "POSTGRES_PASSWORD=${POSTGRES.password}"),
        portMapping = PortMapping(hostPort, POSTGRES.defaultPort))

    private fun newMariaDbConfig(): DockerConfig = DockerConfig(
        tag = "mariadb:${MARIADB.version}",
        env = listOf("MYSQL_DATABASE=${MARIADB.db}", "MYSQL_ROOT_PASSWORD=${MARIADB.rootPassword}", "MYSQL_PASSWORD=${MARIADB.password}"),
        portMapping = PortMapping(MARIADB.defaultPort, MARIADB.defaultPort))

    private fun buildFileFromConfiguration(config: Configuration, jooqOutputPath: String, jooqVersion: String = DEFAULT_JOOQ_VERSION, jooqEdition: String = "OSS") =
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
                jooqModelatorRuntime('org.postgresql:postgresql:$PG_DRIVER_VERSION')
                jooqModelatorRuntime('org.mariadb.jdbc:mariadb-java-client:$MARIADB_DRIVER_VERSION')
                jooqModelatorRuntime('org.yaml:snakeyaml:1.33')
            }

        """.trimIndent()

    private fun File.asConfig(
        migrationEngine: MigrationEngine,
        migrationPaths: List<Path> = migrationsFromResources("/migrations", "/migrationsB"),
        dockerConfigProvider: () -> DockerConfig
    ): Configuration =
        Configuration(
            dockerConfig = dockerConfigProvider(),
            healthCheckConfig = HealthCheckConfig(),
            migrationConfig = MigrationConfig(engine = migrationEngine, migrationsPaths = migrationPaths),
            jooqConfigPath = toPath()
        )

}
