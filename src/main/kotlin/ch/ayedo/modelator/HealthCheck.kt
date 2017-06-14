package ch.ayedo.modelator

import net.jodah.failsafe.Failsafe
import net.jodah.failsafe.RetryPolicy
import org.flywaydb.core.internal.util.jdbc.DriverDataSource
import org.flywaydb.core.internal.util.jdbc.JdbcUtils.openConnection
import java.util.concurrent.TimeUnit

interface HealthChecker {

    /* blocks until the database processes queries */
    fun waitForDatabase()

    companion object {
        fun getDefault(databaseConfig: DatabaseConfig) = FlywayDependentHealthChecker(databaseConfig)
    }
}

/* uses Flyway's exposed JDBC tooling to connect to the database */
class FlywayDependentHealthChecker(databaseConfig: DatabaseConfig) : HealthChecker {

    private val driverDataSource = DriverDataSource(
            this.javaClass.classLoader,
            databaseConfig.driver,
            databaseConfig.url,
            databaseConfig.user,
            databaseConfig.password,
            null,
            null)

    private val retryPolicy = RetryPolicy()
            .withDelay(500, TimeUnit.MILLISECONDS)
            .withMaxDuration(20, TimeUnit.SECONDS)

    override fun waitForDatabase() {
        Failsafe.with<RetryPolicy>(retryPolicy).run { ->

            openConnection(driverDataSource).use {
                // TODO: we could use a case statement, to be more general
                it.createStatement().execute("SELECT 1")
            }
        }
    }
}

