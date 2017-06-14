package ch.ayedo.modelator.configuration

import java.nio.file.Path
import javax.xml.bind.JAXBContext
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.stream.XMLInputFactory
import javax.xml.transform.stream.StreamSource

/** The database configuration is loaded from the jooq configuration file */
@XmlAccessorType(XmlAccessType.FIELD)
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
