package ch.ayedo.modelator

import net.jodah.failsafe.Failsafe
import net.jodah.failsafe.RetryPolicy
import org.flywaydb.core.internal.util.jdbc.DriverDataSource
import org.flywaydb.core.internal.util.jdbc.JdbcUtils.openConnection
import java.util.concurrent.TimeUnit.MILLISECONDS

interface HealthChecker {

    /* blocks until the database processes queries */
    fun waitForDatabase()

    companion object {
        fun getDefault(databaseConfig: DatabaseConfig, healthCheckConfig: HealthCheckConfig): HealthChecker {
            return FlywayDependentHealthChecker(databaseConfig, healthCheckConfig)
        }
    }
}

/* uses Flyway's exposed JDBC tooling to connect to the database */
class FlywayDependentHealthChecker(databaseConfig: DatabaseConfig, healthCheckConfig: HealthCheckConfig) : HealthChecker {

    private val sql = healthCheckConfig.sql

    private val driverDataSource = DriverDataSource(
            this.javaClass.classLoader,
            databaseConfig.driver,
            databaseConfig.url,
            databaseConfig.user,
            databaseConfig.password,
            null,
            null)

    private val retryPolicy = RetryPolicy().apply {
        withDelay(healthCheckConfig.delayMs, MILLISECONDS)
        withMaxDuration(healthCheckConfig.maxDurationMs, MILLISECONDS)
    }

    override fun waitForDatabase() {
        Failsafe.with<RetryPolicy>(retryPolicy).run { ->

            openConnection(driverDataSource).use {
                it.createStatement().execute(sql)
            }
        }
    }
}

