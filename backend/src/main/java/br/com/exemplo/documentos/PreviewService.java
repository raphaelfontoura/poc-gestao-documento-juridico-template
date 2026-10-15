package br.com.exemplo.documentos;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PreviewService {
    public record Preview(String id, String documentId, String origin, boolean valid,
                          Map<String, Object> fields, Map<String, String> errors) {}

    private final Database database;
    private final DocumentService documents;
    private final TemplateCatalog templates;
    private final DocumentValidator validator;

    public PreviewService(Database database, DocumentService documents, TemplateCatalog templates,
                          DocumentValidator validator) {
        this.database = database;
        this.documents = documents;
        this.templates = templates;
        this.validator = validator;
    }

    public Preview upload(String documentId, String filename, byte[] bytes) throws SQLException {
        return create(documentId, filename, bytes, "UPLOAD");
    }

    public Preview create(String documentId, String filename, byte[] bytes, String origin) throws SQLException {
        var document = documents.find(documentId);
        if (document == null) return null;
        if (!document.status().equals("EM_EDICAO")) throw new IllegalStateException("Documento já finalizado.");
        var template = templates.find(document.templateId());
        var result = validator.validate(filename, bytes, template.fields());
        String previewId = UUID.randomUUID().toString();
        try (var connection = database.connect()) {
            connection.setAutoCommit(false);
            try {
                try (var delete = connection.prepareStatement("DELETE FROM previews WHERE document_id = ?")) {
                    delete.setString(1, documentId);
                    delete.executeUpdate();
                }
                try (var insert = connection.prepareStatement(
                    "INSERT INTO previews (id, document_id, origin, docx, valid, created_at) VALUES (?, ?, ?, ?, ?, ?)")) {
                    insert.setString(1, previewId);
                    insert.setString(2, documentId);
                    insert.setString(3, origin);
                    insert.setBytes(4, bytes.length <= DocumentValidator.MAX_BYTES ? bytes : new byte[0]);
                    insert.setInt(5, result.valid() ? 1 : 0);
                    insert.setString(6, Instant.now().toString());
                    insert.executeUpdate();
                }
                if (result.valid()) saveMetadata(connection, previewId, result.fields());
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        }
        return new Preview(previewId, documentId, origin, result.valid(), result.fields(), result.errors());
    }

    private void saveMetadata(Connection connection, String previewId, Map<String, Object> fields) throws SQLException {
        try (var insert = connection.prepareStatement(
            "INSERT INTO preview_metadata (preview_id, field_key, item_position, value) VALUES (?, ?, ?, ?)")) {
            for (var entry : fields.entrySet()) {
                if (entry.getValue() instanceof java.util.List<?> values) {
                    for (int index = 0; index < values.size(); index++) {
                        addMetadata(insert, previewId, entry.getKey(), index, values.get(index).toString());
                    }
                } else {
                    addMetadata(insert, previewId, entry.getKey(), 0, entry.getValue().toString());
                }
            }
            insert.executeBatch();
        }
    }

    private void addMetadata(java.sql.PreparedStatement insert, String id, String key, int position, String value)
        throws SQLException {
        insert.setString(1, id);
        insert.setString(2, key);
        insert.setInt(3, position);
        insert.setString(4, value);
        insert.addBatch();
    }

    public boolean confirm(String documentId, String previewId) throws SQLException {
        try (var connection = database.connect()) {
            connection.setAutoCommit(false);
            try {
                byte[] docx;
                String origin;
                try (var query = connection.prepareStatement(
                    "SELECT p.docx, p.origin FROM previews p JOIN documents d ON d.id = p.document_id " +
                    "WHERE p.id = ? AND p.document_id = ? AND p.valid = 1 AND d.status = 'EM_EDICAO'")) {
                    query.setString(1, previewId);
                    query.setString(2, documentId);
                    try (var rows = query.executeQuery()) {
                        if (!rows.next()) return false;
                        docx = rows.getBytes(1);
                        origin = rows.getString(2);
                    }
                }
                try (var update = connection.prepareStatement(
                    "UPDATE documents SET status = 'FINALIZADO', final_origin = ?, final_docx = ?, finalized_at = ? WHERE id = ?")) {
                    update.setString(1, origin);
                    update.setBytes(2, docx);
                    update.setString(3, Instant.now().toString());
                    update.setString(4, documentId);
                    update.executeUpdate();
                }
                try (var copy = connection.prepareStatement(
                    "INSERT INTO document_metadata (document_id, field_key, item_position, value) " +
                    "SELECT document_id, field_key, item_position, value FROM previews " +
                    "JOIN preview_metadata ON preview_metadata.preview_id = previews.id WHERE previews.id = ?")) {
                    copy.setString(1, previewId);
                    copy.executeUpdate();
                }
                try (var delete = connection.prepareStatement("DELETE FROM previews WHERE document_id = ?")) {
                    delete.setString(1, documentId);
                    delete.executeUpdate();
                }
                connection.commit();
                return true;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }
}
