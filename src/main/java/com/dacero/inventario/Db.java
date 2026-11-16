package com.dacero.inventario;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class Db {
    private Db() {
    }

    public static Connection connect() throws SQLException {
        Properties local = loadLocalSettings();
        String url = setting(local, "DACERO_DB_URL", "db.url",
                "jdbc:mysql://localhost:3306/dacero?useSSL=false&serverTimezone=America/Bogota&allowPublicKeyRetrieval=true");
        String user = setting(local, "DACERO_DB_USER", "db.user", "root");
        String pass = setting(local, "DACERO_DB_PASSWORD", "db.password", "");

        return DriverManager.getConnection(url, user, pass);
    }

    private static Properties loadLocalSettings() throws SQLException {
        Properties settings = new Properties();
        Path file = Path.of("db.local.properties");
        if (!Files.isRegularFile(file)) return settings;

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            settings.load(reader);
            return settings;
        } catch (IOException ex) {
            throw new SQLException("No se pudo leer db.local.properties.", ex);
        }
    }

    private static String setting(Properties local, String environmentKey,
            String propertyKey, String defaultValue) {
        String environmentValue = System.getenv(environmentKey);
        if (environmentValue != null && !environmentValue.isBlank()) return environmentValue;
        return local.getProperty(propertyKey, defaultValue);
    }

    /** Verifica que MySQL acepte la conexión configurada. */
    public static void verifyConnection() throws SQLException {
        try (Connection connection = connect()) {
            if (!connection.isValid(5)) {
                throw new SQLException("MySQL no respondió a la validación de conexión.");
            }
        }
    }
}
