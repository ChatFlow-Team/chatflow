package com.chaflow.server.db;

import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/** One shared PostgreSQL DataSource for authentication, services and legacy DAOs. */
public final class Database {
    private static final String URL = env("DB_URL", "jdbc:postgresql://postgres:5432/chatflow_db");
    private static final String USER = env("DB_USER", "postgres");
    private static final String PASSWORD = env("DB_PASSWORD", "chatflow_dev_password");
    private static final PGSimpleDataSource DATA_SOURCE = createDataSource();

    private Database() {}

    private static PGSimpleDataSource createDataSource() {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(URL);
        ds.setUser(USER);
        ds.setPassword(PASSWORD);
        ds.setApplicationName("ChaFlow-Server");
        return ds;
    }

    public static DataSource getDataSource() { return DATA_SOURCE; }
    public static Connection getConnection() throws SQLException { return DATA_SOURCE.getConnection(); }

    public static void migrate() {
        Flyway.configure()
                .dataSource(DATA_SOURCE)
                .schemas("chatflow")
                .defaultSchema("chatflow")
                .locations("classpath:db/migration")
                // Existing development databases are baselined at version 0 so V1 still runs.
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load()
                .migrate();
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
