package com.dacero.inventario;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Db {
    private Db() {
    }

    public static Connection connect() throws SQLException {
        String url = System.getenv().getOrDefault(
                "DACERO_DB_URL",
                "jdbc:mysql://localhost:3306/dacero?useSSL=false&serverTimezone=America/Bogota&allowPublicKeyRetrieval=true"
        );
        String user = System.getenv().getOrDefault("DACERO_DB_USER", "root");
        String pass = System.getenv().getOrDefault("DACERO_DB_PASSWORD", "");

        return DriverManager.getConnection(url, user, pass);
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
