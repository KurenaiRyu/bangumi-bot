package moe.kurenai.bot.repository

object DBHolder {
    val sqlClient = {
        HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite:im-sync-bot.db"
            driverClassName = "org.sqlite.JDBC"
        }
    }
}
