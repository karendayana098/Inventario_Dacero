package com.dacero.inventario;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class InventoryRepository {
    public record Option(long id, String label) {
        @Override public String toString() { return label; }
    }

    public record Metrics(long products, long lowStock, long monthlyMovements) {
    }

    private InventoryRepository() {
    }

    public static Metrics metrics() throws SQLException {
        try (Connection cn = Db.connect()) {
            return new Metrics(
                    scalar(cn, "SELECT COUNT(*) FROM producto WHERE activo = TRUE"),
                    scalar(cn, "SELECT COUNT(*) FROM vw_stock_bajo"),
                    scalar(cn, "SELECT COUNT(*) FROM movimiento "
                            + "WHERE YEAR(fecha) = YEAR(CURRENT_DATE) "
                            + "AND MONTH(fecha) = MONTH(CURRENT_DATE)")
            );
        }
    }

    private static long scalar(Connection cn, String sql) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    public static List<Object[]> products() throws SQLException {
        String sql = "SELECT p.producto_id, p.codigo, p.nombre, "
                + "COALESCE(c.nombre, 'Sin categoría'), p.unidad_medida, "
                + "p.stock_minimo, p.activo, COALESCE(s.nombre, 'Sin sede activa'), "
                + "COALESCE(i.existencia, 0) "
                + "FROM producto p LEFT JOIN categoria c ON c.categoria_id=p.categoria_id "
                + "LEFT JOIN sede s ON s.activa=TRUE "
                + "LEFT JOIN inventario i ON i.producto_id=p.producto_id AND i.sede_id=s.sede_id "
                + "ORDER BY p.nombre, s.nombre";
        return rows(sql);
    }

    public static List<Object[]> movements() throws SQLException {
        return rows("SELECT movimiento_id, fecha, tipo, sede, codigo, producto, "
                + "cantidad, usuario, referencia FROM vw_movimientos "
                + "ORDER BY fecha DESC, movimiento_id DESC");
    }

    public static List<Object[]> lowStock() throws SQLException {
        return rows("SELECT sede, codigo, producto, existencia, stock_minimo "
                + "FROM vw_stock_bajo ORDER BY sede, producto");
    }

    public static List<Object[]> inventory() throws SQLException {
        return rows("SELECT sede, codigo, producto, categoria, unidad_medida, "
                + "existencia, stock_minimo, requiere_reposicion "
                + "FROM vw_existencias ORDER BY sede, producto");
    }

    private static List<Object[]> rows(String sql) throws SQLException {
        List<Object[]> result = new ArrayList<>();
        try (Connection cn = Db.connect();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int columns = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                Object[] row = new Object[columns];
                for (int i = 0; i < columns; i++) {
                    row[i] = rs.getObject(i + 1);
                }
                result.add(row);
            }
        }
        return result;
    }

    public static List<Option> categories() throws SQLException {
        return options("SELECT categoria_id, nombre FROM categoria ORDER BY nombre");
    }

    public static List<Option> sites() throws SQLException {
        return options("SELECT sede_id, nombre FROM sede WHERE activa=TRUE ORDER BY nombre");
    }

    public static List<Option> activeProducts() throws SQLException {
        return options("SELECT producto_id, CONCAT(codigo, ' - ', nombre) "
                + "FROM producto WHERE activo=TRUE ORDER BY nombre");
    }

    private static List<Option> options(String sql) throws SQLException {
        List<Option> result = new ArrayList<>();
        try (Connection cn = Db.connect();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new Option(rs.getLong(1), rs.getString(2)));
            }
        }
        return result;
    }

    public static void saveProduct(Long productId, String code, String name,
            String categoryName, String unit, BigDecimal minimum, boolean active)
            throws SQLException {
        try (Connection cn = Db.connect()) {
            cn.setAutoCommit(false);
            try {
                Long categoryId = getOrCreateCategory(cn, categoryName);
                if (productId == null) {
                    String sql = "INSERT INTO producto "
                            + "(codigo,nombre,categoria_id,unidad_medida,stock_minimo,activo) "
                            + "VALUES (?,?,?,?,?,?)";
                    try (PreparedStatement ps = cn.prepareStatement(sql)) {
                        bindProduct(ps, code, name, categoryId, unit, minimum, active);
                        ps.executeUpdate();
                    }
                } else {
                    String sql = "UPDATE producto SET codigo=?, nombre=?, categoria_id=?, "
                            + "unidad_medida=?, stock_minimo=?, activo=? WHERE producto_id=?";
                    try (PreparedStatement ps = cn.prepareStatement(sql)) {
                        bindProduct(ps, code, name, categoryId, unit, minimum, active);
                        ps.setLong(7, productId);
                        if (ps.executeUpdate() == 0) {
                            throw new SQLException("No se encontró el producto para actualizar.");
                        }
                    }
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

    private static void bindProduct(PreparedStatement ps, String code, String name,
            Long categoryId, String unit, BigDecimal minimum, boolean active)
            throws SQLException {
        ps.setString(1, code.trim());
        ps.setString(2, name.trim());
        if (categoryId == null) ps.setNull(3, java.sql.Types.BIGINT);
        else ps.setLong(3, categoryId);
        ps.setString(4, unit.trim());
        ps.setBigDecimal(5, minimum);
        ps.setBoolean(6, active);
    }

    private static Long getOrCreateCategory(Connection cn, String categoryName)
            throws SQLException {
        if (categoryName == null || categoryName.isBlank()) return null;
        String name = categoryName.trim();
        try (PreparedStatement insert = cn.prepareStatement(
                "INSERT INTO categoria(nombre) VALUES (?) "
                        + "ON DUPLICATE KEY UPDATE nombre=VALUES(nombre)")) {
            insert.setString(1, name);
            insert.executeUpdate();
        }
        try (PreparedStatement select = cn.prepareStatement(
                "SELECT categoria_id FROM categoria WHERE nombre=?")) {
            select.setString(1, name);
            try (ResultSet rs = select.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("No se pudo identificar la categoría seleccionada.");
    }

    public static void registerMovement(long siteId, String type, String adjustmentDirection,
            long productId, BigDecimal quantity, Long userId, String reference,
            String note, BigDecimal unitCost) throws SQLException {
        String sql = "{CALL sp_registrar_movimiento(?,?,?,?,?,?,?,?,?)}";
        try (Connection cn = Db.connect(); CallableStatement cs = cn.prepareCall(sql)) {
            cs.setLong(1, siteId);
            cs.setString(2, type);
            if (adjustmentDirection == null || adjustmentDirection.isBlank()) {
                cs.setNull(3, java.sql.Types.VARCHAR);
            } else {
                cs.setString(3, adjustmentDirection);
            }
            cs.setLong(4, productId);
            cs.setBigDecimal(5, quantity);
            if (userId == null) cs.setNull(6, java.sql.Types.BIGINT);
            else cs.setLong(6, userId);
            cs.setString(7, blankToNull(reference));
            cs.setString(8, blankToNull(note));
            if (unitCost == null) cs.setNull(9, java.sql.Types.DECIMAL);
            else cs.setBigDecimal(9, unitCost);
            cs.execute();
        }
    }
    

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
    

}
