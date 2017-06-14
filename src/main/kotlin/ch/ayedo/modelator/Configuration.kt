package ch.ayedo.modelator

import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.HostConfig
import com.spotify.docker.client.messages.PortBinding
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.bind.JAXBContext
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.stream.XMLInputFactory
import javax.xml.transform.stream.StreamSource


data class Configuration(val dockerConfig: DockerConfig,
                         val migrationsPath: Path,
                         val jooqConfigPath: Path)

data class DockerConfig(val tag: String,
                        val labelKey: String = "ch.ayedo.JooqMetamodelTask.tag",
                        val env: List<String>,
                        val portMapping: PortMapping) {

    fun toContainerConfig(): ContainerConfig? {
        val hostConfig = createHostConfig(portMapping)

        return ContainerConfig.builder()
                .hostConfig(hostConfig)
                .image(tag)
                .env(env)
                .exposedPorts(portMapping.container.toString())
                .labels(mapOf(labelKey to tag))
                .build()
    }

    private fun createHostConfig(portMapping: PortMapping): HostConfig {

        val defaultPortBinding: PortBinding = PortBinding.of("0.0.0.0", portMapping.container)
        val portBindings = mapOf(portMapping.host.toString() to listOf(defaultPortBinding))

        return HostConfig.builder()
                .portBindings(portBindings)
                .build()
    }
}

data class PortMapping(val host: Int, val container: Int)

@XmlAccessorType(XmlAccessType.FIELD)
class DatabaseConfig {

    lateinit var driver: String
    lateinit var url: String
    lateinit var user: String
    lateinit var password: String

    companion object {

        fun fromJooqConfig(jooqConfigUri: URI) = fromJooqConfig(Paths.get(jooqConfigUri))

        fun fromJooqConfig(jooqConfigPath: Path): DatabaseConfig {

            val xmlStreamReader = XMLInputFactory.newFactory().let { factory ->
                val source = StreamSource(jooqConfigPath.toFile())
                factory.createXMLStreamReader(source)
            }

            try {
                xmlStreamReader.nextTag()

                while (xmlStreamReader.localName != "jdbc") {
                    xmlStreamReader.nextTag()
                }

                val unmarshaller = JAXBContext.newInstance(DatabaseConfig::class.java).createUnmarshaller()
                val jb = unmarshaller.unmarshal(xmlStreamReader, DatabaseConfig::class.java)

                return jb.value

            } finally {
                xmlStreamReader.close()
            }
        }
    }
}
