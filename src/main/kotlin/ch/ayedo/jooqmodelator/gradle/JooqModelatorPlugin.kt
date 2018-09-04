package ch.ayedo.jooqmodelator.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import java.nio.file.Paths

open class JooqModelatorPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        project.extensions.create("jooqModelator", JooqModelatorExtension::class.java)

        val modelatorRuntime = project.configurations.create("jooqModelatorRuntime")

        modelatorRuntime.description = "Add JDBC drivers or generator extensions here."

        project.afterEvaluate({

            val config = project.extensions.findByType(JooqModelatorExtension::class.java)!!

            addJooqDependency(project, modelatorRuntime, config)

            // TODO: description?
            project.tasks.create("generateJooqMetamodel", JooqModelatorTask::class.java).apply {
                jooqConfigPath = Paths.get(config.jooqConfigPath
                    ?: throw IllegalArgumentException("Incomplete plugin configuration: path to the jOOQ generator configuration (jooqConfigPath) is missing "))
                migrationsPath = Paths.get(config.migrationsPath
                    ?: throw IllegalArgumentException("Incomplete plugin configuration: path to the migration files (migrationsPath) is missing"))
                dockerLabelKey = config.labelKey
                dockerTag = config.dockerTag
                dockerEnv = config.dockerEnv
                dockerHostPort = config.dockerHostPort
                dockerContainerPort = config.dockerContainerPort
                migrationEngine = config.migrationEngine
                delayMs = config.delayMs
                maxDurationMs = config.maxDurationMs
                sql = config.sql
                jooqClasspath = modelatorRuntime.map { entry -> entry.toURI().toURL() }
            }
        })
    }

    private fun addJooqDependency(project: Project, modelatorRuntime: Configuration, config: JooqModelatorExtension) {
        val jooqVersion = config.jooqVersion
            ?: throw IllegalArgumentException("Incomplete plugin configuration: jOOQ version is missing")

        project.dependencies.add(modelatorRuntime.name, "${jooqEditionToGroupId(config.jooqEdition)}:jooq-codegen:${jooqVersion}")
    }

    // source: https://github.com/etiennestuder/gradle-jooq-plugin
    private fun jooqEditionToGroupId(edition: String?) = when (edition) {
        "OSS" -> "org.jooq"
        "PRO" -> "org.jooq.pro"
        "PRO_JAVA_6" -> "org.jooq.pro-java-6"
        "TRIAL" -> "org.jooq.trial"
        else -> throw IllegalArgumentException("jOOQ Edition incorrect. Must be one of ['OSS, 'PRO', 'PRO_JAVA_6', 'TRIAL']")

    }


}