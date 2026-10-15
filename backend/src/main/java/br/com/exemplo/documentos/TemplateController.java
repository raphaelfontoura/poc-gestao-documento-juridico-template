package br.com.exemplo.documentos;

import java.sql.SQLException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {
    private static final MediaType DOCX = MediaType.parseMediaType(
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    private final TemplateCatalog catalog;

    public TemplateController(TemplateCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public List<TemplateCatalog.Template> list() throws SQLException {
        return catalog.list();
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable String id) throws SQLException {
        TemplateCatalog.Download file = catalog.download(id);
        if (file == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
            .contentType(DOCX)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"")
            .body(file.bytes());
    }
}
