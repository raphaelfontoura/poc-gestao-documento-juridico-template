package br.com.exemplo.documentos;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DocumentControllerTest {
    @TempDir Path temporaryDirectory;

    @Test
    void createsListsAndRetainsEditableDocumentAfterRestart() throws Exception {
        Path file = temporaryDirectory.resolve("documents.db");
        var database = new Database(file.toString());
        database.initialize();
        var templates = new TemplateCatalog(database);
        templates.seed();
        var documents = new DocumentService(database);
        var previews = new PreviewService(database, documents, templates, new DocumentValidator(new MetadataExtractor()));
        var api = MockMvcBuilders.standaloneSetup(new DocumentController(documents, templates, previews,
            new GoogleDriveGateway("", "", "", "", ""))).build();

        String response = api.perform(post("/api/documents")
                .contentType(MediaType.APPLICATION_JSON).content("{\"templateId\":\"parecer\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("EM_EDICAO"))
            .andReturn().getResponse().getContentAsString();
        String id = new ObjectMapper().readTree(response).get("id").asText();

        api.perform(get("/api/documents"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(id));
        api.perform(get("/api/documents/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.templateId").value("parecer"));
        byte[] downloaded = api.perform(get("/api/documents/" + id + "/template"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertArrayEquals(templates.download("parecer").bytes(), downloaded);

        var restarted = new Database(file.toString());
        restarted.initialize();
        var saved = new DocumentService(restarted).find(id);
        assertEquals("parecer", saved.templateId());
        assertEquals("EM_EDICAO", saved.status());
    }
}
