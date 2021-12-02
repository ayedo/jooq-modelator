package ch.ayedo.jooqmodelator.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import java.nio.file.Paths

@Suppress("SpellCheckingInspection")
open class JooqModelatorPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        project.extensions.create("jooqModelator", JooqModelatorExtension::class.java)

        val modelatorRuntime = project.configurations.create("jooqModelatorRuntime")

        modelatorRuntime.description = "Add JDBC drivers or generator extensions here."

        project.afterEvaluate {

            val config = project.extensions.findByType(JooqModelatorExtension::class.java)!!

            addJooqDependency(project, modelatorRuntime, config)

            val task = project.tasks.create("generateJooqMetamodel", JooqModelatorTask::class.java).apply {
                description = "Generates the jOOQ metamodel from migrations files using a dockerized database."

                jooqConfigPath = Paths.get(
                    config.jooqConfigPath
                        ?: throw IncompletePluginConfigurationException("path to the jOOQ generator configuration (jooqConfigPath)")
                )

                jooqOutputPath = Paths.get(
                    config.jooqOutputPath
                        ?: throw IncompletePluginConfigurationException("path to the output directory (jooqOutputPath)")
                )

                jooqEntitiesPath = config.jooqEntitiesPath
                    ?: throw IncompletePluginConfigurationException("path to entites directory (jooqEntitiesPath)")

                migrationsPaths = config.migrationsPaths?.map { strPath -> Paths.get(strPath) }
                    ?: throw IncompletePluginConfigurationException("path to the migration files (migrationsPaths)")

                dockerLabelKey = config.labelKey

                dockerEnv = config.dockerEnv

                dockerTag = config.dockerTag
                    ?: throw IncompletePluginConfigurationException("docker image tag (dockerTag)")

                dockerHostPort = config.dockerHostPort
                    ?: throw IncompletePluginConfigurationException("docker host port (dockerHostPort)")

                dockerContainerPort = config.dockerContainerPort
                    ?: throw IncompletePluginConfigurationException("docker container port (dockerContainerPort)")

                migrationEngine = config.migrationEngine
                    ?: throw IncompletePluginConfigurationException("migration engine (migrationEngine)")

                delayMs = config.delayMs

                maxDurationMs = config.maxDurationMs

                sql = config.sql

                jooqClasspath = modelatorRuntime.map { entry -> entry.toURI().toURL() }
            }

            // substitute for @InputDirectories
            for (migrationPath in task.migrationsPaths) {
                task.inputs.dir(migrationPath)
            }
        }


    }

    private fun addJooqDependency(project: Project, modelatorRuntime: Configuration, config: JooqModelatorExtension) {
        val jooqVersion = config.jooqVersion
            ?: throw IncompletePluginConfigurationException("jOOQ version (jooqVersion)")
        project.dependencies.add(
            modelatorRuntime.name,
            "${jooqEditionToGroupId(config.jooqEdition)}:jooq-codegen:${jooqVersion}"
        )
        project.dependencies.add(
            modelatorRuntime.name,
            "${jooqEditionToGroupId(config.jooqEdition)}:jooq-meta:${jooqVersion}"
        )
        project.dependencies.add(
            modelatorRuntime.name,
            "${jooqEditionToGroupId(config.jooqEdition)}:jooq:${jooqVersion}"
        )
    }

    // source: https://github.com/etiennestuder/gradle-jooq-plugin
    private fun jooqEditionToGroupId(edition: String?) = when (edition) {
        "OSS" -> "org.jooq"
        "PRO" -> "org.jooq.pro"
        "PRO_JAVA_6" -> "org.jooq.pro-java-6"
        "TRIAL" -> "org.jooq.trial"
        else -> throw IllegalArgumentException("Wrong jooqModelator plugin configuration: jOOQ Edition incorrect. Must be one of ['OSS, 'PRO', 'PRO_JAVA_6', 'TRIAL']")

    }

    class IncompletePluginConfigurationException(missing: String) : IllegalArgumentException(
        "Incomplete jooqModelator plugin configuration: $missing is missing"
    )

}