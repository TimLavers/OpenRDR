package io.rippledown.persistence.postgres

fun dropDB(dbName: String) {
    ConnectionProvider.systemConnection().use {
        it.createStatement().executeUpdate("DROP DATABASE IF EXISTS $dbName")
    }
}