package ch.ayedo.jooqmodelator.core.configuration

import java.nio.charset.Charset
import java.nio.file.Path
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamReader

data class DatabaseConfig(val driver: String, val url: String, val user: String, val password: String) {

    companion object {

        fun fromJooqConfig(jooqConfigPath: Path): DatabaseConfig {

            val factory = XMLInputFactory.newInstance()

            val reader = jooqConfigPath.toFile().reader(Charset.forName("UTF-8"))

            val xmlStreamReader = factory.createXMLStreamReader(reader)

            return try {

                skipToTag(xmlStreamReader, "jdbc")

                skipToTag(xmlStreamReader, "driver")
                val driver = xmlStreamReader.elementText

                skipToTag(xmlStreamReader, "url")
                val url = xmlStreamReader.elementText

                skipToTag(xmlStreamReader, "user")
                val user = xmlStreamReader.elementText

                skipToTag(xmlStreamReader, "password")
                val password = xmlStreamReader.elementText

                DatabaseConfig(driver, url, user, password)

            } catch (e: Exception) {
                throw IllegalStateException("Could not parse database configuration from jooq configuration file.\n" +
                    "[XMLStreamReader=${xmlStreamReader::class.java} jooqConfigPath=${jooqConfigPath.toAbsolutePath()}\n" +
                    "content=${jooqConfigPath.toFile().readText()}]", e)
            } finally {
                xmlStreamReader.close()
            }
        }

        private fun skipToTag(xmlStreamReader: XMLStreamReader, tag: String) {
            xmlStreamReader.nextTag()

            while (xmlStreamReader.localName != tag) {
                xmlStreamReader.nextTag()
            }
        }
    }
}
