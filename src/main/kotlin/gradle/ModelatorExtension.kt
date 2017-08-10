package gradle

import java.nio.file.Path

open class ModelatorExtension(

    var jooqConfigPath: Path,

    var migrationsPath: Path,

    var dockerTag: String,

    var dockerEnv: List<String> = emptyList(),

    var dockerHostPort: Int = 5432,

    var dockerContainerPort: Int = 5432,

    var migrationEngine: String = "FLYWAY",

    var delayMs: Long = 500,

    var maxDurationMs: Long = 20000,

    var sql: String = "SELECT 1",

    var labelKey: String = "ch.ayedo.modelator.tag"
)