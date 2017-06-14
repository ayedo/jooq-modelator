package ch.ayedo.modelator

import ch.ayedo.modelator.configuration.DatabaseConfig
import ch.ayedo.modelator.configuration.MigrationConfig
import ch.ayedo.modelator.configuration.MigrationEngine.FLYWAY
import ch.ayedo.modelator.configuration.MigrationEngine.LIQUIBASE
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

    companion object {
        fun fromConfig(migrationConfig: MigrationConfig, databaseConfig: DatabaseConfig) =
            when (migrationConfig.engine) {
                FLYWAY -> FlywayMigrator(databaseConfig, migrationConfig.migrationsPath)
                LIQUIBASE -> LiquibaseMigrator(databaseConfig, migrationConfig.migrationsPath)
            }
    }
}

class FlywayMigrator(databaseConfig: DatabaseConfig, migrationsPath: Path) : Migrator {

    private val flyway = Flyway().apply {
        with(databaseConfig) {
            setDataSource(url, user, password)
        }
        setLocations("filesystem:$migrationsPath")
    }

    override fun clean() {
        flyway.clean()
    }

    override fun migrate() {
        flyway.migrate()
    }

}

class LiquibaseMigrator(databaseConfig: DatabaseConfig, changelogPath: Path) : Migrator {

    private val liquibase: Liquibase

    init {
        val database = with(databaseConfig) {
            DatabaseFactory.getInstance().openDatabase(url, user, password, null, ResourceSupplier().simpleResourceAccessor)
        }
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