package com.dacero.inventario;

import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/** JFrame Form: abrir Panel.java y elegir Design para editar la interfaz. */
public class Panel extends JFrame {
    private final Color navy = new Color(23, 48, 74);
    private final Color teal = new Color(0, 139, 139);
    private final App user;
    private final JButton btnSedes = new JButton("Sedes");
    private final JButton btnCategorias = new JButton("Categorías");
    private final JButton btnUsuarios = new JButton("Usuarios");
    private final JButton btnSalir = new JButton("Salir");
    private JPanel statsCards;
    private String seccionActual = "Inicio";

    public Panel() {
        this(null);
    }

    public Panel(App user) {
        this.user = user;
        initComponents();
        configurarEstilo();
        construirInterfaz();
        btnInicio.addActionListener(e -> mostrarSeccion("Inicio"));
        btnProductos.addActionListener(e -> mostrarSeccion("Productos"));
        btnMovimientos.addActionListener(e -> mostrarSeccion("Movimientos"));
        btnSedes.addActionListener(e -> mostrarSeccion("Sedes"));
        btnCategorias.addActionListener(e -> mostrarSeccion("Categorías"));
        btnUsuarios.addActionListener(e -> new UserManagementDialog(this).setVisible(true));
        btnSalir.addActionListener(e -> salir());
        btnNuevo.addActionListener(e -> {
            if ("Productos".equals(seccionActual)) nuevoProducto();
            else if ("Movimientos".equals(seccionActual)) registrarMovimiento();
            else if ("Sedes".equals(seccionActual)) editarSede(null, "", "", "", true);
            else if ("Categorías".equals(seccionActual)) editarCategoria(null, "", "");
        });
        btnEditar.addActionListener(e -> {
            if ("Productos".equals(seccionActual)) editarProducto();
            else if ("Sedes".equals(seccionActual)) editarSedeSeleccionada();
            else if ("Categorías".equals(seccionActual)) editarCategoriaSeleccionada();
        });
        btnActualizar.addActionListener(e -> refrescarSeccion());
        btnExportar.addActionListener(e -> exportarTablaCsv());
        mostrarSeccion("Inicio");
    }

    private void configurarEstilo() {
        getContentPane().setBackground(Color.WHITE);
        sidebarPanel.setBackground(navy);
        brandLabel.setForeground(Color.WHITE);
        brandLabel.setFont(new Font("Segoe UI", Font.BOLD, 23));
        for (JButton b : new JButton[]{btnInicio, btnProductos, btnMovimientos, btnReportes,
                btnSedes, btnCategorias, btnUsuarios, btnSalir}) estiloBotonLateral(b);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setForeground(navy);
        for (JPanel card : new JPanel[]{cardProductos, cardReposicion, cardMovimientos}) card.setBackground(new Color(244,248,251));
        for (JLabel value : new JLabel[]{lblProductos, lblReposicion, lblMovimientos}) {
            value.setFont(new Font("Segoe UI", Font.BOLD, 27)); value.setForeground(teal);
        }
        for (JLabel label : new JLabel[]{capProductos, capReposicion, capMovimientos}) label.setForeground(new Color(70,82,95));
        infoLabel.setForeground(new Color(80,95,110));
        dataTable.setRowHeight(30); dataTable.getTableHeader().setBackground(navy);
        dataTable.getTableHeader().setForeground(Color.WHITE); dataTable.setFillsViewportHeight(true);
        for (JButton b : new JButton[]{btnNuevo, btnEditar, btnActualizar, btnExportar}) {
            b.setBackground(teal); b.setForeground(Color.WHITE); b.setFocusPainted(false);
        }
        setTitle("D-ACERO | Gestión de inventario");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 680));
        setSize(1280, 800);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private void estiloBotonLateral(JButton button) {
        button.setForeground(Color.WHITE);
        button.setBackground(navy);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        button.setPreferredSize(new Dimension(200, 44));
    }

    private void construirInterfaz() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        sidebarPanel.removeAll();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setBorder(BorderFactory.createEmptyBorder(22, 16, 18, 16));
        sidebarPanel.setPreferredSize(new Dimension(230, 0));
        brandLabel.setAlignmentX(LEFT_ALIGNMENT);
        sidebarPanel.add(brandLabel);
        sidebarPanel.add(Box.createVerticalStrut(28));
        for (JButton button : new JButton[]{btnInicio, btnProductos, btnSedes,
                btnCategorias, btnMovimientos}) {
            button.setAlignmentX(LEFT_ALIGNMENT);
            sidebarPanel.add(button);
            sidebarPanel.add(Box.createVerticalStrut(7));
        }
        if (user != null && user.isAdmin()) {
            btnUsuarios.setAlignmentX(LEFT_ALIGNMENT);
            sidebarPanel.add(btnUsuarios);
            sidebarPanel.add(Box.createVerticalStrut(7));
        }
        sidebarPanel.add(Box.createVerticalGlue());
        btnSalir.setAlignmentX(LEFT_ALIGNMENT);
        sidebarPanel.add(btnSalir);

        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 4));
        heading.setOpaque(false);
        heading.add(titleLabel);
        heading.add(infoLabel);
        statsCards = new JPanel(new GridLayout(1, 3, 14, 0));
        statsCards.setOpaque(false);
        for (JPanel card : new JPanel[]{cardProductos, cardReposicion, cardMovimientos}) {
            card.setPreferredSize(new Dimension(260, 94));
            statsCards.add(card);
        }
        JPanel header = new JPanel(new BorderLayout(0, 16));
        header.setOpaque(false);
        header.add(heading, BorderLayout.NORTH);
        header.add(statsCards, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        actions.setOpaque(false);
        actions.add(btnNuevo);
        actions.add(btnEditar);
        actions.add(btnActualizar);
        actions.add(btnExportar);

        mainPanel.removeAll();
        mainPanel.setLayout(new BorderLayout(12, 16));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(24, 26, 18, 26));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.add(header, BorderLayout.NORTH);
        mainPanel.add(dataScroll, BorderLayout.CENTER);
        mainPanel.add(actions, BorderLayout.SOUTH);

        root.add(sidebarPanel, BorderLayout.WEST);
        root.add(mainPanel, BorderLayout.CENTER);
        setContentPane(root);
        revalidate();
    }

    private void mostrarSeccion(String seccion) {
        seccionActual = seccion;
        titleLabel.setText(seccion.equals("Inicio") ? "Panel principal" : seccion);
        boolean inicio = seccion.equals("Inicio");
        cardProductos.setVisible(inicio); cardReposicion.setVisible(inicio); cardMovimientos.setVisible(inicio);
        statsCards.setVisible(inicio);
        if (inicio) {
            infoLabel.setText("Inventario por sede · " + (user == null ? "D-ACERO" : "Sesión: " + user.name()));
            actualizarMetricas();
            cargarTabla(new String[]{"Módulo", "Descripción", "Estado"}, new Object[][]{
                {"Productos", "Catálogo e inventario por sede", "Disponible"},
                {"Sedes", "Administración de sedes", "Disponible"},
                {"Categorías", "Clasificación de productos", "Disponible"},
                {"Movimientos", "Entradas, salidas y ajustes", "Disponible"}});
        } else if (seccion.equals("Productos")) {
            infoLabel.setText("Catálogo, categoría, stock mínimo y saldo por sede");
            cargarProductos();
        } else if (seccion.equals("Movimientos")) {
            infoLabel.setText("Historial de entradas y salidas registrado en MySQL");
            cargarMovimientos();
        } else if (seccion.equals("Sedes")) {
            infoLabel.setText("Administra las sedes y sus ubicaciones");
            cargarSedes();
        } else if (seccion.equals("Categorías")) {
            infoLabel.setText("Administra las categorías del catálogo");
            cargarCategorias();
        } else {
            mostrarSeccion("Inicio");
            return;
        }
        boolean listado = !inicio;
        dataScroll.setVisible(true);
        btnNuevo.setText(seccion.equals("Movimientos") ? "Registrar mov."
                : seccion.equals("Sedes") ? "Nueva sede"
                : seccion.equals("Categorías") ? "Nueva categoría" : "Nuevo producto");
        boolean canWrite = user == null || user.canWrite();
        btnNuevo.setVisible(canWrite && List.of("Productos", "Movimientos", "Sedes", "Categorías").contains(seccion));
        btnEditar.setVisible(canWrite && List.of("Productos", "Sedes", "Categorías").contains(seccion));
        btnActualizar.setVisible(listado);
        btnExportar.setText("Exportar CSV");
        btnExportar.setVisible(listado);
        mainPanel.revalidate(); mainPanel.repaint();
    }

    private void actualizarMetricas() {
        try {
            Inventario.Metrics m = Inventario.metrics();
            lblProductos.setText(Long.toString(m.products()));
            lblReposicion.setText(Long.toString(m.lowStock()));
            lblMovimientos.setText(Long.toString(m.monthlyMovements()));
        } catch (SQLException ex) {
            mostrarError(ex);
        }
    }

    private void cargarProductos() {
        try {
            cargarTabla(new String[]{"ID", "Código", "Producto", "Categoría", "Unidad",
                "Stock mínimo", "Activo", "Sede", "Existencia"},
                Inventario.products().toArray(Object[][]::new));
            ocultarId();
        } catch (SQLException ex) { mostrarError(ex); }
    }

    private void cargarMovimientos() {
        try {
            cargarTabla(new String[]{"ID", "Fecha", "Tipo", "Sede", "Código", "Producto",
                "Cantidad", "Usuario", "Referencia"},
                Inventario.movements().toArray(Object[][]::new));
        } catch (SQLException ex) { mostrarError(ex); }
    }

    private void cargarSedes() {
        try { cargarTabla(new String[]{"ID", "Nombre", "Municipio", "Dirección", "Activa"}, Inventario.siteRows().toArray(Object[][]::new)); ocultarId(); }
        catch (SQLException ex) { mostrarError(ex); }
    }

    private void cargarCategorias() {
        try { cargarTabla(new String[]{"ID", "Nombre", "Descripción"}, Inventario.categoryRows().toArray(Object[][]::new)); ocultarId(); }
        catch (SQLException ex) { mostrarError(ex); }
    }

    private void ocultarId() {
        if (dataTable.getColumnCount() > 0) dataTable.removeColumn(dataTable.getColumnModel().getColumn(0));
    }

    private void refrescarSeccion() {
        if ("Inicio".equals(seccionActual)) mostrarSeccion("Inicio");
        else if ("Productos".equals(seccionActual)) cargarProductos();
        else if ("Movimientos".equals(seccionActual)) cargarMovimientos();
        else if ("Sedes".equals(seccionActual)) cargarSedes();
        else if ("Categorías".equals(seccionActual)) cargarCategorias();
    }

    private void nuevoProducto() {
        editarProducto(null, null, "", "", "", BigDecimal.ZERO, true);
    }

    private void editarProducto() {
        int row = dataTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un producto de la tabla.",
                    "Editar producto", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = dataTable.convertRowIndexToModel(row);
        TableModel model = dataTable.getModel();
        Object[] values = new Object[model.getColumnCount()];
        for (int i = 0; i < values.length; i++) values[i] = model.getValueAt(modelRow, i);
        try {
            editarProducto(((Number) values[0]).longValue(), String.valueOf(values[1]),
                    String.valueOf(values[2]), String.valueOf(values[3]),
                    String.valueOf(values[4]), new BigDecimal(String.valueOf(values[5])),
                    Boolean.parseBoolean(String.valueOf(values[6])));
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "No se pudieron leer los datos del producto seleccionado.",
                    "Editar producto", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editarProducto(Long id, String oldCode, String oldName, String oldCategory,
            String oldUnit, BigDecimal oldMinimum, boolean oldActive) {
        JTextField code = new JTextField(oldCode, 20);
        JTextField name = new JTextField(oldName, 20);
        JTextField category = new JTextField("Sin categoría".equals(oldCategory) ? "" : oldCategory, 20);
        JTextField unit = new JTextField(oldUnit, 20);
        JTextField minimum = new JTextField(oldMinimum.toPlainString(), 20);
        JCheckBox active = new JCheckBox("Producto activo", oldActive);
        JPanel form = new JPanel(new java.awt.GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Código:")); form.add(code);
        form.add(new JLabel("Nombre:")); form.add(name);
        form.add(new JLabel("Categoría:")); form.add(category);
        form.add(new JLabel("Unidad de medida:")); form.add(unit);
        form.add(new JLabel("Stock mínimo:")); form.add(minimum);
        form.add(new JLabel("Estado:")); form.add(active);
        int answer = JOptionPane.showConfirmDialog(this, form,
                id == null ? "Crear producto" : "Editar producto",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) return;
        try {
            if (code.getText().isBlank() || name.getText().isBlank() || unit.getText().isBlank())
                throw new IllegalArgumentException("Código, nombre y unidad son obligatorios.");
            BigDecimal stock = new BigDecimal(minimum.getText().trim());
            if (stock.signum() < 0) throw new IllegalArgumentException("El stock mínimo no puede ser negativo.");
            Inventario.saveProduct(id, code.getText(), name.getText(),
                    category.getText(), unit.getText(), stock, active.isSelected());
            cargarProductos();
            JOptionPane.showMessageDialog(this, "Producto guardado correctamente.",
                    "Productos", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException | IllegalArgumentException ex) {
            mostrarError(ex);
        }
    }

    private void editarSedeSeleccionada() {
        int row = dataTable.getSelectedRow();
        if (row < 0) { sinSeleccion("sede"); return; }
        Object[] v = filaModelo(row);
        editarSede(((Number)v[0]).longValue(), String.valueOf(v[1]), String.valueOf(v[2]),
                v[3] == null ? "" : String.valueOf(v[3]), Boolean.parseBoolean(String.valueOf(v[4])));
    }

    private void editarSede(Long id, String name, String municipality, String address, boolean active) {
        JTextField nombre = new JTextField(name, 22), municipio = new JTextField(municipality, 22), direccion = new JTextField(address, 22);
        JCheckBox activa = new JCheckBox("Sede activa", active);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Nombre:")); form.add(nombre); form.add(new JLabel("Municipio:")); form.add(municipio);
        form.add(new JLabel("Dirección:")); form.add(direccion); form.add(new JLabel("Estado:")); form.add(activa);
        if (JOptionPane.showConfirmDialog(this, form, id == null ? "Nueva sede" : "Editar sede",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try { Inventario.saveSite(id, nombre.getText(), municipio.getText(), direccion.getText(), activa.isSelected()); cargarSedes(); }
        catch (SQLException | IllegalArgumentException ex) { mostrarError(ex); }
    }

    private void editarCategoriaSeleccionada() {
        int row = dataTable.getSelectedRow();
        if (row < 0) { sinSeleccion("categoría"); return; }
        Object[] v = filaModelo(row);
        editarCategoria(((Number)v[0]).longValue(), String.valueOf(v[1]), v[2] == null ? "" : String.valueOf(v[2]));
    }

    private void editarCategoria(Long id, String name, String description) {
        JTextField nombre = new JTextField(name, 22), descripcion = new JTextField(description, 22);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Nombre:")); form.add(nombre); form.add(new JLabel("Descripción:")); form.add(descripcion);
        if (JOptionPane.showConfirmDialog(this, form, id == null ? "Nueva categoría" : "Editar categoría",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try { Inventario.saveCategory(id, nombre.getText(), descripcion.getText()); cargarCategorias(); }
        catch (SQLException | IllegalArgumentException ex) { mostrarError(ex); }
    }

    private Object[] filaModelo(int viewRow) {
        int row = dataTable.convertRowIndexToModel(viewRow);
        TableModel model = dataTable.getModel();
        Object[] values = new Object[model.getColumnCount()];
        for (int i = 0; i < values.length; i++) values[i] = model.getValueAt(row, i);
        return values;
    }

    private void sinSeleccion(String entity) {
        JOptionPane.showMessageDialog(this, "Selecciona una " + entity + " de la tabla.",
                "Editar", JOptionPane.INFORMATION_MESSAGE);
    }

    private void registrarMovimiento() {
        try {
            List<Inventario.Option> sites = Inventario.sites();
            List<Inventario.Option> products = Inventario.activeProducts();
            if (sites.isEmpty() || products.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Primero crea al menos una sede y un producto activo.",
                        "Registrar movimiento", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JComboBox<Inventario.Option> site = new JComboBox<>(sites.toArray(Inventario.Option[]::new));
            JComboBox<String> type = new JComboBox<>(new String[]{"ENTRADA", "SALIDA", "AJUSTE"});
            JComboBox<String> direction = new JComboBox<>(new String[]{"ENTRADA", "SALIDA"});
            JComboBox<Inventario.Option> product = new JComboBox<>(products.toArray(Inventario.Option[]::new));
            JTextField quantity = new JTextField("1", 12);
            JTextField cost = new JTextField("", 12);
            JTextField reference = new JTextField("", 18);
            JTextField note = new JTextField("", 18);
            JPanel form = new JPanel(new java.awt.GridLayout(0, 2, 8, 8));
            form.add(new JLabel("Sede:")); form.add(site);
            form.add(new JLabel("Tipo:")); form.add(type);
            form.add(new JLabel("Dirección del ajuste:")); form.add(direction);
            form.add(new JLabel("Producto:")); form.add(product);
            form.add(new JLabel("Cantidad:")); form.add(quantity);
            form.add(new JLabel("Costo unitario (opcional):")); form.add(cost);
            form.add(new JLabel("Referencia (opcional):")); form.add(reference);
            form.add(new JLabel("Observación (opcional):")); form.add(note);

            if (JOptionPane.showConfirmDialog(this, form, "Registrar movimiento",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

            Inventario.Option selectedSite = (Inventario.Option) site.getSelectedItem();
            Inventario.Option selectedProduct = (Inventario.Option) product.getSelectedItem();
            String movementType = String.valueOf(type.getSelectedItem());
            String adjustment = "AJUSTE".equals(movementType)
                    ? String.valueOf(direction.getSelectedItem()) : null;
            BigDecimal qty = new BigDecimal(quantity.getText().trim());
            BigDecimal unitCost = cost.getText().isBlank() ? null : new BigDecimal(cost.getText().trim());
            if (qty.signum() <= 0 || (unitCost != null && unitCost.signum() < 0))
                throw new IllegalArgumentException("Cantidad y costo deben ser valores válidos.");
            Inventario.registerMovement(selectedSite.id(), movementType, adjustment,
                    selectedProduct.id(), qty, user == null ? null : user.id(),
                    reference.getText(), note.getText(), unitCost);
            cargarMovimientos();
            actualizarMetricas();
            JOptionPane.showMessageDialog(this,
                    "Movimiento registrado. El inventario se actualizó automáticamente.",
                    "Movimiento", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException | IllegalArgumentException ex) {
            mostrarError(ex);
        }
    }

    private void exportarTablaCsv() {
        TableModel model = dataTable.getModel();
        if (model.getColumnCount() == 0) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(seccionActual.toLowerCase().replace(' ', '_') + ".csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File destination = chooser.getSelectedFile();
        try {
            if (!destination.getName().toLowerCase().endsWith(".csv")) destination = new File(destination.getParentFile(), destination.getName() + ".csv");
            StringBuilder csv = new StringBuilder("\uFEFF");
            for (int c = 0; c < dataTable.getColumnCount(); c++) {
                if (c > 0) csv.append(',');
                csv.append(csvValue(dataTable.getColumnName(c)));
            }
            csv.append("\r\n");
            for (int r = 0; r < dataTable.getRowCount(); r++) {
                for (int c = 0; c < dataTable.getColumnCount(); c++) {
                    if (c > 0) csv.append(',');
                    csv.append(csvValue(dataTable.getValueAt(r, c)));
                }
                csv.append("\r\n");
            }
            Files.writeString(destination.toPath(), csv.toString(), StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(this, "Tabla exportada en:\n" + destination.getAbsolutePath(), "Exportación", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) { mostrarError(ex); }
    }

    private String csvValue(Object value) {
        String text = value == null ? "" : value.toString();
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private void salir() {
        dispose();
        Login login = new Login();
        login.setVisible(true);
    }

    private void mostrarError(Exception ex) {
        String message = ex.getMessage() == null ? "Ocurrió un error al guardar." : ex.getMessage();
        if (ex instanceof SQLException sql && (sql.getErrorCode() == 1062 || "23000".equals(sql.getSQLState()))) {
            if (message.toLowerCase().contains("duplicate") || sql.getErrorCode() == 1062)
                message = "Ya existe un registro con esos datos. Verifica el código o nombre e inténtalo de nuevo.";
            else if (message.toLowerCase().contains("foreign key") || message.toLowerCase().contains("constraint"))
                message = "No se puede guardar: el registro está relacionado con otros datos o falta una selección válida.";
        }
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void cargarTabla(String[] columnas, Object[][] filas) {
        dataTable.setModel(new DefaultTableModel(filas, columnas) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        });
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    @SuppressWarnings("unchecked")
    private void initComponents() {
        sidebarPanel = new javax.swing.JPanel(); brandLabel = new javax.swing.JLabel();
        btnInicio = new javax.swing.JButton(); btnProductos = new javax.swing.JButton();
        btnMovimientos = new javax.swing.JButton(); btnReportes = new javax.swing.JButton();
        mainPanel = new javax.swing.JPanel(); titleLabel = new javax.swing.JLabel(); infoLabel = new javax.swing.JLabel();
        cardProductos = new javax.swing.JPanel(); lblProductos = new javax.swing.JLabel(); capProductos = new javax.swing.JLabel();
        cardReposicion = new javax.swing.JPanel(); lblReposicion = new javax.swing.JLabel(); capReposicion = new javax.swing.JLabel();
        cardMovimientos = new javax.swing.JPanel(); lblMovimientos = new javax.swing.JLabel(); capMovimientos = new javax.swing.JLabel();
        dataScroll = new javax.swing.JScrollPane(); dataTable = new javax.swing.JTable();
        btnNuevo = new javax.swing.JButton(); btnEditar = new javax.swing.JButton(); btnActualizar = new javax.swing.JButton(); btnExportar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("D-ACERO | Gestión de inventario");
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        sidebarPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        brandLabel.setText("D-ACERO"); sidebarPanel.add(brandLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 24, 180, 48));
        btnInicio.setText("Inicio"); sidebarPanel.add(btnInicio, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 105, 180, 42));
        btnProductos.setText("Productos"); sidebarPanel.add(btnProductos, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 155, 180, 42));
        btnMovimientos.setText("Movimientos"); sidebarPanel.add(btnMovimientos, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 205, 180, 42));
        btnReportes.setText("Consultas y reportes"); sidebarPanel.add(btnReportes, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 255, 180, 42));
        getContentPane().add(sidebarPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 220, 740));

        mainPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        titleLabel.setText("Panel principal"); mainPanel.add(titleLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 20, 870, 42));
        infoLabel.setText("Inventario por sede · Sincelejo y Sahagún"); mainPanel.add(infoLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(12, 62, 870, 28));
        cardProductos.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout()); lblProductos.setText("—"); capProductos.setText("Productos activos"); cardProductos.add(lblProductos, new org.netbeans.lib.awtextra.AbsoluteConstraints(18,10,235,40)); cardProductos.add(capProductos, new org.netbeans.lib.awtextra.AbsoluteConstraints(18,53,235,25)); mainPanel.add(cardProductos, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 105, 270, 88));
        cardReposicion.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout()); lblReposicion.setText("—"); capReposicion.setText("Referencias por reponer"); cardReposicion.add(lblReposicion, new org.netbeans.lib.awtextra.AbsoluteConstraints(18,10,235,40)); cardReposicion.add(capReposicion, new org.netbeans.lib.awtextra.AbsoluteConstraints(18,53,235,25)); mainPanel.add(cardReposicion, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 105, 270, 88));
        cardMovimientos.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout()); lblMovimientos.setText("—"); capMovimientos.setText("Movimientos del mes"); cardMovimientos.add(lblMovimientos, new org.netbeans.lib.awtextra.AbsoluteConstraints(18,10,235,40)); cardMovimientos.add(capMovimientos, new org.netbeans.lib.awtextra.AbsoluteConstraints(18,53,235,25)); mainPanel.add(cardMovimientos, new org.netbeans.lib.awtextra.AbsoluteConstraints(590, 105, 270, 88));
        dataTable.setModel(new DefaultTableModel(new Object[][]{}, new String[]{"Módulo", "Descripción", "Estado"}));
        dataScroll.setViewportView(dataTable); mainPanel.add(dataScroll, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 215, 850, 390));
        btnNuevo.setText("Nuevo producto"); mainPanel.add(btnNuevo, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 625, 145, 38));
        btnEditar.setText("Editar"); mainPanel.add(btnEditar, new org.netbeans.lib.awtextra.AbsoluteConstraints(165, 625, 120, 38));
        btnActualizar.setText("Actualizar"); mainPanel.add(btnActualizar, new org.netbeans.lib.awtextra.AbsoluteConstraints(295, 625, 130, 38));
        btnExportar.setText("Exportar PDF"); mainPanel.add(btnExportar, new org.netbeans.lib.awtextra.AbsoluteConstraints(435, 625, 145, 38));
        getContentPane().add(mainPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 0, 890, 700));
        pack();
    }// </editor-fold>//GEN-END:initComponents

//GEN-BEGIN:variables
    private javax.swing.JPanel sidebarPanel, mainPanel, cardProductos, cardReposicion, cardMovimientos;
    private javax.swing.JLabel brandLabel, titleLabel, infoLabel, lblProductos, capProductos, lblReposicion, capReposicion, lblMovimientos, capMovimientos;
    private javax.swing.JButton btnInicio, btnProductos, btnMovimientos, btnReportes, btnNuevo, btnEditar, btnActualizar, btnExportar;
    private javax.swing.JScrollPane dataScroll;
    private javax.swing.JTable dataTable;
//GEN-END:variables
}

