package com.dacero.inventario;

public record AppUser(long id, String name, String username, String role) {
    public boolean canWrite() {
        return !"CONSULTA".equalsIgnoreCase(role);
    }
}
