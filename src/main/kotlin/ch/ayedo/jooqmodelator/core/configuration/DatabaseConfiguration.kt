package ch.ayedo.jooqmodelator.core.configuration

import java.nio.charset.Charset
import java.nio.file.Path
import javax.xml.bind.JAXBContext
import javax.xml.stream.XMLInputFactory

/** The database configuration is loaded from the jooq configuration file */
@javax.xml.bind.annotation.XmlAccessorType(javax.xml.bind.annotation.XmlAccessType.FIELD)
class DatabaseConfig {

    var driver: String? = null
    var url: String? = null
    var user: String? = null
    var password: String? = null

    companion object {

        fun fromJooqConfig(jooqConfigPath: Path): DatabaseConfig {

            val xmlStreamReader = XMLInputFactory.newFactory().let { factory ->
                val reader = jooqConfigPath.toFile().reader(Charset.forName("UTF-8"))
                factory.createXMLStreamReader(reader)
            }

            try {
                xmlStreamReader.nextTag()

                while (xmlStreamReader.localName != "jdbc") {
                    xmlStreamReader.nextTag()
                }

                val unmarshaller = JAXBContext.newInstance(DatabaseConfig::class.java).createUnmarshaller()
                val jb = unmarshaller.unmarshal(xmlStreamReader, DatabaseConfig::class.java)

                val config = jb.value

                if (config.url == null && config.user == null && config.password == null) {
                    throw IllegalStateException("Could not parse database configuration from jooq configuration file." +
                        "[unmarshaller=${unmarshaller::class.java} jooqConfigPath=${jooqConfigPath.toAbsolutePath()} content=${jooqConfigPath.toFile().readText()}]")
                }

                return config

            } finally {
                xmlStreamReader.close()
            }
        }
    }
}
