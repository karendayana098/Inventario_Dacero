package com.dacero.inventario;

public record App(long id, String name, String username, String role) {
    public boolean isAdmin() {
        return isRole("ADMINISTRADOR", "ADMIN");
    }

    public boolean isSeller() {
        return isRole("VENDEDOR", "CONSULTA");
    }

    public boolean canManageCatalog() {
        return isAdmin() || isRole("BODEGUERO", "OPERADOR");
    }

    public boolean canManageConfiguration() {
        return isAdmin();
    }

    public boolean canWrite() {
        return !isSeller();
    }

    public String roleLabel() {
        if (isAdmin()) return "Administrador";
        if (isSeller()) return "Vendedor";
        if (isRole("BODEGUERO", "OPERADOR")) return "Bodeguero";
        return role;
    }

    private boolean isRole(String... values) {
        for (String value : values) if (value.equalsIgnoreCase(role)) return true;
        return false;
    }
}
