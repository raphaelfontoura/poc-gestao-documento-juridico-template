package br.com.exemplo.documentos;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.springframework.stereotype.Component;

@Component
public class MetadataExtractor {
    public record Extraction(boolean markerFound, boolean tableFound, Map<String, String> rawFields) {}

    public Extraction extract(byte[] docx) throws IOException {
        try (var document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            List<IBodyElement> elements = document.getBodyElements();
            boolean markerFound = false;
            for (IBodyElement element : elements) {
                if (!markerFound) {
                    if (element instanceof XWPFParagraph paragraph
                        && paragraph.getText().trim().equals("METADADOS_POC_V1")) {
                        markerFound = true;
                    }
                } else if (element instanceof XWPFTable table) {
                    return new Extraction(true, true, readTable(table));
                } else if (element instanceof XWPFParagraph paragraph && !paragraph.getText().isBlank()) {
                    break;
                }
            }
            return new Extraction(markerFound, false, Map.of());
        }
    }

    private Map<String, String> readTable(XWPFTable table) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (var row : table.getRows()) {
            if (row.getTableCells().size() != 2) continue;
            String key = cellText(row.getCell(0)).trim();
            if (key.equalsIgnoreCase("Chave")) continue;
            fields.put(key, cellText(row.getCell(1)).trim());
        }
        return fields;
    }

    private String cellText(XWPFTableCell cell) {
        List<String> lines = new ArrayList<>();
        for (XWPFParagraph paragraph : cell.getParagraphs()) {
            String text = paragraph.getText();
            if (!text.isBlank()) lines.add(text.trim());
        }
        return String.join("\n", lines);
    }

    public Map<String, Object> typedFields(Extraction extraction, List<TemplateCatalog.Field> definitions) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (TemplateCatalog.Field field : definitions) {
            String value = extraction.rawFields().getOrDefault(field.key(), "").trim();
            if (field.repeatable()) {
                result.put(field.key(), value.lines().map(String::trim).filter(line -> !line.isEmpty()).toList());
            } else {
                result.put(field.key(), value);
            }
        }
        return result;
    }
}
