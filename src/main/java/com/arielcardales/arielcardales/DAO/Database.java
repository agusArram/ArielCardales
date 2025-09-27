package com.arielcardales.arielcardales.DAO;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public final class Database {
    private static final HikariDataSource DS;

    static {
        HikariConfig cfg = new HikariConfig();

        String url = getEnv("PG_URL",
                "jdbc:postgresql://aws-1-us-east-2.pooler.supabase.com:5432/postgres" +
                        "?sslmode=require&preferQueryMode=simple&reWriteBatchedInserts=true");
        String user = getEnv("PG_USER", "postgres.gybuxvjuhqhjmjmwkwyb");
        String pass = getEnv("PG_PASSWORD", "r$t13XR$^*R!U!@w");


        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(pass);

        // Pool & rendimiento
        int maxPool = Integer.parseInt(getEnv("PG_POOL_SIZE", "5"));
        cfg.setMaximumPoolSize(maxPool);
        cfg.setMinimumIdle(Math.min(1, maxPool));
        cfg.setAutoCommit(true); // Lecturas no transaccionales; las transacciones se manejan en servicios

        // Timeouts razonables
        cfg.setConnectionTimeout(10_000);
        cfg.setIdleTimeout(60_000);
        cfg.setMaxLifetime(30 * 60_000);

        // Si usás esquemas, podrías: cfg.setConnectionInitSql("set search_path to public");

        DS = new HikariDataSource(cfg);
    }

    private static String getEnv(String k, String def) {
        String v = System.getenv(k);
        return (v == null || v.isBlank()) ? def : v;
    }

    private Database() {}

    public static Connection get() throws SQLException {
        return DS.getConnection();
    }
}
