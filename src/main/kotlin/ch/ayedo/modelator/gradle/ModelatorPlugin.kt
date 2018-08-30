package ch.ayedo.modelator.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.nio.file.Paths

open class ModelatorPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        project.extensions.create("modelator", ModelatorExtension::class.java)

        val modelatorRuntime = project.configurations.create("modelatorRuntime")

        modelatorRuntime.description = "Add JDBC drivers or generator extensions here."

        project.dependencies.add(modelatorRuntime.name, "org.jooq:jooq-codegen:3.11.4")

        project.afterEvaluate({

            val config = project.extensions.findByType(ModelatorExtension::class.java)!!

            // TODO: description?
            project.tasks.create("generateMetamodel", ModelatorTask::class.java).apply {
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

}