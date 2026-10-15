package br.com.exemplo.documentos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DriveFallbackTest {
    @TempDir Path temporaryDirectory;

    @Test
    void missingCredentialsKeepsManualUploadAvailable() throws Exception {
        var setup = setup(new GoogleDriveGateway("", "", "", "", ""));
        String created = setup.api().perform(post("/api/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"templateId\":\"portaria\",\"useDrive\":true}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.driveError").exists())
            .andReturn().getResponse().getContentAsString();
        String id = new ObjectMapper().readTree(created).get("id").asText();
        assertEquals("EM_EDICAO", setup.documents().find(id).status());
        upload(setup, id);
    }

    @Test
    void deniedCopyAndFailedExportKeepManualUploadAvailable() throws Exception {
        var denied = setup(new StubDrive(true, false));
        String deniedResponse = denied.api().perform(post("/api/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"templateId\":\"portaria\",\"useDrive\":true}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.driveError").exists())
            .andReturn().getResponse().getContentAsString();
        String deniedId = new ObjectMapper().readTree(deniedResponse).get("id").asText();
        upload(denied, deniedId);

        var failedExport = setup(new StubDrive(false, true));
        String copied = failedExport.api().perform(post("/api/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"templateId\":\"portaria\",\"useDrive\":true}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.driveFileId").value("copied-id"))
            .andReturn().getResponse().getContentAsString();
        String id = new ObjectMapper().readTree(copied).get("id").asText();
        failedExport.api().perform(post("/api/documents/" + id + "/previews/drive"))
            .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.error").exists());
        assertEquals("EM_EDICAO", failedExport.documents().find(id).status());
        upload(failedExport, id);
    }

    private void upload(Setup setup, String id) throws Exception {
        setup.api().perform(multipart("/api/documents/" + id + "/previews/upload")
                .file(new MockMultipartFile("file", "portaria.docx", "application/octet-stream", filledPortaria())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(true));
    }

    private Setup setup(DriveGateway drive) throws Exception {
        var database = new Database(temporaryDirectory.resolve(java.util.UUID.randomUUID() + ".db").toString());
        database.initialize();
        var templates = new TemplateCatalog(database);
        templates.seed();
        var documents = new DocumentService(database);
        var previews = new PreviewService(database, documents, templates, new DocumentValidator(new MetadataExtractor()));
        var api = MockMvcBuilders.standaloneSetup(new DocumentController(documents, templates, previews, drive)).build();
        return new Setup(api, documents);
    }

    private byte[] filledPortaria() throws Exception {
        try (var input = new ClassPathResource("templates/portaria.docx").getInputStream();
             var document = new XWPFDocument(input);
             var output = new ByteArrayOutputStream()) {
            var table = document.getTables().get(0);
            table.getRow(1).getCell(1).getParagraphs().get(0).getRuns().get(0).setText("Organização fictícia", 0);
            table.getRow(2).getCell(1).getParagraphs().get(0).getRuns().get(0).setText("14/10/2026", 0);
            table.getRow(3).getCell(1).getParagraphs().get(0).getRuns().get(0).setText("Ana Exemplo", 0);
            document.write(output);
            return output.toByteArray();
        }
    }

    private record Setup(org.springframework.test.web.servlet.MockMvc api, DocumentService documents) {}

    private record StubDrive(boolean denyCopy, boolean failExport) implements DriveGateway {
        @Override public boolean configured(String templateId) { return true; }
        @Override public Copy copy(String templateId) throws DriveFailure {
            if (denyCopy) throw new DriveFailure("Permissão negada; use upload de DOCX.");
            return new Copy("copied-id", "https://docs.google.com/document/d/copied-id/edit");
        }
        @Override public byte[] exportDocx(String fileId) throws DriveFailure {
            if (failExport) throw new DriveFailure("Falha de exportação; use upload de DOCX.");
            return new byte[0];
        }
    }
}
