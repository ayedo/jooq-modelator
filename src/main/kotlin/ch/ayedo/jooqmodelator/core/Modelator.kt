package ch.ayedo.jooqmodelator.core

import ch.ayedo.jooqmodelator.core.configuration.Configuration
import ch.ayedo.jooqmodelator.core.configuration.DatabaseConfig
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory


@Suppress("SpellCheckingInspection")
class Modelator(configuration: Configuration) {

    private val log = LoggerFactory.getLogger(Modelator::class.java)

    private val dockerConfig = configuration.dockerConfig

    private val healthCheckConfig = configuration.healthCheckConfig

    private val jooqConfigPath = configuration.jooqConfigPath

    private val databaseConfig = DatabaseConfig.fromJooqConfig(jooqConfigPath)

    private val migrationConfig = configuration.migrationConfig

    private val jooqEntitiesPath = configuration.jooqEntitiesPath

    fun generate() {
        val dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        val httpClient = ApacheDockerHttpClient.Builder()
            .dockerHost(dockerClientConfig.dockerHost)
            .sslConfig(dockerClientConfig.sslConfig)
            .build()
        val dockerClient = DockerClientImpl.getInstance(dockerClientConfig, httpClient)

        dockerClient.use { docker ->
            val tag = dockerConfig.tag
            docker.checkAndPullImage(tag)
            val existingContainers =
                docker.findLabeledContainers(key = dockerConfig.labelKey, value = dockerConfig.labelValue)

            val containerId =
                if (existingContainers.isEmpty()) {
                    dockerClient.createContainerCmd(dockerConfig.tag)
                        .withHostConfig(dockerConfig.createHostConfig())
                        .withEnv(dockerConfig.env)
                        .withExposedPorts(ExposedPort(dockerConfig.portMapping.container))
                        .withLabels(mapOf(dockerConfig.labelKey to dockerConfig.labelValue))
                        .exec().id
                } else {
                    if (existingContainers.size > 1) {
                        log.warn(
                            "More than one container with tag ${dockerConfig.labelKey}=$tag has been found. " +
                                    "Using the one which was most recently created"
                        )
                    }
                    existingContainers.sortedBy { it.created }.map { it.id }.first()
                }

            docker.useContainer(containerId) {
                waitForDatabase()
                migrateDatabase()
                runJooqGenerator()
            }
        }
    }

    private fun waitForDatabase() {
        val healthChecker = HealthChecker.getDefault(databaseConfig, healthCheckConfig)

        healthChecker.waitForDatabase()
    }

    private fun migrateDatabase() {
        val migrator = Migrator.fromConfig(migrationConfig, databaseConfig)

        with(migrator) {
            clean()
            migrate()
        }
    }

    private fun runJooqGenerator() {
        var jooqConfig = modifyJooqConfig(jooqConfigPath.toFile().readText())

        val generationTool =
            Class.forName("org.jooq.codegen.GenerationTool", true, Thread.currentThread().contextClassLoader)

        val generateMethod = generationTool.getDeclaredMethod("generate", String::class.java)

        generateMethod.invoke(generationTool, jooqConfig)
    }

    private fun modifyJooqConfig(jooqConfig: String): String {
        val doc: Document = DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .parse(InputSource(StringReader(jooqConfig)))
        val xPath: XPath = XPathFactory.newInstance().newXPath()
        val targetDirectoryExpression = "/configuration/generator/target/directory"
        val nodeList = xPath.compile(targetDirectoryExpression).evaluate(doc, XPathConstants.NODESET) as NodeList
        for (i in 0 until nodeList.getLength()) {
            nodeList.item(i).textContent = jooqEntitiesPath;
        }
        val transformer: Transformer = TransformerFactory.newInstance().newTransformer()
        val writer = StringWriter()
        transformer.transform(DOMSource(doc), StreamResult(writer));
        return writer.getBuffer().toString()
    }

}