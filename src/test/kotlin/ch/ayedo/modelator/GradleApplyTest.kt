package ch.ayedo.modelator

import ch.ayedo.modelator.gradle.ModelatorTask
import org.gradle.testfixtures.ProjectBuilder
import org.testng.Assert.assertTrue
import org.testng.annotations.Test

class GradleApplyTest {

    @Test
    fun addPluginToProject() {

        val project = ProjectBuilder.builder().build()

        project.pluginManager.apply("ch.ayedo.modelator")

        val taskLookup = project.task(hashMapOf("type" to ModelatorTask::class.java), "generateMetamodel")

        assertTrue(taskLookup is ModelatorTask)

    }
}