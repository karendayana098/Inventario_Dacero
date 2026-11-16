package com.dacero.inventario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class Autenticacion {
    public record UserEntry(long id, String name, String username, String role, boolean active) {
    }

    private Autenticacion() {
    }

    public static boolean hasUsers() throws SQLException {
        String sql = "SELECT EXISTS(SELECT 1 FROM usuario_app)";
        try (Connection cn = Db.connect();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getBoolean(1);
        }
    }

    public static List<UserEntry> users() throws SQLException {
        List<UserEntry> users = new ArrayList<>();
        String sql = "SELECT usuario_id, nombre, usuario, rol, activo "
                + "FROM usuario_app ORDER BY nombre, usuario";
        try (Connection cn = Db.connect();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(new UserEntry(rs.getLong("usuario_id"), rs.getString("nombre"),
                        rs.getString("usuario"), rs.getString("rol"), rs.getBoolean("activo")));
            }
        }
        return users;
    }

    public static void createUser(String name, String username, String role, char[] password)
            throws SQLException {
        validateUser(name, username, role);
        validatePassword(password);
        String sql = "INSERT INTO usuario_app (nombre, usuario, clave_hash, rol, activo) "
                + "VALUES (?, ?, ?, ?, TRUE)";
        try (Connection cn = Db.connect(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ps.setString(2, username.trim());
            ps.setString(3, PasswordHasher.hash(password));
            ps.setString(4, role);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw explainUserConstraint(ex);
        }
    }

    public static void updateUser(long id, String name, String username, String role,
            boolean active, char[] newPassword) throws SQLException {
        validateUser(name, username, role);
        if (newPassword != null && newPassword.length > 0) validatePassword(newPassword);

        try (Connection cn = Db.connect()) {
            cn.setAutoCommit(false);
            try {
                if (!("ADMINISTRADOR".equals(role) || "ADMIN".equals(role)) || !active) {
                    try (PreparedStatement count = cn.prepareStatement(
                            "SELECT COUNT(*) FROM usuario_app "
                                    + "WHERE rol IN ('ADMINISTRADOR','ADMIN') AND activo=TRUE AND usuario_id<>?")) {
                        count.setLong(1, id);
                        try (ResultSet rs = count.executeQuery()) {
                            if (rs.next() && rs.getLong(1) == 0) {
                                throw new IllegalArgumentException(
                                        "Debe quedar al menos un administrador activo.");
                            }
                        }
                    }
                }

                String sql = newPassword != null && newPassword.length > 0
                        ? "UPDATE usuario_app SET nombre=?, usuario=?, rol=?, activo=?, clave_hash=? WHERE usuario_id=?"
                        : "UPDATE usuario_app SET nombre=?, usuario=?, rol=?, activo=? WHERE usuario_id=?";
                try (PreparedStatement ps = cn.prepareStatement(sql)) {
                    ps.setString(1, name.trim());
                    ps.setString(2, username.trim());
                    ps.setString(3, role);
                    ps.setBoolean(4, active);
                    if (newPassword != null && newPassword.length > 0) {
                        ps.setString(5, PasswordHasher.hash(newPassword));
                        ps.setLong(6, id);
                    } else {
                        ps.setLong(5, id);
                    }
                    if (ps.executeUpdate() == 0) {
                        throw new SQLException("No se encontró el usuario para actualizar.");
                    }
                }
                cn.commit();
            } catch (SQLException | RuntimeException ex) {
                cn.rollback();
                if (ex instanceof SQLException sqlEx) throw explainUserConstraint(sqlEx);
                throw ex;
            } finally {
                cn.setAutoCommit(true);
            }
        }
    }

    private static void validateUser(String name, String username, String role) {
        if (name == null || name.isBlank() || username == null || username.isBlank()) {
            throw new IllegalArgumentException("Nombre y usuario son obligatorios.");
        }
        if (!List.of("ADMINISTRADOR", "VENDEDOR", "BODEGUERO").contains(role)) {
            throw new IllegalArgumentException("Selecciona un rol válido.");
        }
    }

    private static void validatePassword(char[] password) {
        if (password == null || password.length < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        }
    }

    private static SQLException explainUserConstraint(SQLException ex) {
        if ("23000".equals(ex.getSQLState()) || "23505".equals(ex.getSQLState())) {
            return new SQLException("Ese nombre de usuario ya está registrado. Elige otro.", ex.getSQLState(), ex.getErrorCode(), ex);
        }
        return ex;
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
                        + "VALUES (?, ?, ?, 'ADMINISTRADOR', TRUE)";
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

    public static App authenticate(String username, char[] password) throws SQLException {
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
                return new App(
                        rs.getLong("usuario_id"),
                        rs.getString("nombre"),
                        rs.getString("usuario"),
                        rs.getString("rol")
                );
            }
        }
    }
}
