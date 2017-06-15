package ch.ayedo.modelator.core.configuration

import java.nio.file.Path
import javax.xml.bind.JAXBContext
import javax.xml.stream.XMLInputFactory
import javax.xml.transform.stream.StreamSource

/** The database configuration is loaded from the jooq configuration file */
@javax.xml.bind.annotation.XmlAccessorType(javax.xml.bind.annotation.XmlAccessType.FIELD)
class DatabaseConfig {

    var driver: String? = null
    lateinit var url: String
    lateinit var user: String
    lateinit var password: String

    companion object {

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
