package io.rippledown.persistence.postgres

fun refreshDatabase(dbName: String) {
    ConnectionProvider.systemConnection().use {
        it.createStatement().executeUpdate("DROP DATABASE IF EXISTS $dbName")
    }
    ConnectionProvider.systemConnection().use {
        it.createStatement().executeUpdate("CREATE DATABASE $dbName")
    }
}