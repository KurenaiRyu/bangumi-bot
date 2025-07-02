package moe.kurenai.bot.repository

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.babyfish.jimmer.sql.dialect.SQLiteDialect
import org.babyfish.jimmer.sql.event.TriggerType
import org.babyfish.jimmer.sql.kt.newKSqlClient
import org.babyfish.jimmer.sql.runtime.ConnectionManager

object DBHolder {
    val sqlClient = lazy {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite:bangumi-bot.db"
            driverClassName = "org.sqlite.JDBC"
            isAutoCommit = true
            maximumPoolSize = 1
        }
        newKSqlClient {
            setTriggerType(TriggerType.BINLOG_ONLY)
            setDialect(SQLiteDialect())
            setConnectionManager(ConnectionManager.simpleConnectionManager(HikariDataSource(config)))
        }
    }
}
