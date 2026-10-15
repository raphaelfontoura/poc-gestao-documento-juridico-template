package br.com.exemplo.documentos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Component
public class Database {
    private final Path path;

    public Database(@Value("${poc.database.path:data/poc.db}") String path) {
        this.path = Path.of(path).toAbsolutePath();
    }

    public Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path);
        try (var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    @PostConstruct
    public void initialize() throws IOException, SQLException {
        Files.createDirectories(path.getParent());
        String schema;
        try (var input = new ClassPathResource("schema.sql").getInputStream()) {
            schema = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (var connection = connect(); var statement = connection.createStatement()) {
            for (String command : schema.split(";")) {
                if (!command.isBlank()) statement.execute(command);
            }
        }
    }
}
