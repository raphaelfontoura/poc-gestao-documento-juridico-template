package br.com.exemplo.documentos;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class TemplateCatalog {
    public record Field(String key, String label, boolean required, boolean repeatable) {}
    public record Template(String id, String name, String description, String filename, List<Field> fields) {}
    public record Download(String filename, byte[] bytes) {}

    private final Database database;

    public TemplateCatalog(Database database) {
        this.database = database;
    }

    @PostConstruct
    public void seed() throws SQLException, IOException {
        seedTemplate("parecer", "Parecer", "Análise jurídica fictícia", "parecer.docx", List.of(
            new Field("interessados", "Interessados", true, true),
            new Field("assunto", "Assunto", true, false),
            new Field("ementa", "Ementa", true, false),
            new Field("data", "Data", true, false),
            new Field("procuradores_assinantes", "Procuradores assinantes", true, true)));
        seedTemplate("portaria", "Portaria", "Ato administrativo fictício", "portaria.docx", List.of(
            new Field("assunto", "Assunto", true, false),
            new Field("data", "Data", true, false),
            new Field("signatarios", "Signatários", true, true)));
    }

    private void seedTemplate(String id, String name, String description, String filename, List<Field> fields)
        throws SQLException, IOException {
        byte[] bytes;
        try (var input = new ClassPathResource("templates/" + filename).getInputStream()) {
            bytes = input.readAllBytes();
        }
        try (var connection = database.connect()) {
            try (var insert = connection.prepareStatement(
                "INSERT INTO templates (id, name, description, filename, docx) VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT(id) DO UPDATE SET name = excluded.name, description = excluded.description, " +
                "filename = excluded.filename, docx = excluded.docx")) {
                insert.setString(1, id);
                insert.setString(2, name);
                insert.setString(3, description);
                insert.setString(4, filename);
                insert.setBytes(5, bytes);
                insert.executeUpdate();
            }
            try (var insert = connection.prepareStatement(
                "INSERT OR IGNORE INTO template_fields (template_id, field_key, label, required, repeatable, position) VALUES (?, ?, ?, ?, ?, ?)")) {
                for (int index = 0; index < fields.size(); index++) {
                    Field field = fields.get(index);
                    insert.setString(1, id);
                    insert.setString(2, field.key());
                    insert.setString(3, field.label());
                    insert.setInt(4, field.required() ? 1 : 0);
                    insert.setInt(5, field.repeatable() ? 1 : 0);
                    insert.setInt(6, index);
                    insert.addBatch();
                }
                insert.executeBatch();
            }
        }
    }

    public List<Template> list() throws SQLException {
        List<Template> templates = new ArrayList<>();
        try (var connection = database.connect();
             var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT id, name, description, filename FROM templates ORDER BY name")) {
            while (rows.next()) {
                templates.add(new Template(rows.getString(1), rows.getString(2), rows.getString(3),
                    rows.getString(4), fields(rows.getString(1))));
            }
        }
        return templates;
    }

    public Template find(String id) throws SQLException {
        return list().stream().filter(template -> template.id().equals(id)).findFirst().orElse(null);
    }

    private List<Field> fields(String templateId) throws SQLException {
        List<Field> fields = new ArrayList<>();
        try (var connection = database.connect();
             var query = connection.prepareStatement(
                 "SELECT field_key, label, required, repeatable FROM template_fields WHERE template_id = ? ORDER BY position")) {
            query.setString(1, templateId);
            try (var rows = query.executeQuery()) {
                while (rows.next()) {
                    fields.add(new Field(rows.getString(1), rows.getString(2), rows.getInt(3) == 1, rows.getInt(4) == 1));
                }
            }
        }
        return fields;
    }

    public Download download(String templateId) throws SQLException {
        try (var connection = database.connect();
             var query = connection.prepareStatement("SELECT filename, docx FROM templates WHERE id = ?")) {
            query.setString(1, templateId);
            try (var rows = query.executeQuery()) {
                return rows.next() ? new Download(rows.getString(1), rows.getBytes(2)) : null;
            }
        }
    }
}
