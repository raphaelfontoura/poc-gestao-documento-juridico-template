package br.com.exemplo.documentos;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.io.ByteArrayInputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TemplateControllerTest {
    @TempDir Path temporaryDirectory;

    @Test
    void listsTypeSpecificFieldsAndDownloadsMatchingDocx() throws Exception {
        var database = new Database(temporaryDirectory.resolve("documents.db").toString());
        database.initialize();
        var catalog = new TemplateCatalog(database);
        catalog.seed();
        var api = MockMvcBuilders.standaloneSetup(new TemplateController(catalog)).build();

        api.perform(get("/api/templates"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("parecer"))
            .andExpect(jsonPath("$[0].fields[4].key").value("procuradores_assinantes"))
            .andExpect(jsonPath("$[1].id").value("portaria"))
            .andExpect(jsonPath("$[1].fields[2].key").value("signatarios"))
            .andExpect(jsonPath("$[1].fields.length()").value(3));

        for (String type : new String[] {"parecer", "portaria"}) {
            byte[] downloaded = api.perform(get("/api/templates/" + type + "/download"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
            assertArrayEquals(catalog.download(type).bytes(), downloaded);
        }
    }

    @Test
    void reseedingReplacesOldTemplateWithUnfilledModel() throws Exception {
        var database = new Database(temporaryDirectory.resolve("documents.db").toString());
        database.initialize();
        var catalog = new TemplateCatalog(database);
        catalog.seed();
        try (var connection = database.connect();
             var update = connection.prepareStatement("UPDATE templates SET docx = ? WHERE id = 'parecer'")) {
            update.setBytes(1, new byte[] {1, 2, 3});
            update.executeUpdate();
        }
        catalog.seed();
        try (var document = new XWPFDocument(new ByteArrayInputStream(catalog.download("parecer").bytes()))) {
            String text = document.getParagraphs().stream().map(paragraph -> paragraph.getText())
                .reduce("", (left, right) -> left + "\n" + right);
            assertTrue(text.contains("PARECER Nº [número]/[ano]"));
            assertTrue(text.contains("[Descreva a consulta e os fatos relevantes.]"));
        }
    }
}
