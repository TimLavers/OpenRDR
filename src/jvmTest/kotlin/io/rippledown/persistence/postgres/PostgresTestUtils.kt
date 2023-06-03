package io.rippledown.persistence.postgres

fun refreshDatabase(dbName: String) {
    dropDB(dbName)
    ConnectionProvider.systemConnection().use {
        it.createStatement().executeUpdate("CREATE DATABASE $dbName")
    }
}

fun dropDB(dbName: String) {
    ConnectionProvider.systemConnection().use {
        it.createStatement().executeUpdate("DROP DATABASE IF EXISTS $dbName")
    }
}