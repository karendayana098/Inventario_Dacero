package com.dacero.inventario;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public final class Login extends JFrame {
    private final JTextField userField = new JTextField(24);
    private final JPasswordField passwordField = new JPasswordField(24);
    private final Color navy = new Color(23, 48, 74);
    private final Color teal = new Color(0, 139, 139);
    private boolean ready = true;

    public Login() {
        setTitle("D-ACERO | Iniciar sesión");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        buildUi();
        setLocationRelativeTo(null);

        try {
            Db.verifyConnection();
            if (!Autenticacion.hasUsers() && !createFirstAdmin()) {
                ready = false;
                dispose();
            }
        } catch (SQLException ex) {
            ready = false;
            JOptionPane.showMessageDialog(this,
                    "No se pudo conectar con MySQL.\n" + ex.getMessage()
                    + "\n\nVerifica que MySQL esté iniciado y que db.local.properties tenga el usuario y contraseña correctos.",
                    "Error de conexión", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    public boolean isReady() {
        return ready;
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(18, 18));
        root.setBorder(BorderFactory.createEmptyBorder(28, 34, 30, 34));
        root.setBackground(Color.WHITE);

        JLabel title = new JLabel("D-ACERO", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(navy);
        JLabel subtitle = new JLabel("Inventario de materiales", SwingConstants.CENTER);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(new Color(90, 103, 117));
        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 4));
        heading.setBackground(Color.WHITE);
        heading.add(title);
        heading.add(subtitle);

        JPanel fields = new JPanel(new GridLayout(4, 1, 0, 8));
        fields.setBackground(Color.WHITE);
        fields.add(new JLabel("Usuario"));
        fields.add(userField);
        fields.add(new JLabel("Contraseña"));
        fields.add(passwordField);

        JButton loginButton = new JButton("Ingresar");
        loginButton.setBackground(teal);
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.addActionListener(e -> doLogin());
        getRootPane().setDefaultButton(loginButton);

        root.add(heading, BorderLayout.NORTH);
        root.add(fields, BorderLayout.CENTER);
        root.add(loginButton, BorderLayout.SOUTH);
        setContentPane(root);
        Font inputFont = new Font("Segoe UI", Font.PLAIN, 17);
        userField.setFont(inputFont);
        passwordField.setFont(inputFont);
        userField.setPreferredSize(new Dimension(300, 42));
        passwordField.setPreferredSize(new Dimension(300, 42));
        setSize(new Dimension(460, 340));
    }

    private boolean createFirstAdmin() {
        JTextField name = new JTextField(18);
        JTextField username = new JTextField(18);
        JPasswordField password = new JPasswordField(18);
        JPasswordField confirmation = new JPasswordField(18);
        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        form.add(new JLabel("Nombre completo:"));
        form.add(name);
        form.add(new JLabel("Usuario administrador:"));
        form.add(username);
        form.add(new JLabel("Contraseña (mínimo 8 caracteres):"));
        form.add(password);
        form.add(new JLabel("Confirmar contraseña:"));
        form.add(confirmation);

        int answer = JOptionPane.showConfirmDialog(this, form,
                "Configuración inicial: crear administrador",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) return false;

        char[] secret = password.getPassword();
        char[] confirm = confirmation.getPassword();
        if (!java.util.Arrays.equals(secret, confirm)) {
            java.util.Arrays.fill(secret, '\0');
            java.util.Arrays.fill(confirm, '\0');
            JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden.",
                    "Revisa los datos", JOptionPane.WARNING_MESSAGE);
            return createFirstAdmin();
        }
        try {
            Autenticacion.createFirstAdmin(name.getText(), username.getText(), secret);
            JOptionPane.showMessageDialog(this,
                    "Administrador creado. Ahora inicia sesión con ese usuario.",
                    "Configuración completa", JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (SQLException | IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "No se pudo crear el administrador", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            java.util.Arrays.fill(secret, '\0');
            java.util.Arrays.fill(confirm, '\0');
        }
    }

    private void doLogin() {
        char[] password = passwordField.getPassword();
        try {
            App user = Autenticacion.authenticate(userField.getText(), password);
            if (user == null) {
                JOptionPane.showMessageDialog(this,
                        "Usuario o contraseña incorrectos, o cuenta inactiva.",
                        "Acceso denegado", JOptionPane.WARNING_MESSAGE);
                passwordField.setText("");
                return;
            }
            new Panel(user).setVisible(true);
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo validar el usuario.\n" + ex.getMessage(),
                    "Error de acceso", JOptionPane.ERROR_MESSAGE);
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }
}
