package br.com.exemplo.documentos;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DocumentValidator {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu")
        .withResolverStyle(ResolverStyle.STRICT);

    public record Result(Map<String, Object> fields, Map<String, String> errors) {
        public boolean valid() { return errors.isEmpty(); }
    }

    private final MetadataExtractor extractor;

    public DocumentValidator(MetadataExtractor extractor) {
        this.extractor = extractor;
    }

    public Result validate(String filename, byte[] bytes, List<TemplateCatalog.Field> definitions) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (filename == null || !filename.toLowerCase().endsWith(".docx")) {
            errors.put("arquivo", "Envie um arquivo DOCX. Exporte o documento como Microsoft Word (.docx).");
            return new Result(Map.of(), errors);
        }
        if (bytes == null || bytes.length == 0) {
            errors.put("arquivo", "O arquivo DOCX está vazio.");
            return new Result(Map.of(), errors);
        }
        if (bytes.length > MAX_BYTES) {
            errors.put("arquivo", "O DOCX excede o limite de 5 MB.");
            return new Result(Map.of(), errors);
        }
        if (bytes.length < 4 || bytes[0] != 'P' || bytes[1] != 'K') {
            errors.put("arquivo", "DOCX inválido ou corrompido. Exporte novamente como DOCX.");
            return new Result(Map.of(), errors);
        }

        MetadataExtractor.Extraction extraction;
        try {
            extraction = extractor.extract(bytes);
        } catch (IOException | RuntimeException exception) {
            errors.put("arquivo", "DOCX inválido ou corrompido. Exporte novamente como DOCX.");
            return new Result(Map.of(), errors);
        }
        if (!extraction.markerFound() || !extraction.tableFound()) {
            errors.put("estrutura", "Tabela METADADOS_POC_V1 ausente ou alterada.");
            return new Result(Map.of(), errors);
        }

        Map<String, Object> fields = new LinkedHashMap<>(extractor.typedFields(extraction, definitions));
        for (TemplateCatalog.Field definition : definitions) {
            if (!extraction.rawFields().containsKey(definition.key())) {
                errors.put(definition.key(), "Chave ausente na tabela de metadados.");
                continue;
            }
            Object value = fields.get(definition.key());
            boolean empty = value instanceof String text ? text.isBlank() || text.startsWith("[")
                : ((List<?>) value).isEmpty() || ((List<?>) value).stream().anyMatch(item -> item.toString().startsWith("["));
            if (definition.required() && empty) {
                errors.put(definition.key(), "Campo obrigatório não preenchido.");
                continue;
            }
            if (definition.key().equals("data") && !empty) {
                try {
                    fields.put("data", LocalDate.parse((String) value, DATE).toString());
                } catch (DateTimeParseException exception) {
                    errors.put("data", "Use uma data válida no formato dd/MM/aaaa.");
                }
            }
        }
        return new Result(fields, errors);
    }
}
