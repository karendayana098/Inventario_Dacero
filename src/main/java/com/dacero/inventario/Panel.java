package com.dacero.inventario;

import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
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
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** JFrame Form: abrir Panel.java y elegir Design para editar la interfaz. */
public class Panel extends JFrame {
    private final Color navy = new Color(23, 48, 74);
    private final Color teal = new Color(0, 139, 139);
    private final Color pageBackground = new Color(243, 246, 249);
    private final Color mutedText = new Color(96, 112, 128);
    private final App user;
    private final JButton btnSedes = new JButton("Sedes");
    private final JButton btnCategorias = new JButton("Categorías");
    private final JButton btnUsuarios = new JButton("Usuarios");
    private final JButton btnSalir = new JButton("Salir");
    private JPanel statsCards;
    private JPanel tableCard;
    private JLabel tableHeading;
    private JLabel rowCountLabel;
    private final JTextField searchField = new JTextField(24);
    private TableRowSorter<TableModel> tableSorter;
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
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filtrarTabla(); }
            @Override public void removeUpdate(DocumentEvent e) { filtrarTabla(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrarTabla(); }
        });
        mostrarSeccion("Inicio");
    }

    private void configurarEstilo() {
        getContentPane().setBackground(Color.WHITE);
        sidebarPanel.setBackground(navy);
        brandLabel.setForeground(Color.WHITE);
        brandLabel.setFont(new Font("Segoe UI", Font.BOLD, 23));
        for (JButton b : new JButton[]{btnInicio, btnProductos, btnMovimientos, btnReportes,
                btnSedes, btnCategorias, btnUsuarios, btnSalir}) estiloBotonLateral(b);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 30));
        titleLabel.setForeground(navy);
        for (JPanel card : new JPanel[]{cardProductos, cardReposicion, cardMovimientos}) {
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(224, 231, 237)),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        }
        for (JLabel value : new JLabel[]{lblProductos, lblReposicion, lblMovimientos}) {
            value.setFont(new Font("Segoe UI", Font.BOLD, 27)); value.setForeground(teal);
        }
        for (JLabel label : new JLabel[]{capProductos, capReposicion, capMovimientos}) label.setForeground(new Color(70,82,95));
        infoLabel.setForeground(mutedText);
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        dataTable.setRowHeight(34);
        dataTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        dataTable.setSelectionBackground(new Color(218, 240, 238));
        dataTable.setSelectionForeground(navy);
        dataTable.setShowGrid(false);
        dataTable.setIntercellSpacing(new Dimension(0, 1));
        dataTable.setFillsViewportHeight(true);
        dataTable.setAutoCreateRowSorter(true);
        dataTable.getTableHeader().setBackground(new Color(235, 241, 245));
        dataTable.getTableHeader().setForeground(navy);
        dataTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        dataTable.getTableHeader().setPreferredSize(new Dimension(0, 38));
        dataTable.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer bodyRenderer = new DefaultTableCellRenderer() {
            @Override public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                    boolean selected, boolean focused, int row, int column) {
                java.awt.Component component = super.getTableCellRendererComponent(
                        table, value, selected, focused, row, column);
                if (!selected) component.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return component;
            }
        };
        dataTable.setDefaultRenderer(Object.class, bodyRenderer);
        dataTable.setDefaultRenderer(Number.class, bodyRenderer);
        for (JButton b : new JButton[]{btnNuevo, btnEditar, btnActualizar, btnExportar}) {
            b.setBackground(teal); b.setForeground(Color.WHITE); b.setFocusPainted(false);
            b.setFont(new Font("Segoe UI", Font.BOLD, 13));
            b.setBorder(BorderFactory.createEmptyBorder(9, 15, 9, 15));
            b.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        }
        setTitle("D-ACERO | Gestión de inventario");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 650));
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
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setMargin(new Insets(0, 14, 0, 8));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        button.setPreferredSize(new Dimension(210, 46));
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    }

    private void construirInterfaz() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        sidebarPanel.removeAll();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setBorder(BorderFactory.createEmptyBorder(24, 15, 18, 15));
        sidebarPanel.setPreferredSize(new Dimension(246, 0));
        brandLabel.setAlignmentX(LEFT_ALIGNMENT);
        brandLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        sidebarPanel.add(brandLabel);
        JLabel subtitle = new JLabel("INVENTARIO DE FERRETERÍA");
        subtitle.setForeground(new Color(174, 195, 211));
        subtitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        subtitle.setAlignmentX(LEFT_ALIGNMENT);
        sidebarPanel.add(Box.createVerticalStrut(3));
        sidebarPanel.add(subtitle);
        sidebarPanel.add(Box.createVerticalStrut(28));
        agregarGrupoNavegacion("GENERAL", btnInicio, btnProductos);
        agregarGrupoNavegacion("OPERACIÓN", btnMovimientos, btnSedes, btnCategorias);
        if (user != null && user.isAdmin()) {
            agregarGrupoNavegacion("ADMINISTRACIÓN", btnUsuarios);
        }
        sidebarPanel.add(Box.createVerticalGlue());
        JLabel session = new JLabel(user == null ? "Sesión de demostración"
                : user.name() + " · " + user.roleLabel());
        session.setForeground(new Color(211, 222, 231));
        session.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        session.setAlignmentX(LEFT_ALIGNMENT);
        sidebarPanel.add(session);
        sidebarPanel.add(Box.createVerticalStrut(8));
        btnSalir.setAlignmentX(LEFT_ALIGNMENT);
        sidebarPanel.add(btnSalir);

        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 7));
        heading.setOpaque(false);
        heading.add(titleLabel);
        heading.add(infoLabel);
        statsCards = new JPanel(new GridLayout(1, 3, 14, 0));
        statsCards.setOpaque(false);
        for (JPanel card : new JPanel[]{cardProductos, cardReposicion, cardMovimientos}) {
            card.setPreferredSize(new Dimension(260, 94));
            statsCards.add(card);
        }
        JPanel header = new JPanel(new BorderLayout(0, 20));
        header.setOpaque(false);
        header.add(heading, BorderLayout.NORTH);
        header.add(statsCards, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 12));
        actions.setOpaque(false);
        actions.add(btnNuevo);
        actions.add(btnEditar);
        actions.add(btnActualizar);
        actions.add(btnExportar);

        mainPanel.removeAll();
        mainPanel.setLayout(new BorderLayout(0, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 34, 20, 34));
        mainPanel.setBackground(pageBackground);
        mainPanel.add(header, BorderLayout.NORTH);
        tableCard = new JPanel(new BorderLayout(0, 0));
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(BorderFactory.createLineBorder(new Color(225, 232, 238)));
        JPanel tableTop = new JPanel(new BorderLayout(16, 8));
        tableTop.setBackground(Color.WHITE);
        tableTop.setBorder(BorderFactory.createEmptyBorder(16, 18, 14, 18));
        tableHeading = new JLabel("Registros");
        tableHeading.setFont(new Font("Segoe UI", Font.BOLD, 17));
        tableHeading.setForeground(navy);
        JPanel searchBox = new JPanel(new BorderLayout(8, 0));
        searchBox.setOpaque(false);
        JLabel searchLabel = new JLabel("Buscar:");
        searchLabel.setForeground(mutedText);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setToolTipText("Busca por cualquier dato visible en la tabla");
        searchField.setPreferredSize(new Dimension(260, 36));
        searchBox.add(searchLabel, BorderLayout.WEST);
        searchBox.add(searchField, BorderLayout.CENTER);
        rowCountLabel = new JLabel("0 registros");
        rowCountLabel.setForeground(mutedText);
        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        topRight.setOpaque(false);
        topRight.add(rowCountLabel);
        topRight.add(searchBox);
        tableTop.add(tableHeading, BorderLayout.WEST);
        tableTop.add(topRight, BorderLayout.EAST);
        dataScroll.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(232, 237, 241)));
        tableCard.add(tableTop, BorderLayout.NORTH);
        tableCard.add(dataScroll, BorderLayout.CENTER);
        tableCard.add(actions, BorderLayout.SOUTH);
        mainPanel.add(tableCard, BorderLayout.CENTER);

        root.add(sidebarPanel, BorderLayout.WEST);
        root.add(mainPanel, BorderLayout.CENTER);
        setContentPane(root);
        revalidate();
    }

    private void agregarGrupoNavegacion(String label, JButton... buttons) {
        JLabel heading = new JLabel(label);
        heading.setForeground(new Color(153, 176, 193));
        heading.setFont(new Font("Segoe UI", Font.BOLD, 10));
        heading.setAlignmentX(LEFT_ALIGNMENT);
        sidebarPanel.add(heading);
        sidebarPanel.add(Box.createVerticalStrut(8));
        for (JButton button : buttons) {
            button.setAlignmentX(LEFT_ALIGNMENT);
            sidebarPanel.add(button);
            sidebarPanel.add(Box.createVerticalStrut(5));
        }
        sidebarPanel.add(Box.createVerticalStrut(14));
    }

    private void mostrarSeccion(String seccion) {
        seccionActual = seccion;
        searchField.setText("");
        titleLabel.setText(seccion.equals("Inicio") ? "Panel principal" : seccion);
        tableHeading.setText(switch (seccion) {
            case "Inicio" -> "Accesos rápidos";
            case "Productos" -> "Catálogo de productos";
            case "Movimientos" -> "Historial de movimientos";
            case "Sedes" -> "Sedes registradas";
            case "Categorías" -> "Categorías de productos";
            default -> "Registros";
        });
        actualizarNavegacion(seccion);
        boolean inicio = seccion.equals("Inicio");
        cardProductos.setVisible(inicio); cardReposicion.setVisible(inicio); cardMovimientos.setVisible(inicio);
        statsCards.setVisible(inicio);
        if (inicio) {
            infoLabel.setText("Resumen de la operación y accesos a las tareas de inventario");
            actualizarMetricas();
            cargarTabla(new String[]{"Tarea", "Cómo usar el sistema"}, new Object[][]{
                {"1. Configurar sedes", "Crea las sucursales o bodegas donde guardarás productos."},
                {"2. Organizar categorías", "Agrupa los artículos, por ejemplo: acero, herramientas o pinturas."},
                {"3. Crear el catálogo", "Registra cada producto con su código, unidad y stock mínimo."},
                {"4. Agregar existencias", "Movimientos > Registrar movimiento > Entrada de mercancía suma stock a una sede."},
                {"5. Registrar salidas", "Usa Salida de mercancía para descontar ventas o materiales entregados."}});
        } else if (seccion.equals("Productos")) {
            infoLabel.setText("Consulta códigos, unidades y existencias registradas por sede");
            cargarProductos();
        } else if (seccion.equals("Movimientos")) {
            infoLabel.setText("Registra ingresos, salidas y ajustes para mantener actualizado el stock");
            cargarMovimientos();
        } else if (seccion.equals("Sedes")) {
            infoLabel.setText("Organiza los puntos de almacenamiento y venta de la ferretería");
            cargarSedes();
        } else if (seccion.equals("Categorías")) {
            infoLabel.setText("Agrupa los artículos por tipo de material o uso");
            cargarCategorias();
        } else {
            mostrarSeccion("Inicio");
            return;
        }
        boolean listado = !inicio;
        dataScroll.setVisible(true);
        btnNuevo.setText(seccion.equals("Movimientos") ? "Registrar movimiento"
                : seccion.equals("Sedes") ? "Nueva sede"
                : seccion.equals("Categorías") ? "Nueva categoría" : "Nuevo producto");
        boolean canManageCatalog = user == null || user.canManageCatalog();
        boolean canManageConfiguration = user == null || user.canManageConfiguration();
        boolean canRegisterMovements = user == null || user.canWrite() || user.isSeller();
        btnNuevo.setVisible((seccion.equals("Productos") && canManageCatalog)
                || (seccion.equals("Movimientos") && canRegisterMovements)
                || ((seccion.equals("Sedes") || seccion.equals("Categorías")) && canManageConfiguration));
        btnEditar.setVisible((seccion.equals("Productos") && canManageCatalog)
                || ((seccion.equals("Sedes") || seccion.equals("Categorías")) && canManageConfiguration));
        btnEditar.setText("Editar seleccionado");
        btnActualizar.setVisible(listado);
        btnActualizar.setText("Actualizar lista");
        btnExportar.setText("Descargar CSV");
        btnExportar.setVisible(listado);
        actualizarConteo();
        mainPanel.revalidate(); mainPanel.repaint();
    }

    private void actualizarNavegacion(String section) {
        JButton selected = switch (section) {
            case "Productos" -> btnProductos;
            case "Movimientos" -> btnMovimientos;
            case "Sedes" -> btnSedes;
            case "Categorías" -> btnCategorias;
            default -> btnInicio;
        };
        for (JButton button : new JButton[]{btnInicio, btnProductos, btnMovimientos,
                btnSedes, btnCategorias}) {
            boolean active = button == selected;
            button.setBackground(active ? teal : navy);
            button.setForeground(Color.WHITE);
        }
    }

    private void filtrarTabla() {
        if (tableSorter == null) return;
        String query = searchField.getText().trim();
        try {
            tableSorter.setRowFilter(query.isEmpty() ? null
                    : RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(query)));
        } catch (java.util.regex.PatternSyntaxException ignored) {
            tableSorter.setRowFilter(null);
        }
        actualizarConteo();
    }

    private void actualizarConteo() {
        if (rowCountLabel == null) return;
        int visible = dataTable.getRowCount();
        int total = dataTable.getModel().getRowCount();
        rowCountLabel.setText(visible == total ? total + (total == 1 ? " registro" : " registros")
                : visible + " de " + total + " registros");
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
            List<Object[]> rows = Inventario.products();
            for (Object[] row : rows) row[6] = Boolean.TRUE.equals(row[6]) ? "Activo" : "Inactivo";
            cargarTabla(new String[]{"ID", "Código", "Producto", "Categoría", "Unidad",
                "Stock mínimo", "Activo", "Sede", "Existencia"},
                rows.toArray(Object[][]::new));
            ocultarId();
        } catch (SQLException ex) { mostrarError(ex); }
    }

    private void cargarMovimientos() {
        try {
            cargarTabla(new String[]{"ID", "Fecha", "Tipo", "Sede", "Código", "Producto",
                "Cantidad", "Usuario", "Referencia"},
                Inventario.movements().toArray(Object[][]::new));
            ocultarId();
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
                    "Activo".equalsIgnoreCase(String.valueOf(values[6]))
                            || Boolean.parseBoolean(String.valueOf(values[6])));
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "No se pudieron leer los datos del producto seleccionado.",
                    "Editar producto", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editarProducto(Long id, String oldCode, String oldName, String oldCategory,
            String oldUnit, BigDecimal oldMinimum, boolean oldActive) {
        JTextField code = new JTextField(oldCode, 22);
        JTextField name = new JTextField(oldName, 22);
        JComboBox<String> category = new JComboBox<>();
        category.setEditable(true);
        category.addItem("");
        try {
            for (Inventario.Option option : Inventario.categories()) category.addItem(option.label());
        } catch (SQLException ex) { mostrarError(ex); return; }
        category.setSelectedItem("Sin categoría".equals(oldCategory) ? "" : oldCategory);
        JComboBox<String> unit = new JComboBox<>(new String[]{"unidad", "kg", "metro", "litro", "bulto", "rollo", "lámina", "par"});
        unit.setEditable(true);
        unit.setSelectedItem(oldUnit.isBlank() ? "unidad" : oldUnit);
        JTextField minimum = new JTextField(oldMinimum.toPlainString(), 22);
        JCheckBox active = new JCheckBox("Producto activo", oldActive);
        JPanel form = nuevoFormulario();
        agregarCampo(form, "Código del producto *", code);
        agregarCampo(form, "Nombre del producto *", name);
        agregarCampo(form, "Categoría", category);
        agregarCampo(form, "Unidad de venta *", unit);
        agregarCampo(form, "Avisar cuando el stock llegue a", minimum);
        agregarCampo(form, "Estado", active);
        JLabel note = new JLabel("Los campos con * son obligatorios. El stock actual se controla por sede.");
        note.setForeground(mutedText);
        form.add(note, BorderLayout.SOUTH);
        int answer = JOptionPane.showConfirmDialog(this, form,
                id == null ? "Agregar producto al catálogo" : "Editar producto del catálogo",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) return;
        try {
            if (code.getText().isBlank() || name.getText().isBlank()
                    || String.valueOf(unit.getEditor().getItem()).isBlank())
                throw new IllegalArgumentException("Código, nombre y unidad son obligatorios.");
            BigDecimal stock = decimal(minimum.getText(), "El stock mínimo");
            if (stock.signum() < 0) throw new IllegalArgumentException("El stock mínimo no puede ser negativo.");
            Inventario.saveProduct(id, code.getText(), name.getText(),
                    String.valueOf(category.getEditor().getItem()),
                    String.valueOf(unit.getEditor().getItem()), stock, active.isSelected());
            cargarProductos();
            mostrarSeccion("Productos");
            JOptionPane.showMessageDialog(this, "El producto quedó guardado en el catálogo.",
                    "Producto guardado", JOptionPane.INFORMATION_MESSAGE);
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
        JPanel form = nuevoFormulario();
        agregarCampo(form, "Nombre de la sede *", nombre);
        agregarCampo(form, "Municipio *", municipio);
        agregarCampo(form, "Dirección", direccion);
        agregarCampo(form, "Estado", activa);
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
        JPanel form = nuevoFormulario();
        agregarCampo(form, "Nombre de la categoría *", nombre);
        agregarCampo(form, "Descripción", descripcion);
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
        JOptionPane.showMessageDialog(this, "Selecciona primero una " + entity + " de la lista.",
                "Editar", JOptionPane.INFORMATION_MESSAGE);
    }

    private JPanel nuevoFormulario() {
        JPanel form = new JPanel(new BorderLayout(10, 14));
        form.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 12));
        fields.setOpaque(false);
        form.add(fields, BorderLayout.CENTER);
        form.putClientProperty("fields", fields);
        return form;
    }

    private void agregarCampo(JPanel form, String labelText, JComponent input) {
        JPanel fields = (JPanel) form.getClientProperty("fields");
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(navy);
        fields.add(label);
        fields.add(input);
        input.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        input.setPreferredSize(new Dimension(250, 38));
    }

    private BigDecimal decimal(String value, String label) {
        try { return new BigDecimal(value.trim().replace(',', '.')); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException(label + " debe ser un número válido."); }
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
            boolean seller = user != null && user.isSeller();
            JComboBox<String> type = new JComboBox<>(seller
                    ? new String[]{"Salida de mercancía"}
                    : new String[]{"Entrada de mercancía", "Salida de mercancía", "Ajuste de inventario"});
            JComboBox<String> direction = new JComboBox<>(new String[]{
                "Sumar al inventario", "Restar del inventario"});
            JComboBox<Inventario.Option> product = new JComboBox<>(products.toArray(Inventario.Option[]::new));
            JTextField quantity = new JTextField("1", 12);
            JTextField cost = new JTextField("", 12);
            JTextField reference = new JTextField("", 18);
            JTextField note = new JTextField("", 18);
            JPanel form = nuevoFormulario();
            agregarCampo(form, "Sede donde se mueve *", site);
            agregarCampo(form, "Tipo de movimiento *", type);
            if (!seller) agregarCampo(form, "Para ajustes: dirección", direction);
            agregarCampo(form, "Producto *", product);
            agregarCampo(form, "Cantidad *", quantity);
            agregarCampo(form, "Costo por unidad (opcional)", cost);
            agregarCampo(form, "Factura o referencia (opcional)", reference);
            agregarCampo(form, "Observación (opcional)", note);
            JLabel noteHint = new JLabel("Las salidas reducen existencias; no se permiten cantidades negativas.");
            noteHint.setForeground(mutedText);
            form.add(noteHint, BorderLayout.SOUTH);

            if (JOptionPane.showConfirmDialog(this, form, "Registrar movimiento",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

            Inventario.Option selectedSite = (Inventario.Option) site.getSelectedItem();
            Inventario.Option selectedProduct = (Inventario.Option) product.getSelectedItem();
            String movementType = seller ? "SALIDA" : switch (type.getSelectedIndex()) {
                case 1 -> "SALIDA";
                case 2 -> "AJUSTE";
                default -> "ENTRADA";
            };
            String adjustment = "AJUSTE".equals(movementType)
                    ? (direction.getSelectedIndex() == 0 ? "ENTRADA" : "SALIDA") : null;
            BigDecimal qty = decimal(quantity.getText(), "La cantidad");
            BigDecimal unitCost = cost.getText().isBlank() ? null : decimal(cost.getText(), "El costo unitario");
            if (qty.signum() <= 0 || (unitCost != null && unitCost.signum() < 0))
                throw new IllegalArgumentException("Cantidad y costo deben ser valores válidos.");
            Inventario.registerMovement(selectedSite.id(), movementType, adjustment,
                    selectedProduct.id(), qty, user == null ? null : user.id(),
                    reference.getText(), note.getText(), unitCost);
            cargarMovimientos();
            actualizarMetricas();
            JOptionPane.showMessageDialog(this,
                    "Movimiento guardado. Las existencias de la sede se actualizaron.",
                    "Movimiento registrado", JOptionPane.INFORMATION_MESSAGE);
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
        tableSorter = new TableRowSorter<>(dataTable.getModel());
        dataTable.setRowSorter(tableSorter);
        filtrarTabla();
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

