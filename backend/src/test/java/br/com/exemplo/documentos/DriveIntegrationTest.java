package br.com.exemplo.documentos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DriveIntegrationTest {
    @TempDir Path temporaryDirectory;

    @Test
    void driveExportAndUploadUseSameExtractionAndDoNotExposeToken() throws Exception {
        byte[] docx;
        try (var input = new ClassPathResource("google-docs/documento-test.docx").getInputStream()) {
            docx = input.readAllBytes();
        }
        var drive = new DriveGateway() {
            @Override public boolean configured(String templateId) { return true; }
            @Override public Copy copy(String templateId) {
                return new Copy("copy-id", "https://docs.google.com/document/d/copy-id/edit");
            }
            @Override public byte[] exportDocx(String fileId) { return docx; }
        };
        var database = new Database(temporaryDirectory.resolve("documents.db").toString());
        database.initialize();
        var templates = new TemplateCatalog(database);
        templates.seed();
        var documents = new DocumentService(database);
        var previews = new PreviewService(database, documents, templates, new DocumentValidator(new MetadataExtractor()));
        var api = MockMvcBuilders.standaloneSetup(new DocumentController(documents, templates, previews, drive)).build();
        ObjectMapper json = new ObjectMapper();

        String created = api.perform(post("/api/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"templateId\":\"parecer\",\"useDrive\":true}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.driveFileId").value("copy-id"))
            .andExpect(jsonPath("$.editUrl").value("https://docs.google.com/document/d/copy-id/edit"))
            .andReturn().getResponse().getContentAsString();
        assertFalse(created.contains("token"));
        String id = json.readTree(created).get("id").asText();

        String fromDrive = api.perform(post("/api/documents/" + id + "/previews/drive"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.origin").value("DRIVE"))
            .andExpect(jsonPath("$.valid").value(true))
            .andReturn().getResponse().getContentAsString();
        String fromUpload = api.perform(multipart("/api/documents/" + id + "/previews/upload")
                .file(new MockMultipartFile("file", "parecer.docx", "application/octet-stream", docx)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(true))
            .andReturn().getResponse().getContentAsString();
        assertEquals(json.readTree(fromDrive).get("fields"), json.readTree(fromUpload).get("fields"));
        String uploadPreviewId = json.readTree(fromUpload).get("id").asText();
        api.perform(post("/api/documents/" + id + "/previews/" + uploadPreviewId + "/confirm"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FINALIZADO"))
            .andExpect(jsonPath("$.finalOrigin").value("UPLOAD"));
        api.perform(get("/api/documents/" + id))
            .andExpect(status().isOk()).andExpect(jsonPath("$.metadata.assunto").exists());
    }

}
