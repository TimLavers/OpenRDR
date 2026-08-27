package io.rippledown.persistence.postgres

fun dropDB(dbName: String) {
    ConnectionProvider.systemConnection().use {
        it.createStatement().executeUpdate("DROP DATABASE IF EXISTS $dbName")
    }
}

/**
 * Empty every table of the given database, so that a test can start from an
 * empty one without the cost of recreating the database. Identities are
 * restarted so that a test can rely on the ids that a store generates. The KB
 * info table is left alone, as it holds the single row identifying the KB,
 * which PostgresKB requires.
 */
fun clearTablesOf(dbName: String) {
    ConnectionProvider.connection(dbName).use { connection ->
        val tables = mutableListOf<String>()
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT tablename FROM pg_tables WHERE schemaname = 'public'").use { rows ->
                while (rows.next()) {
                    val table = rows.getString(1)
                    if (table != KB_INFO_TABLE) tables.add("\"$table\"")
                }
            }
        }
        if (tables.isEmpty()) return@use
        connection.createStatement().use {
            it.executeUpdate("TRUNCATE TABLE ${tables.joinToString(", ")} RESTART IDENTITY CASCADE")
        }
    }
}