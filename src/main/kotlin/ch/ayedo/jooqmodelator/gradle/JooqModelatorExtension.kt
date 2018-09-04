package ch.ayedo.jooqmodelator.gradle


open class JooqModelatorExtension {

    var jooqVersion: String? = null

    var jooqEdition: String? = null

    var jooqConfigPath: String? = null

    var migrationsPath: String? = null

    var dockerTag: String = "postgres:9.5"

    var dockerEnv: List<String> = emptyList()

    var dockerHostPort: Int = 5432

    var dockerContainerPort: Int = 5432

    var migrationEngine: String = "FLYWAY"

    var delayMs: Long = 500

    var maxDurationMs: Long = 20000

    var sql: String = "SELECT 1"

    // TODO: rename to dockerLabelKey?
    var labelKey: String = "ch.ayedo.jooqmodelator.tag"
}
