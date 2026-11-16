package com.dacero.inventario;

public record App(long id, String name, String username, String role) {
    public boolean canWrite() {
        return !"CONSULTA".equalsIgnoreCase(role);
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}
