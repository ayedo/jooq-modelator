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

    private val driverDataSource = with(databaseConfig) {
        DriverDataSource(this.javaClass.classLoader, driver, url, user, password, null, null)
    }

    private val retryPolicy = RetryPolicy().apply {
        val (delayMs, maxDurationMs) = healthCheckConfig

        withDelay(delayMs, MILLISECONDS)
        withMaxDuration(maxDurationMs, MILLISECONDS)
    }

    override fun waitForDatabase() {
        Failsafe.with<RetryPolicy>(retryPolicy).run { ->

            openConnection(driverDataSource).use {
                it.createStatement().execute(sql)
            }
        }
    }
}

