package ch.ayedo.jooqmodelator

import ch.ayedo.jooqmodelator.gradle.JooqModelatorTask
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GradleApplyTest {

    @Test
    fun addPluginToProject() {

        val project = ProjectBuilder.builder().build()

        project.pluginManager.apply("ch.ayedo.jooqmodelator")

        val taskLookup = project.task(hashMapOf("type" to JooqModelatorTask::class.java), "generateJooqMetamodel")

        assertTrue(taskLookup is JooqModelatorTask)

    }

}