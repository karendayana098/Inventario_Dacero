package com.dacero.inventario;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.JTable;
import javax.swing.table.TableModel;

/** Writes a formatted Excel workbook with a native filterable table. */
public final class ExcelTableExporter {
    private ExcelTableExporter() { }

    public static void export(JTable table, Path destination, String sheetName) throws IOException {
        TableModel model = table.getModel();
        List<String> headers = uniqueHeaders(table);
        int viewRows = table.getRowCount();
        int dataRows = Math.max(viewRows, 1); // Excel tables need a body row when the list is empty.
        int lastRow = dataRows + 1;
        String lastColumn = columnName(headers.size());
        String tableRef = "A1:" + lastColumn + lastRow;

        try (OutputStream file = Files.newOutputStream(destination);
             ZipOutputStream zip = new ZipOutputStream(file, StandardCharsets.UTF_8)) {
            add(zip, "[Content_Types].xml", contentTypes());
            add(zip, "_rels/.rels", rootRelationships());
            add(zip, "xl/workbook.xml", workbookXml(safeSheetName(sheetName)));
            add(zip, "xl/_rels/workbook.xml.rels", workbookRelationships());
            add(zip, "xl/styles.xml", stylesXml());
            add(zip, "xl/worksheets/sheet1.xml",
                    sheetXml(table, headers, dataRows, lastColumn, tableRef, model));
            add(zip, "xl/worksheets/_rels/sheet1.xml.rels", sheetRelationships());
            add(zip, "xl/tables/table1.xml", tableXml(headers, tableRef));
        }
    }

    private static List<String> uniqueHeaders(JTable table) {
        List<String> result = new ArrayList<>();
        Set<String> used = new HashSet<>();
        for (int c = 0; c < table.getColumnCount(); c++) {
            String base = table.getColumnName(c).isBlank() ? "Columna " + (c + 1) : table.getColumnName(c);
            String candidate = base;
            int suffix = 2;
            while (!used.add(candidate.toLowerCase(java.util.Locale.ROOT))) candidate = base + " " + suffix++;
            result.add(candidate);
        }
        return result;
    }

    private static String sheetXml(JTable table, List<String> headers, int dataRows,
            String lastColumn, String tableRef, TableModel model) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
                .append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">")
                .append("<sheetViews><sheetView workbookViewId=\"0\" showGridLines=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>")
                .append("<sheetFormatPr defaultRowHeight=\"24\"/><cols>");
        for (int c = 0; c < headers.size(); c++) {
            int width = Math.max(14, Math.min(42, headers.get(c).length() + 5));
            xml.append("<col min=\"").append(c + 1).append("\" max=\"").append(c + 1)
                    .append("\" width=\"").append(width).append("\" customWidth=\"1\"/>");
        }
        xml.append("</cols><sheetData><row r=\"1\" ht=\"28\" customHeight=\"1\">");
        for (int c = 0; c < headers.size(); c++) appendTextCell(xml, columnName(c + 1) + "1", headers.get(c), 1);
        xml.append("</row>");
        for (int r = 0; r < dataRows; r++) {
            xml.append("<row r=\"").append(r + 2).append("\">");
            boolean hasSourceRow = r < table.getRowCount();
            for (int c = 0; c < headers.size(); c++) {
                String ref = columnName(c + 1) + (r + 2);
                Object value = hasSourceRow
                        ? table.getValueAt(r, c)
                        : "";
                // Render every table cell as displayed in the application, except real numbers remain numeric.
                int modelColumn = hasSourceRow ? table.convertColumnIndexToModel(c) : c;
                Object raw = hasSourceRow
                        ? model.getValueAt(table.convertRowIndexToModel(r), modelColumn) : "";
                if (raw instanceof Number number) appendNumberCell(xml, ref, number.toString());
                else appendTextCell(xml, ref, value == null ? "" : value.toString(), 0);
            }
            xml.append("</row>");
        }
        return xml.append("</sheetData><tableParts count=\"1\"><tablePart r:id=\"rId1\"/></tableParts></worksheet>").toString();
    }

    private static void appendTextCell(StringBuilder xml, String ref, String value, int style) {
        xml.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"");
        if (style > 0) xml.append(" s=\"").append(style).append("\"");
        xml.append("><is><t xml:space=\"preserve\">").append(escape(value)).append("</t></is></c>");
    }

    private static void appendNumberCell(StringBuilder xml, String ref, String value) {
        xml.append("<c r=\"").append(ref).append("\" s=\"2\"><v>")
                .append(escape(value)).append("</v></c>");
    }

    private static String tableXml(List<String> headers, String ref) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                .append("<table xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" id=\"1\" name=\"InventarioExportado\" displayName=\"InventarioExportado\" ref=\"")
                .append(ref).append("\" totalsRowShown=\"0\"><autoFilter ref=\"").append(ref)
                .append("\"/><tableColumns count=\"").append(headers.size()).append("\">");
        for (int c = 0; c < headers.size(); c++) {
            xml.append("<tableColumn id=\"").append(c + 1).append("\" name=\"")
                    .append(escape(headers.get(c))).append("\"/>");
        }
        return xml.append("</tableColumns><tableStyleInfo name=\"TableStyleMedium2\" showFirstColumn=\"0\" showLastColumn=\"0\" showRowStripes=\"1\" showColumnStripes=\"0\"/></table>").toString();
    }

    private static String stylesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<numFmts count=\"1\"><numFmt numFmtId=\"164\" formatCode=\"#,##0.##\"/></numFmts>"
                + "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Aptos\"/></font><font><b/><color rgb=\"FFFFFFFF\"/><sz val=\"11\"/><name val=\"Aptos\"/></font></fonts>"
                + "<fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF17304A\"/><bgColor indexed=\"64\"/></patternFill></fill></fills>"
                + "<borders count=\"2\"><border><left/><right/><top/><bottom/><diagonal/></border><border><left style=\"thin\"><color rgb=\"FFE0E7ED\"/></left><right style=\"thin\"><color rgb=\"FFE0E7ED\"/></right><top style=\"thin\"><color rgb=\"FFE0E7ED\"/></top><bottom style=\"thin\"><color rgb=\"FFE0E7ED\"/></bottom><diagonal/></border></borders>"
                + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                + "<cellXfs count=\"3\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\"/><xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\"/></xf><xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\"/></cellXfs>"
                + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles></styleSheet>";
    }

    private static String contentTypes() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/><Override PartName=\"/xl/tables/table1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.table+xml\"/></Types>";
    }

    private static String rootRelationships() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>";
    }

    private static String workbookXml(String sheetName) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"" + escape(sheetName) + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>";
    }

    private static String workbookRelationships() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/><Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>";
    }

    private static String sheetRelationships() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/table\" Target=\"../tables/table1.xml\"/></Relationships>";
    }

    private static String safeSheetName(String input) {
        String cleaned = (input == null ? "Inventario" : input).replaceAll("[\\[\\]:*?/\\\\]", " ").trim();
        if (cleaned.isEmpty()) cleaned = "Inventario";
        return cleaned.substring(0, Math.min(31, cleaned.length()));
    }

    private static String columnName(int number) {
        StringBuilder name = new StringBuilder();
        while (number > 0) {
            int remainder = (number - 1) % 26;
            name.insert(0, (char) ('A' + remainder));
            number = (number - 1) / 26;
        }
        return name.toString();
    }

    private static String escape(String value) {
        if (value == null) return "";
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '&') escaped.append("&amp;");
            else if (ch == '<') escaped.append("&lt;");
            else if (ch == '>') escaped.append("&gt;");
            else if (ch == '\"') escaped.append("&quot;");
            else if (ch == '\'') escaped.append("&apos;");
            else if (ch == '\t' || ch == '\n' || ch == '\r' || ch >= 0x20) escaped.append(ch);
        }
        return escaped.toString();
    }

    private static void add(ZipOutputStream zip, String name, String text) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(text.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
