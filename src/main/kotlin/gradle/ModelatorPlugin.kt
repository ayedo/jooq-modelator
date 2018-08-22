package gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

open class ModelatorPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        project.extensions.create("modelator", ModelatorExtension::class.java)

        project.afterEvaluate({

            val config = project.extensions.findByType(ModelatorExtension::class.java)!!

            project.tasks.create("generateMetamodel", ModelatorTask::class.java).apply {
                jooqConfigPath = config.jooqConfigPath
                migrationsPath = config.migrationsPath
                dockerLabelKey = config.labelKey
                dockerTag = config.dockerTag
                dockerEnv = config.dockerEnv
                dockerHostPort = config.dockerHostPort
                dockerContainerPort = config.dockerContainerPort
                migrationEngine = config.migrationEngine
                delayMs = config.delayMs
                maxDurationMs = config.maxDurationMs
                sql = config.sql
            }
        })
    }

}