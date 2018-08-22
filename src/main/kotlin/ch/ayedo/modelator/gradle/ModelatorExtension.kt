package ch.ayedo.modelator.gradle

import java.nio.file.Path
import java.nio.file.Paths


open class ModelatorExtension {

    var jooqConfigPath: Path = Paths.get("")

    var migrationsPath: Path = Paths.get("")

    var dockerTag: String = "postgres:9.5"

    var dockerEnv: List<String> = emptyList()

    var dockerHostPort: Int = 5432

    var dockerContainerPort: Int = 5432

    var migrationEngine: String = "FLYWAY"

    var delayMs: Long = 500

    var maxDurationMs: Long = 20000

    var sql: String = "SELECT 1"

    var labelKey: String = "ch.ayedo.modelator.tag"
}
