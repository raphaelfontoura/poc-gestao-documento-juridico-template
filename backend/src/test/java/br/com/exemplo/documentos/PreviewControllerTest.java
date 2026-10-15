package br.com.exemplo.documentos;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PreviewControllerTest {
    @TempDir Path temporaryDirectory;

    @Test
    void onlyLatestValidPreviewCanBeConfirmed() throws Exception {
        var database = new Database(temporaryDirectory.resolve("documents.db").toString());
        database.initialize();
        var templates = new TemplateCatalog(database);
        templates.seed();
        var documents = new DocumentService(database);
        var previews = new PreviewService(database, documents, templates, new DocumentValidator(new MetadataExtractor()));
        var api = MockMvcBuilders.standaloneSetup(new DocumentController(documents, templates, previews,
            new GoogleDriveGateway("", "", "", "", ""))).build();
        String documentId = documents.create("portaria").id();
        byte[] validDocx = filledPortaria();
        ObjectMapper json = new ObjectMapper();

        String first = api.perform(multipart("/api/documents/" + documentId + "/previews/upload")
                .file(new MockMultipartFile("file", "portaria.docx", "application/octet-stream", validDocx)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(true))
            .andReturn().getResponse().getContentAsString();
        String firstId = json.readTree(first).get("id").asText();
        assertEquals("EM_EDICAO", documents.find(documentId).status());

        String invalid = api.perform(multipart("/api/documents/" + documentId + "/previews/upload")
                .file(new MockMultipartFile("file", "arquivo.pdf", "application/pdf", "%PDF".getBytes())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(false))
            .andReturn().getResponse().getContentAsString();
        String invalidId = json.readTree(invalid).get("id").asText();
        api.perform(post("/api/documents/" + documentId + "/previews/" + firstId + "/confirm"))
            .andExpect(status().isConflict());
        api.perform(post("/api/documents/" + documentId + "/previews/" + invalidId + "/confirm"))
            .andExpect(status().isConflict());
        assertEquals("EM_EDICAO", documents.find(documentId).status());

        String last = api.perform(multipart("/api/documents/" + documentId + "/previews/upload")
                .file(new MockMultipartFile("file", "portaria.docx", "application/octet-stream", validDocx)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(true))
            .andReturn().getResponse().getContentAsString();
        String lastId = json.readTree(last).get("id").asText();
        api.perform(post("/api/documents/" + documentId + "/previews/" + lastId + "/confirm"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FINALIZADO"));
        api.perform(get("/api/documents/" + documentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.finalOrigin").value("UPLOAD"))
            .andExpect(jsonPath("$.metadata.assunto").value("Organização fictícia"))
            .andExpect(jsonPath("$.metadata.data").value("2026-10-14"))
            .andExpect(jsonPath("$.metadata.signatarios[0]").value("Ana Exemplo"));
        byte[] downloaded = api.perform(get("/api/documents/" + documentId + "/file"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertArrayEquals(validDocx, downloaded);
        var restarted = new Database(temporaryDirectory.resolve("documents.db").toString());
        restarted.initialize();
        assertEquals("UPLOAD", new DocumentService(restarted).detail(documentId).finalOrigin());
        assertArrayEquals(validDocx, new DocumentService(restarted).finalDocx(documentId));
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
}
