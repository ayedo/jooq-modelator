package ch.ayedo.modelator

import ch.ayedo.modelator.IntegrationTest.Database.MARIADB
import ch.ayedo.modelator.IntegrationTest.Database.POSTGRES
import ch.ayedo.modelator.core.configuration.Configuration
import ch.ayedo.modelator.core.configuration.DockerConfig
import ch.ayedo.modelator.core.configuration.HealthCheckConfig
import ch.ayedo.modelator.core.configuration.MigrationConfig
import ch.ayedo.modelator.core.configuration.MigrationEngine
import ch.ayedo.modelator.core.configuration.PortMapping
import org.gradle.internal.impldep.org.junit.Rule
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class IntegrationTest {

    @Rule
    private val tempDir = TemporaryFolder().also { it.create() }

    @Test
    fun flywayPostgres() {

        val jooqConfig = createJooqConfig(POSTGRES)

        val config = Configuration(
            dockerConfig = DockerConfig(
                tag = "postgres:9.5",
                env = listOf("POSTGRES_DB=postgres", "POSTGRES_USER=postgres", "POSTGRES_PASSWORD=secret"),
                portMapping = PortMapping(5432, 5432)),
            healthCheckConfig = HealthCheckConfig(),
            migrationConfig = MigrationConfig(engine = MigrationEngine.FLYWAY, migrationsPath = getResourcePath("/migrations")),
            jooqConfigPath = jooqConfig.toPath()
        )

        assertSuccessfulBuild(tempDir, config)

        assertFileExists(tempDir.root.absolutePath + "/ch/ayedo/modelator/test/tables/Tab.java")

    }

    @Test
    fun liquibasePostgres() {

        val jooqConfig = createJooqConfig(POSTGRES)

        val config = Configuration(
            dockerConfig = DockerConfig(
                tag = "postgres:9.5",
                env = listOf("POSTGRES_DB=postgres", "POSTGRES_USER=postgres", "POSTGRES_PASSWORD=secret"),
                portMapping = PortMapping(5432, 5432)),
            healthCheckConfig = HealthCheckConfig(),
            migrationConfig = MigrationConfig(engine = MigrationEngine.LIQUIBASE, migrationsPath = getResourcePath("/migrations")),
            jooqConfigPath = jooqConfig.toPath()
        )

        assertSuccessfulBuild(tempDir, config)

        assertFileExists(tempDir.root.absolutePath + "/ch/ayedo/modelator/test/tables/Tab.java")

    }

    @Test
    fun flywayMariaDb() {

        val jooqConfig = createJooqConfig(MARIADB)

        val config = Configuration(
            dockerConfig = DockerConfig(
                tag = "mariadb:10.2",
                env = listOf("MYSQL_DATABASE=maria", "MYSQL_ROOT_PASSWORD=pass", "MYSQL_PASSWORD=pass"),
                portMapping = PortMapping(3306, 3306)),
            healthCheckConfig = HealthCheckConfig(),
            migrationConfig = MigrationConfig(engine = MigrationEngine.FLYWAY, migrationsPath = getResourcePath("/migrations")),
            jooqConfigPath = jooqConfig.toPath()
        )

        assertSuccessfulBuild(tempDir, config)

        assertFileExists(tempDir.root.absolutePath + "/ch/ayedo/modelator/test/maria/tables/Tab.java")

    }

    @Test
    fun liquibaseMariaDb() {

        val jooqConfig = createJooqConfig(MARIADB)

        val config = Configuration(
            dockerConfig = DockerConfig(
                tag = "mariadb:10.2",
                env = listOf("MYSQL_DATABASE=maria", "MYSQL_ROOT_PASSWORD=pass", "MYSQL_PASSWORD=pass"),
                portMapping = PortMapping(3306, 3306)),
            healthCheckConfig = HealthCheckConfig(),
            migrationConfig = MigrationConfig(engine = MigrationEngine.LIQUIBASE, migrationsPath = getResourcePath("/migrations")),
            jooqConfigPath = jooqConfig.toPath()
        )

        assertSuccessfulBuild(tempDir, config)

        assertFileExists(tempDir.root.absolutePath + "/ch/ayedo/modelator/test/maria/tables/Tab.java")

    }

    private fun createJooqConfig(database: Database): File =
        tempDir.newFile("jooqConfig.xml").also {
            val configFilePath = tempDir.root.absolutePath

            val content = when (database) {
                POSTGRES -> jooqPostgresConfig(configFilePath)
                MARIADB -> jooqMariaDbConfig(configFilePath)
            }

            it.writeText(content)
        }

    private enum class Database {
        POSTGRES,
        MARIADB
    }

    private fun jooqPostgresConfig(target: String) = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <configuration>
            <jdbc>
                <driver>org.postgresql.Driver</driver>
                <url>jdbc:postgresql://localhost:5432/postgres?loggerLevel=OFF</url>
                <user>postgres</user>
                <password>secret</password>
            </jdbc>
            <generator>
                <database>
                    <name>org.jooq.meta.postgres.PostgresDatabase</name>
                    <inputSchema>public</inputSchema>
                </database>
                <target>
                    <packageName>ch.ayedo.modelator.test</packageName>
                    <directory>$target</directory>
                </target>
            </generator>
        </configuration>
    """.trimIndent()

    private fun jooqMariaDbConfig(target: String) = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <configuration>
            <jdbc>
                <driver>org.mariadb.jdbc.Driver</driver>
                <url>jdbc:mariadb://localhost:3306/maria</url>
                <user>root</user>
                <password>pass</password>
            </jdbc>
            <generator>
                <database>
                    <name>org.jooq.meta.mariadb.MariaDBDatabase</name>
                </database>
                <target>
                    <packageName>ch.ayedo.modelator.test</packageName>
                    <directory>$target</directory>
                </target>
            </generator>
        </configuration>
    """.trimIndent()


    private fun assertFileExists(fileName: String) {
        Assertions.assertTrue(fileExists(fileName), "Expected file does not exist.")
    }

    private fun fileExists(fileName: String) = File(fileName).exists()

    private fun getResourcePath(path: String): Path = Paths.get(this.javaClass.getResource(path).toURI())

    private fun assertSuccessfulBuild(tempDir: TemporaryFolder, config: Configuration) {

        tempDir.newFile("build.gradle").also {
            val buildFileText = buildFileFromConfiguration(config)

            it.writeText(buildFileText)
        }

        val result = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(tempDir.root)
            .withArguments("generateMetamodel", "--stacktrace")
            .withDebug(true)
            .build()

        Assertions.assertTrue(result.task(":generateMetamodel")?.outcome == TaskOutcome.SUCCESS)

    }

    private fun buildFileFromConfiguration(config: Configuration, jooqVersion: String = "3.11.4", jooqEdition: String = "OSS") =
        """
            plugins {
                id 'ch.ayedo.modelator'
            }

            modelator {

                jooqVersion = '$jooqVersion'

                jooqEdition = '$jooqEdition'

                jooqConfigPath = '${config.jooqConfigPath}'

                migrationsPath = '${config.migrationConfig.migrationsPath}'

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
                modelatorRuntime('org.postgresql:postgresql:42.2.4')
                modelatorRuntime('org.mariadb.jdbc:mariadb-java-client:2.2.6')
            }

        """.trimIndent()
}