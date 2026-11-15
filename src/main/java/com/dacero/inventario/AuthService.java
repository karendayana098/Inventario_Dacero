package com.dacero.inventario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class AuthService {
    private AuthService() {
    }

    public static boolean hasUsers() throws SQLException {
        String sql = "SELECT EXISTS(SELECT 1 FROM usuario_app WHERE activo = TRUE)";
        try (Connection cn = Db.connect();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getBoolean(1);
        }
    }

    public static void createFirstAdmin(String name, String username, char[] password)
            throws SQLException {
        if (name == null || name.isBlank() || username == null || username.isBlank()) {
            throw new IllegalArgumentException("Nombre y usuario son obligatorios.");
        }
        if (password == null || password.length < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        }

        try (Connection cn = Db.connect()) {
            cn.setAutoCommit(false);
            try {
                try (PreparedStatement check = cn.prepareStatement(
                        "SELECT COUNT(*) FROM usuario_app");
                     ResultSet rs = check.executeQuery()) {
                    rs.next();
                    if (rs.getLong(1) > 0) {
                        throw new IllegalStateException(
                                "Ya existe un usuario. El administrador inicial solo se crea una vez.");
                    }
                }
                String sql = "INSERT INTO usuario_app (nombre, usuario, clave_hash, rol, activo) "
                        + "VALUES (?, ?, ?, 'ADMIN', TRUE)";
                try (PreparedStatement ps = cn.prepareStatement(sql)) {
                    ps.setString(1, name.trim());
                    ps.setString(2, username.trim());
                    ps.setString(3, PasswordHasher.hash(password));
                    ps.executeUpdate();
                }
                cn.commit();
            } catch (SQLException | RuntimeException ex) {
                cn.rollback();
                throw ex;
            } finally {
                cn.setAutoCommit(true);
            }
        }
    }

    public static AppUser authenticate(String username, char[] password) throws SQLException {
        String sql = "SELECT usuario_id, nombre, usuario, clave_hash, rol "
                + "FROM usuario_app WHERE usuario = ? AND activo = TRUE";
        try (Connection cn = Db.connect();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, username == null ? "" : username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String stored = rs.getString("clave_hash");
                boolean valid = PasswordHasher.verify(password, stored);
                if (!valid && stored != null && !stored.startsWith("PBKDF2$")) {
                    valid = java.security.MessageDigest.isEqual(
                            new String(password).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                            stored.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    if (valid) {
                        try (PreparedStatement update = cn.prepareStatement(
                                "UPDATE usuario_app SET clave_hash=? WHERE usuario_id=?")) {
                            update.setString(1, PasswordHasher.hash(password));
                            update.setLong(2, rs.getLong("usuario_id"));
                            update.executeUpdate();
                        }
                    }
                }
                if (!valid) {
                    return null;
                }
                return new AppUser(
                        rs.getLong("usuario_id"),
                        rs.getString("nombre"),
                        rs.getString("usuario"),
                        rs.getString("rol")
                );
            }
        }
    }
}
