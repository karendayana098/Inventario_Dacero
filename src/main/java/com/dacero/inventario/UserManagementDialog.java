package com.dacero.inventario;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

final class UserManagementDialog extends JDialog {
    private record RoleChoice(String code, String label) {
        @Override public String toString() { return label; }
    }

    private final Color navy = new Color(23, 48, 74);
    private final Color teal = new Color(0, 139, 139);
    private final JTable table = new JTable();
    private List<Autenticacion.UserEntry> users = List.of();

    UserManagementDialog(Panel owner) {
        super(owner, "Gestión de usuarios", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(0, 14));
        getContentPane().setBackground(new Color(243, 246, 249));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(20, 22, 16, 22));
        JLabel heading = new JLabel("Cuentas con acceso al sistema");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 21));
        heading.setForeground(navy);
        add(heading, BorderLayout.NORTH);
        table.setRowHeight(34);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setSelectionBackground(new Color(218, 240, 238));
        table.setSelectionForeground(navy);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.getTableHeader().setBackground(new Color(235, 241, 245));
        table.getTableHeader().setForeground(navy);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(225, 232, 238)));
        add(tableScroll, BorderLayout.CENTER);

        JButton add = new JButton("Nuevo usuario");
        JButton edit = new JButton("Editar / contraseña");
        JButton refresh = new JButton("Actualizar");
        JButton export = new JButton("Exportar a Excel");
        JButton close = new JButton("Cerrar");
        add.addActionListener(e -> editUser(null));
        edit.addActionListener(e -> editSelected());
        refresh.addActionListener(e -> loadUsers());
        export.addActionListener(e -> exportUsers());
        close.addActionListener(e -> dispose());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(add);
        actions.add(edit);
        actions.add(refresh);
        actions.add(export);
        actions.add(close);
        add(actions, BorderLayout.SOUTH);
        for (JButton button : new JButton[]{add, edit, refresh, export, close}) {
            button.setFont(new Font("Segoe UI", Font.BOLD, 12));
            button.setFocusPainted(false);
            button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        }
        add.setBackground(teal); add.setForeground(Color.WHITE);
        edit.setBackground(navy); edit.setForeground(Color.WHITE);
        refresh.setBackground(Color.WHITE); export.setBackground(Color.WHITE); close.setBackground(Color.WHITE);

        setSize(900, 520);
        setLocationRelativeTo(owner);
        loadUsers();
    }

    private void loadUsers() {
        try {
            users = Autenticacion.users();
            Object[][] rows = users.stream().map(u -> new Object[]{u.id(), u.name(),
                u.username(), roleLabel(u.role()), u.active() ? "Activo" : "Inactivo"}).toArray(Object[][]::new);
            table.setModel(new DefaultTableModel(rows,
                    new String[]{"ID", "Nombre", "Usuario", "Rol", "Estado"}) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            });
            table.removeColumn(table.getColumnModel().getColumn(0));
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error al cargar usuarios",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= users.size()) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario.",
                    "Gestión de usuarios", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        editUser(users.get(row));
    }

    private void editUser(Autenticacion.UserEntry current) {
        JTextField name = new JTextField(current == null ? "" : current.name(), 24);
        JTextField username = new JTextField(current == null ? "" : current.username(), 24);
        JComboBox<RoleChoice> role = new JComboBox<>(new RoleChoice[]{
            new RoleChoice("ADMINISTRADOR", "Administrador"),
            new RoleChoice("VENDEDOR", "Vendedor"),
            new RoleChoice("BODEGUERO", "Bodeguero")
        });
        role.setToolTipText("Administrador: acceso total. Vendedor: registra salidas. Bodeguero: administra productos y movimientos de stock.");
        if (current != null) role.setSelectedItem(roleChoice(current.role()));
        JCheckBox active = new JCheckBox("Cuenta activa", current == null || current.active());
        JPasswordField password = new JPasswordField(24);
        JPasswordField confirmation = new JPasswordField(24);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        form.add(new JLabel("Nombre completo:")); form.add(name);
        form.add(new JLabel("Usuario:")); form.add(username);
        form.add(new JLabel("Tipo de usuario:")); form.add(role);
        form.add(new JLabel("Estado:")); form.add(active);
        form.add(new JLabel(current == null ? "Contraseña (8+):" : "Nueva contraseña (opcional):"));
        form.add(password);
        form.add(new JLabel("Confirmar contraseña:")); form.add(confirmation);
        form.add(new JLabel("Permisos del tipo:"));
        form.add(new JLabel("Admin: todo · Vendedor: salidas · Bodeguero: productos y stock"));

        int result = JOptionPane.showConfirmDialog(this, form,
                current == null ? "Crear usuario" : "Editar usuario",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        char[] secret = password.getPassword();
        char[] confirm = confirmation.getPassword();
        try {
            if (!Arrays.equals(secret, confirm)) {
                throw new IllegalArgumentException("Las contraseñas no coinciden.");
            }
            if (current == null) {
                Autenticacion.createUser(name.getText(), username.getText(),
                        ((RoleChoice) role.getSelectedItem()).code(), secret);
            } else {
                Autenticacion.updateUser(current.id(), name.getText(), username.getText(),
                        ((RoleChoice) role.getSelectedItem()).code(), active.isSelected(), secret);
            }
            loadUsers();
        } catch (SQLException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo guardar el usuario",
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(secret, '\0');
            Arrays.fill(confirm, '\0');
        }
    }

    private void exportUsers() {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setSelectedFile(new File("usuarios.xlsx"));
        if (chooser.showSaveDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".xlsx")) file = new File(file.getParentFile(), file.getName() + ".xlsx");
        try {
            ExcelTableExporter.export(table, file.toPath(), "Usuarios");
            JOptionPane.showMessageDialog(this, "Tabla de Excel guardada en:\n" + file.getAbsolutePath(),
                    "Exportación completada", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo exportar", JOptionPane.ERROR_MESSAGE);
        }
    }

    private RoleChoice roleChoice(String role) {
        return switch (role == null ? "" : role.toUpperCase()) {
            case "ADMIN", "ADMINISTRADOR" -> new RoleChoice("ADMINISTRADOR", "Administrador");
            case "CONSULTA", "VENDEDOR" -> new RoleChoice("VENDEDOR", "Vendedor");
            case "OPERADOR", "BODEGUERO" -> new RoleChoice("BODEGUERO", "Bodeguero");
            default -> new RoleChoice("BODEGUERO", "Bodeguero");
        };
    }

    private String roleLabel(String role) {
        return roleChoice(role).label();
    }
}
