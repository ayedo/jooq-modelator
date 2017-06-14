import com.beust.kobalt.plugin.application.application
import com.beust.kobalt.plugin.packaging.assemble
import com.beust.kobalt.project

val modelator = project {
    name = "modelator"
    group = "ch.ayedo"
    artifactId = name
    version = gitDescribe()

    dependencies {
        compile("org.jetbrains.kotlin:kotlin-stdlib-jre7:1.1.2-4")
        compile("net.jodah:failsafe:0.9.1")
        compile("com.spotify:docker-client:8.1.2")
        compile("org.flywaydb:flyway-core:4.1.2")
        compile("org.liquibase:liquibase-core:jar:3.5.3")
        compile("org.jooq:jooq-codegen:3.9.1")
        compile("org.postgresql:postgresql:42.0.0")
        compile("org.mariadb.jdbc:mariadb-java-client:jar:2.0.2")
        compile("org.slf4j:slf4j-simple:1.7.23")
    }

    dependenciesTest {
        compile("org.testng:testng:6.11")
        compile("org.postgresql:postgresql:42.0.0")
        compile("org.mariadb.jdbc:mariadb-java-client:jar:2.0.2")
    }

    assemble {
        jar {
            fatJar = true
        }
    }

    application {
        mainClass = "com.ayedo.MainKt"
    }
}
