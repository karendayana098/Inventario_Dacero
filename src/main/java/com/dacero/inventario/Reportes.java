package com.dacero.inventario;

import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.util.HashMap;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

public final class Reportes {
    public enum Report {
        EXISTENCIAS("/reports/existencias.jrxml", "Reporte_existencias.pdf"),
        STOCK_BAJO("/reports/stock_bajo.jrxml", "Reporte_stock_bajo.pdf"),
        MOVIMIENTOS("/reports/movimientos.jrxml", "Reporte_movimientos.pdf");

        private final String resource;
        private final String fileName;

        Report(String resource, String fileName) {
            this.resource = resource;
            this.fileName = fileName;
        }

        String resource() { return resource; }
        public String fileName() { return fileName; }
    }

    private Reportes() {
    }

    public static void exportPdf(Report report, File destination) throws Exception {
        try (InputStream template = Reportes.class.getResourceAsStream(report.resource());
             Connection connection = Db.connect()) {
            if (template == null) {
                throw new IllegalStateException("No se encontró la plantilla " + report.resource());
            }
            JasperReport compiled = JasperCompileManager.compileReport(template);
            JasperPrint filled = JasperFillManager.fillReport(compiled, new HashMap<>(), connection);
            JasperExportManager.exportReportToPdfFile(filled, destination.getAbsolutePath());
        }
    }
}
