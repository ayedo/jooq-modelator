package ch.ayedo.modelator

import org.testng.annotations.Test

class ConfigurationParserTest {

    @Test
    fun `test the parsing of the jOOQ JDBC configuration from the jOOQ configuration file`() {

        val configuration = this.javaClass.getResource("/jooqConfiguration.xml").toURI()

        val fromJooqConfig = DatabaseConfig.fromJooqConfig(configuration)

        listOf(fromJooqConfig::user, fromJooqConfig::url, fromJooqConfig::password, fromJooqConfig::driver).forEach {
            assert(it.get() == it.name)
        }

    }


}