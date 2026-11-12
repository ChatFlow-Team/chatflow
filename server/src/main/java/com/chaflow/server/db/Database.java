package com.chaflow.server.db;

import org.flywaydb.core.Flyway;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    private static final String URL = env("DB_URL", "jdbc:postgresql://postgres:5432/chatflow_db");
    private static final String USER = env("DB_USER", "postgres");
    private static final String PASSWORD = env("DB_PASSWORD", "chatflow_dev_password");

    private Database() {}

    public static void migrate() {
        Flyway.configure()
                .dataSource(URL, USER, PASSWORD)
                .schemas("chatflow")
                .defaultSchema("chatflow")
                .locations("classpath:db/migration")
                // Existing development databases are baselined at version 0 so V1 still runs.
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load()
                .migrate();
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
