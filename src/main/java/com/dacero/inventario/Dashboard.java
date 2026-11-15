package com.dacero.inventario;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** JFrame Form: abrir Dashboard.java y elegir Design para editar la interfaz. */
public class Dashboard extends JFrame {
    private final Color navy = new Color(23, 48, 74);
    private final Color teal = new Color(0, 139, 139);
    private final AppUser user;
    private final JComboBox<String> reportSelector = new JComboBox<>(new String[]{
        "Existencias por sede", "Productos con stock bajo", "Historial de movimientos"
    });
    private String seccionActual = "Inicio";

    public Dashboard() {
        this(null);
    }

    public Dashboard(AppUser user) {
        this.user = user;
        initComponents();
        configurarEstilo();
        reportSelector.setVisible(false);
        reportSelector.addActionListener(e -> cargarReporteSeleccionado());
        mainPanel.add(reportSelector, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 105, 300, 30));
        btnInicio.addActionListener(e -> mostrarSeccion("Inicio"));
        btnProductos.addActionListener(e -> mostrarSeccion("Productos"));
        btnMovimientos.addActionListener(e -> mostrarSeccion("Movimientos"));
        btnReportes.addActionListener(e -> mostrarSeccion("Consultas y reportes"));
        btnNuevo.addActionListener(e -> {
            if ("Productos".equals(seccionActual)) nuevoProducto();
            else if ("Movimientos".equals(seccionActual)) registrarMovimiento();
        });
        btnEditar.addActionListener(e -> editarProducto());
        btnActualizar.addActionListener(e -> refrescarSeccion());
        btnExportar.addActionListener(e -> exportarReporte());
        mostrarSeccion("Inicio");
    }

    private void configurarEstilo() {
        getContentPane().setBackground(Color.WHITE);
        sidebarPanel.setBackground(navy);
        brandLabel.setForeground(Color.WHITE);
        brandLabel.setFont(new Font("Segoe UI", Font.BOLD, 23));
        for (JButton b : new JButton[]{btnInicio, btnProductos, btnMovimientos, btnReportes}) {
            b.setForeground(Color.WHITE); b.setBackground(navy); b.setBorderPainted(false);
            b.setFocusPainted(false); b.setHorizontalAlignment(SwingConstants.LEFT);
        }
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
        setSize(1180, 740); setMinimumSize(new java.awt.Dimension(1000, 650)); setLocationRelativeTo(null);
    }

    private void mostrarSeccion(String seccion) {
        seccionActual = seccion;
        titleLabel.setText(seccion.equals("Inicio") ? "Panel principal" : seccion);
        boolean inicio = seccion.equals("Inicio");
        cardProductos.setVisible(inicio); cardReposicion.setVisible(inicio); cardMovimientos.setVisible(inicio);
        if (inicio) {
            infoLabel.setText("Inventario por sede · " + (user == null ? "D-ACERO" : "Sesión: " + user.name()));
            actualizarMetricas();
            cargarTabla(new String[]{"Módulo", "Descripción", "Estado"}, new Object[][]{
                {"Productos", "Catálogo y existencias por sede", "Disponible"},
                {"Movimientos", "Entradas, salidas y ajustes", "Disponible"},
                {"Reportes", "Existencias, bajo mínimo y movimientos", "JasperReports"}});
        } else if (seccion.equals("Productos")) {
            infoLabel.setText("Catálogo, categoría, stock mínimo y saldo por sede");
            cargarProductos();
        } else if (seccion.equals("Movimientos")) {
            infoLabel.setText("Historial de entradas y salidas registrado en MySQL");
            cargarMovimientos();
        } else {
            infoLabel.setText("Consulta los datos y exporta un reporte PDF con JasperReports");
            reportSelector.setVisible(true);
            cargarReporteSeleccionado();
        }
        if (!seccion.equals("Consultas y reportes")) reportSelector.setVisible(false);
        boolean listado = !inicio;
        dataScroll.setVisible(true);
        btnNuevo.setText(seccion.equals("Movimientos") ? "Registrar mov." : "Nuevo producto");
        btnNuevo.setVisible(user == null || user.canWrite()
                ? seccion.equals("Productos") || seccion.equals("Movimientos")
                : false);
        btnEditar.setVisible(seccion.equals("Productos") && (user == null || user.canWrite()));
        btnActualizar.setVisible(listado);
        btnExportar.setVisible(seccion.equals("Consultas y reportes"));
        mainPanel.revalidate(); mainPanel.repaint();
    }

    private void actualizarMetricas() {
        try {
            InventoryRepository.Metrics m = InventoryRepository.metrics();
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
                InventoryRepository.products().toArray(Object[][]::new));
        } catch (SQLException ex) { mostrarError(ex); }
    }

    private void cargarMovimientos() {
        try {
            cargarTabla(new String[]{"ID", "Fecha", "Tipo", "Sede", "Código", "Producto",
                "Cantidad", "Usuario", "Referencia", "Estado"},
                InventoryRepository.movements().toArray(Object[][]::new));
        } catch (SQLException ex) { mostrarError(ex); }
    }

    private void cargarReporteExistencias() {
        try {
            cargarTabla(new String[]{"Sede", "Código", "Producto", "Categoría", "Unidad",
                "Existencia", "Mínimo", "Reponer"},
                InventoryRepository.inventory().toArray(Object[][]::new));
        } catch (SQLException ex) { mostrarError(ex); }
    }

    private void cargarReporteSeleccionado() {
    if (!"Consultas y reportes".equals(seccionActual)) return;

    int selected = reportSelector.getSelectedIndex();

    try {
        if (selected == 1) {
            cargarTabla(
                new String[]{"Sede", "Código", "Producto", "Existencia", "Stock mínimo"},
                InventoryRepository.lowStock().toArray(Object[][]::new)
            );
        } else if (selected == 2) {
            cargarMovimientos();
        } else {
            cargarReporteExistencias();
        }
    } catch (SQLException | RuntimeException ex) {
        mostrarError(ex);
    }
}

    private void refrescarSeccion() {
        if ("Inicio".equals(seccionActual)) mostrarSeccion("Inicio");
        else if ("Productos".equals(seccionActual)) cargarProductos();
        else if ("Movimientos".equals(seccionActual)) cargarMovimientos();
        else cargarReporteSeleccionado();
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
        Object[] values = new Object[ dataTable.getColumnCount() ];
        for (int i = 0; i < values.length; i++) values[i] = dataTable.getValueAt(row, i);
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
            InventoryRepository.saveProduct(id, code.getText(), name.getText(),
                    category.getText(), unit.getText(), stock, active.isSelected());
            cargarProductos();
            JOptionPane.showMessageDialog(this, "Producto guardado correctamente.",
                    "Productos", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException | IllegalArgumentException ex) {
            mostrarError(ex);
        }
    }

    private void registrarMovimiento() {
        try {
            List<InventoryRepository.Option> sites = InventoryRepository.sites();
            List<InventoryRepository.Option> products = InventoryRepository.activeProducts();
            if (sites.isEmpty() || products.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Primero crea al menos una sede y un producto activo.",
                        "Registrar movimiento", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JComboBox<InventoryRepository.Option> site = new JComboBox<>(sites.toArray(InventoryRepository.Option[]::new));
            JComboBox<String> type = new JComboBox<>(new String[]{"ENTRADA", "SALIDA", "AJUSTE"});
            JComboBox<String> direction = new JComboBox<>(new String[]{"ENTRADA", "SALIDA"});
            JComboBox<InventoryRepository.Option> product = new JComboBox<>(products.toArray(InventoryRepository.Option[]::new));
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

            InventoryRepository.Option selectedSite = (InventoryRepository.Option) site.getSelectedItem();
            InventoryRepository.Option selectedProduct = (InventoryRepository.Option) product.getSelectedItem();
            String movementType = String.valueOf(type.getSelectedItem());
            String adjustment = "AJUSTE".equals(movementType)
                    ? String.valueOf(direction.getSelectedItem()) : null;
            BigDecimal qty = new BigDecimal(quantity.getText().trim());
            BigDecimal unitCost = cost.getText().isBlank() ? null : new BigDecimal(cost.getText().trim());
            if (qty.signum() <= 0 || (unitCost != null && unitCost.signum() < 0))
                throw new IllegalArgumentException("Cantidad y costo deben ser valores válidos.");
            InventoryRepository.registerMovement(selectedSite.id(), movementType, adjustment,
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

    private void exportarReporte() {
        JasperReportsService.Report report = switch (reportSelector.getSelectedIndex()) {
            case 1 -> JasperReportsService.Report.STOCK_BAJO;
            case 2 -> JasperReportsService.Report.MOVIMIENTOS;
            default -> JasperReportsService.Report.EXISTENCIAS;
        };
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(report.fileName()));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File destination = chooser.getSelectedFile();
        if (!destination.getName().toLowerCase().endsWith(".pdf"))
            destination = new File(destination.getParentFile(), destination.getName() + ".pdf");
        try {
            JasperReportsService.exportPdf(report, destination);
            JOptionPane.showMessageDialog(this, "Reporte guardado en:\n" + destination.getAbsolutePath(),
                    "Reporte generado", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            mostrarError(ex);
        }
    }

    private void mostrarError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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

