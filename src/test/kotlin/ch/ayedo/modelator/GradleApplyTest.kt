package ch.ayedo.modelator

import ch.ayedo.modelator.core.configuration.Configuration
import ch.ayedo.modelator.core.configuration.DockerConfig
import ch.ayedo.modelator.core.configuration.HealthCheckConfig
import ch.ayedo.modelator.core.configuration.MigrationConfig
import ch.ayedo.modelator.core.configuration.MigrationEngine
import ch.ayedo.modelator.core.configuration.PortMapping
import ch.ayedo.modelator.gradle.ModelatorTask
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.testng.Assert.assertTrue
import org.testng.annotations.Test
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class GradleApplyTest {

    @Test
    fun addPluginToProject() {

        val project = ProjectBuilder.builder().build()

        project.pluginManager.apply("ch.ayedo.modelator")

        val taskLookup = project.task(hashMapOf("type" to ModelatorTask::class.java), "generateMetamodel")

        assertTrue(taskLookup is ModelatorTask)

    }

    private fun buildFileFromConfiguration(config: Configuration) =
        """
            plugins {
                id 'ch.ayedo.modelator'
            }

            modelator {

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
        """.trimIndent()

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

    private fun fileExists(fileName: String) = File(fileName).exists()

    private fun assertFileExists(fileName: String) {
        assertTrue(fileExists(fileName), "Expected file does not exist.")
    }

    private fun getResourcePath(path: String): Path = Paths.get(this.javaClass.getResource(path).toURI())

    @Test
    fun gradleRunTest() {

        // TODO: with(tempdir)?
        val tempDir = TemporaryFolder()

        tempDir.create()

        val configFile = tempDir.newFile("jooqConfig.xml")

        configFile.writeText(jooqPostgresConfig(tempDir.root.absolutePath))

        val config = Configuration(
            dockerConfig = DockerConfig(
                tag = "postgres:9.5",
                env = listOf("POSTGRES_DB=postgres", "POSTGRES_USER=postgres", "POSTGRES_PASSWORD=secret"),
                portMapping = PortMapping(5432, 5432)),
            healthCheckConfig = HealthCheckConfig(),
            migrationConfig = MigrationConfig(engine = MigrationEngine.FLYWAY, migrationsPath = getResourcePath("/migrations")),
            jooqConfigPath = configFile.toPath()
        )

        val buildFile = tempDir.newFile("build.gradle")

        val buildFileText = buildFileFromConfiguration(config)

        buildFile.writeText(buildFileText)

        val result = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(tempDir.root)
            .withArguments("generateMetamodel")
            .build()

        println(result.output)

        assertTrue(result.task(":generateMetamodel")?.outcome == SUCCESS)

        assertFileExists(tempDir.root.absolutePath + "/ch/ayedo/modelator/test/tables/Tab.java")

        // TODO: why is there no console output from the jOOQ generator, and the migrators?
    }
}