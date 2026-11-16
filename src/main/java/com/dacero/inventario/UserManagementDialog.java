package com.dacero.inventario;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
    private final JTable table = new JTable();
    private List<Autenticacion.UserEntry> users = List.of();

    UserManagementDialog(Panel owner) {
        super(owner, "Gestión de usuarios", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        table.setRowHeight(28);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton add = new JButton("Nuevo usuario");
        JButton edit = new JButton("Editar / contraseña");
        JButton refresh = new JButton("Actualizar");
        JButton export = new JButton("Exportar CSV");
        JButton close = new JButton("Cerrar");
        add.addActionListener(e -> editUser(null));
        edit.addActionListener(e -> editSelected());
        refresh.addActionListener(e -> loadUsers());
        export.addActionListener(e -> exportUsers());
        close.addActionListener(e -> dispose());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(add);
        actions.add(edit);
        actions.add(refresh);
        actions.add(export);
        actions.add(close);
        add(actions, BorderLayout.SOUTH);

        setSize(800, 460);
        setLocationRelativeTo(owner);
        loadUsers();
    }

    private void loadUsers() {
        try {
            users = Autenticacion.users();
            Object[][] rows = users.stream().map(u -> new Object[]{u.id(), u.name(),
                u.username(), u.role(), u.active() ? "Activo" : "Inactivo"}).toArray(Object[][]::new);
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
        JComboBox<String> role = new JComboBox<>(new String[]{"ADMIN", "OPERADOR", "CONSULTA"});
        if (current != null) role.setSelectedItem(current.role());
        JCheckBox active = new JCheckBox("Cuenta activa", current == null || current.active());
        JPasswordField password = new JPasswordField(24);
        JPasswordField confirmation = new JPasswordField(24);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        form.add(new JLabel("Nombre completo:")); form.add(name);
        form.add(new JLabel("Usuario:")); form.add(username);
        form.add(new JLabel("Rol:")); form.add(role);
        form.add(new JLabel("Estado:")); form.add(active);
        form.add(new JLabel(current == null ? "Contraseña (8+):" : "Nueva contraseña (opcional):"));
        form.add(password);
        form.add(new JLabel("Confirmar contraseña:")); form.add(confirmation);

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
                        (String) role.getSelectedItem(), secret);
            } else {
                Autenticacion.updateUser(current.id(), name.getText(), username.getText(),
                        (String) role.getSelectedItem(), active.isSelected(), secret);
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
        chooser.setSelectedFile(new File("usuarios.csv"));
        if (chooser.showSaveDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) file = new File(file.getParentFile(), file.getName() + ".csv");
        StringBuilder csv = new StringBuilder("\uFEFF\"Nombre\",\"Usuario\",\"Rol\",\"Estado\"\r\n");
        for (Autenticacion.UserEntry user : users) {
            csv.append(csv(user.name())).append(',').append(csv(user.username())).append(',')
                    .append(csv(user.role())).append(',').append(csv(user.active() ? "Activo" : "Inactivo"))
                    .append("\r\n");
        }
        try {
            Files.writeString(file.toPath(), csv.toString(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo exportar", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String csv(String value) {
        return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\"";
    }
}
