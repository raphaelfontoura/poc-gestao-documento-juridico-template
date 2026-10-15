package br.com.exemplo.documentos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

class DatabaseTest {
    @TempDir Path temporaryDirectory;

    @Test
    void retainsTemplateAfterReinitialization() throws Exception {
        Path file = temporaryDirectory.resolve("documents.db");
        String insertedId = UUID.randomUUID().toString();
        try (var firstStart = new SpringApplicationBuilder(DocumentosApplication.class)
                 .web(WebApplicationType.NONE)
                 .properties("poc.database.path=" + file)
                 .run();
             var connection = firstStart.getBean(Database.class).connect();
             PreparedStatement insert = connection.prepareStatement(
                 "INSERT INTO templates (id, name, description, filename, docx) VALUES (?, ?, ?, ?, ?)")) {
            insert.setString(1, insertedId);
            insert.setString(2, "Parecer");
            insert.setString(3, "Modelo fictício");
            insert.setString(4, "parecer.docx");
            insert.setBytes(5, new byte[] {1, 2, 3});
            insert.executeUpdate();
        }

        try (var secondStart = new SpringApplicationBuilder(DocumentosApplication.class)
                 .web(WebApplicationType.NONE)
                 .properties("poc.database.path=" + file)
                 .run();
             var connection = secondStart.getBean(Database.class).connect();
             var statement = connection.prepareStatement("SELECT COUNT(*) FROM templates WHERE id = ?")) {
            statement.setString(1, insertedId);
            try (var result = statement.executeQuery()) {
                assertEquals(1, result.getInt(1));
            }
        }
    }
}
