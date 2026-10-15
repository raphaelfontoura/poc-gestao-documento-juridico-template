package br.com.exemplo.documentos;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.function.Consumer;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class DocumentValidatorTest {
    private final DocumentValidator validator = new DocumentValidator(new MetadataExtractor());
    private final List<TemplateCatalog.Field> portaria = List.of(
        new TemplateCatalog.Field("assunto", "Assunto", true, false),
        new TemplateCatalog.Field("data", "Data", true, false),
        new TemplateCatalog.Field("signatarios", "Signatários", true, true));

    @Test
    void rejectsEmptyRequiredFieldAndInvalidDate() throws Exception {
        byte[] docx = edit("portaria", document -> {
            setCell(document, 1, 1, "");
            setCell(document, 2, 1, "31/02/2026");
            setCell(document, 3, 1, "Ana Exemplo");
        });
        var result = validator.validate("portaria.docx", docx, portaria);
        assertFalse(result.valid());
        assertTrue(result.errors().containsKey("assunto"));
        assertTrue(result.errors().containsKey("data"));
    }

    @Test
    void rejectsMissingMarkerAndKey() throws Exception {
        byte[] noMarker = edit("portaria", document -> document.getParagraphs().stream()
            .filter(paragraph -> paragraph.getText().equals("METADADOS_POC_V1"))
            .findFirst().orElseThrow().getRuns().get(0).setText("", 0));
        assertTrue(validator.validate("portaria.docx", noMarker, portaria).errors().containsKey("estrutura"));

        byte[] noKey = edit("portaria", document -> {
            setCell(document, 1, 1, "Organização fictícia");
            setCell(document, 2, 1, "14/10/2026");
            setCell(document, 3, 0, "outra_chave");
            setCell(document, 3, 1, "Ana Exemplo");
        });
        assertTrue(validator.validate("portaria.docx", noKey, portaria).errors().containsKey("signatarios"));
    }

    @Test
    void acceptsPortariaWithoutEmentaAndNormalizesDate() throws Exception {
        byte[] docx = edit("portaria", document -> {
            setCell(document, 1, 1, "Organização fictícia");
            setCell(document, 2, 1, "14/10/2026");
            setCell(document, 3, 1, "Ana Exemplo");
        });
        var result = validator.validate("portaria.docx", docx, portaria);
        assertTrue(result.valid());
        assertEquals("2026-10-14", result.fields().get("data"));
        assertFalse(result.fields().containsKey("ementa"));
    }

    @Test
    void rejectsPdfCorruptAndOversizeWithoutChangingInput() {
        byte[] pdf = "%PDF-example".getBytes();
        byte[] snapshot = pdf.clone();
        assertTrue(validator.validate("arquivo.pdf", pdf, portaria).errors().get("arquivo").contains("DOCX"));
        assertArrayEquals(snapshot, pdf);
        assertTrue(validator.validate("arquivo.docx", new byte[] {'P', 'K', 1, 2}, portaria)
            .errors().get("arquivo").contains("corrompido"));
        assertTrue(validator.validate("arquivo.docx", new byte[DocumentValidator.MAX_BYTES + 1], portaria)
            .errors().get("arquivo").contains("5 MB"));
    }

    private byte[] edit(String type, Consumer<XWPFDocument> change) throws Exception {
        try (var input = new ClassPathResource("templates/" + type + ".docx").getInputStream();
             var document = new XWPFDocument(input);
             var output = new ByteArrayOutputStream()) {
            change.accept(document);
            document.write(output);
            return output.toByteArray();
        }
    }

    private void setCell(XWPFDocument document, int row, int column, String value) {
        document.getTables().get(0).getRow(row).getCell(column)
            .getParagraphs().get(0).getRuns().get(0).setText(value, 0);
    }
}
