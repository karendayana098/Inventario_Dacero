package com.dacero.inventario;
import javax.swing.SwingUtilities;
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Login login = new Login();
            if (login.isReady()) login.setVisible(true);
        });
    }
}
