package ch.ayedo.modelator

import liquibase.Contexts
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.resource.FileSystemResourceAccessor
import liquibase.sdk.supplier.resource.ResourceSupplier
import org.flywaydb.core.Flyway
import java.nio.file.Path


interface Migrator {

    /* deletes all objects in the database */
    fun clean()

    /* applies all migrations to the database */
    fun migrate()
}

class FlywayMigrator(jdbcUrl: String, user: String, password: String, migrationsDirectory: Path) : Migrator {

    private val flyway = Flyway().apply {
        setDataSource(jdbcUrl, user, password)
        setLocations("filesystem:$migrationsDirectory")
    }

    override fun clean() {
        flyway.clean()
    }

    override fun migrate() {
        flyway.migrate()
    }

}

class LiquibaseMigrator(jdbcUrl: String, user: String, password: String, changelogPath: Path) : Migrator {

    private val liquibase: Liquibase

    init {
        val database = DatabaseFactory.getInstance().openDatabase(jdbcUrl, user, password, null, ResourceSupplier().simpleResourceAccessor)
        liquibase = Liquibase(changelogPath.toString(), FileSystemResourceAccessor(), database)
    }

    override fun clean() {
        liquibase.dropAll()
    }

    override fun migrate() {
        val nullContext: Contexts? = null
        liquibase.update(nullContext)
    }
}