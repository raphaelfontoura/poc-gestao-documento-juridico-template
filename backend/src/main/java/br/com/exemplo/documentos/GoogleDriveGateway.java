package br.com.exemplo.documentos;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GoogleDriveGateway implements DriveGateway {
    private static final String DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String GOOGLE_DOC_MIME = "application/vnd.google-apps.document";
    private final String clientId;
    private final String clientSecret;
    private final String refreshToken;
    private final String parecerTemplateId;
    private final String portariaTemplateId;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper json = new ObjectMapper();

    public GoogleDriveGateway(@Value("${GOOGLE_CLIENT_ID:}") String clientId,
                              @Value("${GOOGLE_CLIENT_SECRET:}") String clientSecret,
                              @Value("${GOOGLE_REFRESH_TOKEN:}") String refreshToken,
                              @Value("${GOOGLE_TEMPLATE_PARECER_ID:}") String parecerTemplateId,
                              @Value("${GOOGLE_TEMPLATE_PORTARIA_ID:}") String portariaTemplateId) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
        this.parecerTemplateId = parecerTemplateId;
        this.portariaTemplateId = portariaTemplateId;
    }

    @Override
    public boolean configured(String templateId) {
        return !clientId.isBlank() && !clientSecret.isBlank() && !refreshToken.isBlank()
            && !sourceId(templateId).isBlank();
    }

    private String sourceId(String templateId) {
        return switch (templateId) {
            case "parecer" -> parecerTemplateId;
            case "portaria" -> portariaTemplateId;
            default -> "";
        };
    }

    @Override
    public Copy copy(String templateId) throws DriveFailure {
        if (!configured(templateId)) throw new DriveFailure("Integração Google não configurada; use o DOCX por upload.");
        String source = encode(sourceId(templateId));
        String name = "POC " + templateId + " " + java.time.Instant.now();
        String body;
        try {
            body = json.writeValueAsString(java.util.Map.of("name", name));
        } catch (IOException exception) {
            throw new DriveFailure("Não foi possível preparar a cópia no Drive.", exception);
        }
        var request = HttpRequest.newBuilder(URI.create("https://www.googleapis.com/drive/v3/files/" + source +
                "/copy?fields=id,webViewLink"))
            .header("Authorization", "Bearer " + accessToken())
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 403 || response.statusCode() == 404) {
                throw new DriveFailure("Sem permissão para copiar o template no Drive; use o DOCX por upload.");
            }
            if (response.statusCode() / 100 != 2) {
                throw new DriveFailure("Falha ao copiar o template no Drive; use o DOCX por upload.");
            }
            String id = json.readTree(response.body()).path("id").asText();
            if (id.isBlank()) throw new DriveFailure("O Drive não retornou o ID da cópia.");
            String link = json.readTree(response.body()).path("webViewLink").asText();
            if (link.isBlank()) link = "https://docs.google.com/document/d/" + encode(id) + "/edit";
            return new Copy(id, link);
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new DriveFailure("Não foi possível acessar o Drive; use o DOCX por upload.", exception);
        }
    }

    @Override
    public byte[] exportDocx(String fileId) throws DriveFailure {
        if (!configured("parecer") && !configured("portaria")) {
            throw new DriveFailure("Integração Google não configurada; use o DOCX por upload.");
        }
        String token = accessToken();
        var metadataRequest = HttpRequest.newBuilder(URI.create("https://www.googleapis.com/drive/v3/files/" +
                encode(fileId) + "?fields=mimeType"))
            .header("Authorization", "Bearer " + token)
            .timeout(Duration.ofSeconds(30)).GET().build();
        try {
            var metadataResponse = http.send(metadataRequest, HttpResponse.BodyHandlers.ofString());
            if (metadataResponse.statusCode() == 403) {
                throw new DriveFailure("Sem permissão para ler a cópia no Drive; use o DOCX por upload.");
            }
            if (metadataResponse.statusCode() == 404) {
                throw new DriveFailure("Cópia não encontrada no Drive; use o DOCX por upload.");
            }
            if (metadataResponse.statusCode() / 100 != 2) {
                throw new DriveFailure("Falha ao consultar a cópia no Drive; use o DOCX por upload.");
            }
            String mimeType = json.readTree(metadataResponse.body()).path("mimeType").asText();
            var request = HttpRequest.newBuilder(documentContentUri(fileId, mimeType))
                .header("Authorization", "Bearer " + token)
                .timeout(Duration.ofSeconds(30)).GET().build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 403) {
                throw new DriveFailure("Sem permissão para ler o documento no Drive; use o DOCX por upload.");
            }
            if (response.statusCode() == 404) {
                throw new DriveFailure("Documento não encontrado no Drive; use o DOCX por upload.");
            }
            if (response.statusCode() / 100 != 2) {
                throw new DriveFailure("Falha ao obter o DOCX do Drive; use o DOCX por upload.");
            }
            return response.body();
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new DriveFailure("Não foi possível exportar o Google Docs; use o DOCX por upload.", exception);
        }
    }

    static URI documentContentUri(String fileId, String mimeType) throws DriveFailure {
        String base = "https://www.googleapis.com/drive/v3/files/" +
            URLEncoder.encode(fileId, StandardCharsets.UTF_8);
        return switch (mimeType) {
            case GOOGLE_DOC_MIME -> URI.create(base + "/export?mimeType=" +
                URLEncoder.encode(DOCX_MIME, StandardCharsets.UTF_8));
            case DOCX_MIME -> URI.create(base + "?alt=media");
            default -> throw new DriveFailure("A cópia no Drive não é Google Docs nem DOCX; use o DOCX por upload.");
        };
    }

    private String accessToken() throws DriveFailure {
        String form = "client_id=" + encode(clientId) + "&client_secret=" + encode(clientSecret)
            + "&refresh_token=" + encode(refreshToken) + "&grant_type=refresh_token";
        var request = HttpRequest.newBuilder(URI.create("https://oauth2.googleapis.com/token"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .timeout(Duration.ofSeconds(20))
            .POST(HttpRequest.BodyPublishers.ofString(form)).build();
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                String code;
                try {
                    code = json.readTree(response.body()).path("error").asText();
                } catch (IOException exception) {
                    code = "";
                }
                throw new DriveFailure(authErrorMessage(code));
            }
            String token = json.readTree(response.body()).path("access_token").asText();
            if (token.isBlank()) throw new DriveFailure("A autorização Google não retornou um token.");
            return token;
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new DriveFailure("Não foi possível autorizar a conta Google; use o DOCX por upload.", exception);
        }
    }

    static String authErrorMessage(String code) {
        return switch (code) {
            case "invalid_grant" -> "Refresh token inválido ou expirado para este cliente OAuth. "
                + "Gere outro com o mesmo client ID configurado no backend.";
            case "invalid_client" -> "Client ID ou client secret rejeitado pelo Google. "
                + "Confira se o backend usa o mesmo cliente OAuth do refresh token.";
            case "unauthorized_client" -> "Cliente OAuth não autorizado a renovar o token. "
                + "Confira o tipo e a configuração do cliente.";
            default -> "Autorização Google indisponível; use o DOCX por upload.";
        };
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
