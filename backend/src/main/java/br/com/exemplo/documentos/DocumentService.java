package br.com.exemplo.documentos;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DocumentService {
    public record Document(String id, String templateId, String status, String driveFileId,
                           String editUrl, String finalOrigin, String createdAt, String finalizedAt) {}
    public record Detail(String id, String templateId, String status, String driveFileId,
                         String editUrl, String finalOrigin, String createdAt, String finalizedAt,
                         Map<String, Object> metadata) {}

    private final Database database;

    public DocumentService(Database database) {
        this.database = database;
    }

    public Document create(String templateId) throws SQLException {
        try (var connection = database.connect();
             var check = connection.prepareStatement("SELECT 1 FROM templates WHERE id = ?")) {
            check.setString(1, templateId);
            try (var rows = check.executeQuery()) {
                if (!rows.next()) return null;
            }
            String id = UUID.randomUUID().toString();
            String createdAt = Instant.now().toString();
            try (var insert = connection.prepareStatement(
                "INSERT INTO documents (id, template_id, status, created_at) VALUES (?, ?, 'EM_EDICAO', ?)")) {
                insert.setString(1, id);
                insert.setString(2, templateId);
                insert.setString(3, createdAt);
                insert.executeUpdate();
            }
            return new Document(id, templateId, "EM_EDICAO", null, null, null, createdAt, null);
        }
    }

    public Document linkDrive(String id, DriveGateway.Copy copy) throws SQLException {
        try (var connection = database.connect();
             var update = connection.prepareStatement(
                 "UPDATE documents SET drive_file_id = ?, edit_url = ? WHERE id = ? AND status = 'EM_EDICAO'")) {
            update.setString(1, copy.fileId());
            update.setString(2, copy.editUrl());
            update.setString(3, id);
            update.executeUpdate();
        }
        return find(id);
    }

    public List<Document> list() throws SQLException {
        List<Document> documents = new ArrayList<>();
        try (var connection = database.connect();
             var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT id, template_id, status, drive_file_id, edit_url, final_origin, created_at, finalized_at FROM documents ORDER BY created_at DESC")) {
            while (rows.next()) documents.add(map(rows));
        }
        return documents;
    }

    public Document find(String id) throws SQLException {
        try (var connection = database.connect();
             var query = connection.prepareStatement("SELECT id, template_id, status, drive_file_id, edit_url, final_origin, created_at, finalized_at FROM documents WHERE id = ?")) {
            query.setString(1, id);
            try (var rows = query.executeQuery()) {
                return rows.next() ? map(rows) : null;
            }
        }
    }

    public Detail detail(String id) throws SQLException {
        Document document = find(id);
        if (document == null) return null;
        Map<String, Object> metadata = new LinkedHashMap<>();
        try (var connection = database.connect();
             var query = connection.prepareStatement(
                 "SELECT m.field_key, m.value, f.repeatable FROM document_metadata m " +
                 "JOIN documents d ON d.id = m.document_id " +
                 "JOIN template_fields f ON f.template_id = d.template_id AND f.field_key = m.field_key " +
                 "WHERE m.document_id = ? ORDER BY f.position, m.item_position")) {
            query.setString(1, id);
            try (var rows = query.executeQuery()) {
                while (rows.next()) {
                    String key = rows.getString(1);
                    String value = rows.getString(2);
                    if (rows.getInt(3) == 1) {
                        @SuppressWarnings("unchecked")
                        List<String> values = (List<String>) metadata.computeIfAbsent(key, ignored -> new ArrayList<String>());
                        values.add(value);
                    } else {
                        metadata.put(key, value);
                    }
                }
            }
        }
        return new Detail(document.id(), document.templateId(), document.status(), document.driveFileId(),
            document.editUrl(), document.finalOrigin(), document.createdAt(), document.finalizedAt(), metadata);
    }

    public byte[] finalDocx(String id) throws SQLException {
        try (var connection = database.connect();
             var query = connection.prepareStatement(
                 "SELECT final_docx FROM documents WHERE id = ? AND status = 'FINALIZADO'")) {
            query.setString(1, id);
            try (var rows = query.executeQuery()) {
                return rows.next() ? rows.getBytes(1) : null;
            }
        }
    }

    private Document map(java.sql.ResultSet rows) throws SQLException {
        return new Document(rows.getString(1), rows.getString(2), rows.getString(3),
            rows.getString(4), rows.getString(5), rows.getString(6), rows.getString(7), rows.getString(8));
    }
}
