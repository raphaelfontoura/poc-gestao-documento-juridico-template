package br.com.exemplo.documentos;

import java.sql.SQLException;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    public record CreateRequest(String templateId, Boolean useDrive) {}
    public record CreateResponse(String id, String templateId, String status, String driveFileId,
                                 String editUrl, String driveError) {}
    private final DocumentService documents;
    private final TemplateCatalog templates;
    private final PreviewService previews;
    private final DriveGateway drive;

    public DocumentController(DocumentService documents, TemplateCatalog templates, PreviewService previews,
                              DriveGateway drive) {
        this.documents = documents;
        this.templates = templates;
        this.previews = previews;
        this.drive = drive;
    }

    @PostMapping
    public ResponseEntity<CreateResponse> create(@RequestBody CreateRequest request) throws SQLException {
        if (request == null || request.templateId() == null) return ResponseEntity.badRequest().build();
        var document = documents.create(request.templateId());
        if (document == null) return ResponseEntity.notFound().build();
        String driveError = null;
        if (Boolean.TRUE.equals(request.useDrive())) {
            try {
                document = documents.linkDrive(document.id(), drive.copy(document.templateId()));
            } catch (DriveGateway.DriveFailure exception) {
                driveError = exception.getMessage();
            }
        }
        return ResponseEntity.status(201).body(new CreateResponse(document.id(), document.templateId(),
            document.status(), document.driveFileId(), document.editUrl(), driveError));
    }

    @GetMapping
    public List<DocumentService.Document> list() throws SQLException {
        return documents.list();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentService.Detail> detail(@PathVariable String id) throws SQLException {
        var document = documents.detail(id);
        return document == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(document);
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> finalFile(@PathVariable String id) throws SQLException {
        byte[] bytes = documents.finalDocx(id);
        if (bytes == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
            .header("Content-Disposition", "attachment; filename=\"documento-" + id + ".docx\"")
            .body(bytes);
    }

    @GetMapping("/{id}/template")
    public ResponseEntity<byte[]> template(@PathVariable String id) throws SQLException {
        var document = documents.find(id);
        if (document == null) return ResponseEntity.notFound().build();
        var file = templates.download(document.templateId());
        if (file == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
            .header("Content-Disposition", "attachment; filename=\"" + file.filename() + "\"")
            .body(file.bytes());
    }

    @PostMapping(value = "/{id}/previews/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@PathVariable String id, @RequestParam("file") MultipartFile file)
        throws SQLException, java.io.IOException {
        try {
            var preview = previews.upload(id, file.getOriginalFilename(), file.getBytes());
            return preview == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(preview);
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/{id}/previews/drive")
    public ResponseEntity<?> previewFromDrive(@PathVariable String id) throws SQLException {
        var document = documents.find(id);
        if (document == null) return ResponseEntity.notFound().build();
        if (!document.status().equals("EM_EDICAO")) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", "Documento já finalizado."));
        }
        if (document.driveFileId() == null) {
            return ResponseEntity.status(422).body(java.util.Map.of("error", "Documento sem cópia no Drive; use upload de DOCX."));
        }
        try {
            byte[] bytes = drive.exportDocx(document.driveFileId());
            return ResponseEntity.ok(previews.create(id, "documento.docx", bytes, "DRIVE"));
        } catch (DriveGateway.DriveFailure exception) {
            return ResponseEntity.status(422).body(java.util.Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/{id}/previews/{previewId}/confirm")
    public ResponseEntity<?> confirm(@PathVariable String id, @PathVariable String previewId) throws SQLException {
        if (documents.find(id) == null) return ResponseEntity.notFound().build();
        if (!previews.confirm(id, previewId)) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", "Prévia inválida, substituída ou documento já finalizado."));
        }
        return detail(id);
    }
}
