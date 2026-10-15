package br.com.exemplo.documentos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class MetadataExtractorTest {
    private final MetadataExtractor extractor = new MetadataExtractor();

    @Test
    void readsParecerScalarsAndPeopleAsSeparateItems() throws Exception {
        byte[] docx = filled("parecer", Map.of(
            "interessados", List.of("Ana Exemplo", "Bruno Exemplo"),
            "assunto", List.of("Organização de arquivo"),
            "ementa", List.of("Análise fictícia"),
            "data", List.of("14/10/2026"),
            "procuradores_assinantes", List.of("Clara Exemplo", "Davi Exemplo")));
        var extraction = extractor.extract(docx);
        assertTrue(extraction.markerFound());
        assertTrue(extraction.tableFound());
        var fields = extractor.typedFields(extraction, List.of(
            new TemplateCatalog.Field("interessados", "Interessados", true, true),
            new TemplateCatalog.Field("assunto", "Assunto", true, false),
            new TemplateCatalog.Field("ementa", "Ementa", true, false),
            new TemplateCatalog.Field("data", "Data", true, false),
            new TemplateCatalog.Field("procuradores_assinantes", "Procuradores assinantes", true, true)));
        assertEquals(List.of("Ana Exemplo", "Bruno Exemplo"), fields.get("interessados"));
        assertEquals("Organização de arquivo", fields.get("assunto"));
        assertEquals(List.of("Clara Exemplo", "Davi Exemplo"), fields.get("procuradores_assinantes"));
    }

    @Test
    void readsPortariaWithoutParecerOnlyFields() throws Exception {
        byte[] docx = filled("portaria", Map.of(
            "assunto", List.of("Organização fictícia"),
            "data", List.of("14/10/2026"),
            "signatarios", List.of("Elaine Exemplo", "Fabio Exemplo")));
        var extraction = extractor.extract(docx);
        var fields = extractor.typedFields(extraction, List.of(
            new TemplateCatalog.Field("assunto", "Assunto", true, false),
            new TemplateCatalog.Field("data", "Data", true, false),
            new TemplateCatalog.Field("signatarios", "Signatários", true, true)));
        assertEquals(List.of("Elaine Exemplo", "Fabio Exemplo"), fields.get("signatarios"));
        assertEquals(3, fields.size());
    }

    @Test
    void readsRealGoogleDocsExport() throws Exception {
        byte[] docx;
        try (var input = new ClassPathResource("google-docs/documento-test.docx").getInputStream()) {
            docx = input.readAllBytes();
        }
        var definitions = List.of(
            new TemplateCatalog.Field("interessados", "Interessados", true, true),
            new TemplateCatalog.Field("assunto", "Assunto", true, false),
            new TemplateCatalog.Field("ementa", "Ementa", true, false),
            new TemplateCatalog.Field("data", "Data", true, false),
            new TemplateCatalog.Field("procuradores_assinantes", "Procuradores assinantes", true, true));
        var extraction = extractor.extract(docx);
        assertTrue(extraction.markerFound());
        assertTrue(extraction.tableFound());
        assertEquals(5, extraction.rawFields().size());
        var fields = extractor.typedFields(extraction, definitions);
        assertTrue(((List<?>) fields.get("interessados")).size() >= 1);
        assertTrue(((List<?>) fields.get("procuradores_assinantes")).size() >= 1);
        assertTrue(new DocumentValidator(extractor).validate("documento-test.docx", docx, definitions).valid());
    }

    private byte[] filled(String type, Map<String, List<String>> values) throws Exception {
        try (var input = new ClassPathResource("templates/" + type + ".docx").getInputStream();
             var document = new XWPFDocument(input);
             var output = new ByteArrayOutputStream()) {
            var table = document.getTables().get(0);
            for (int index = 1; index < table.getNumberOfRows(); index++) {
                String key = table.getRow(index).getCell(0).getText();
                List<String> lines = values.get(key);
                XWPFTableCell cell = table.getRow(index).getCell(1);
                cell.getParagraphs().get(0).getRuns().get(0).setText(lines.get(0), 0);
                for (int line = 1; line < lines.size(); line++) {
                    cell.addParagraph().createRun().setText(lines.get(line));
                }
            }
            document.write(output);
            return output.toByteArray();
        }
    }
}
