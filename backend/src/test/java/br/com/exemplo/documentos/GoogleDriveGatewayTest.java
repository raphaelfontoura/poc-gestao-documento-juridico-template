package br.com.exemplo.documentos;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GoogleDriveGatewayTest {
    @Test
    void oauthFailuresPointToTheCredentialThatNeedsCorrection() {
        assertTrue(GoogleDriveGateway.authErrorMessage("invalid_grant").contains("Refresh token"));
        assertTrue(GoogleDriveGateway.authErrorMessage("invalid_client").contains("Client ID"));
        assertTrue(GoogleDriveGateway.authErrorMessage("unauthorized_client").contains("Cliente OAuth"));
        assertFalse(GoogleDriveGateway.authErrorMessage("unexpected").contains("unexpected"));
    }

    @Test
    void readsNativeDocsByExportAndOfficeDocumentsByDownload() throws Exception {
        assertTrue(GoogleDriveGateway.documentContentUri("copy-id", "application/vnd.google-apps.document")
            .toString().contains("/export?mimeType="));
        assertTrue(GoogleDriveGateway.documentContentUri("copy-id", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            .toString().endsWith("?alt=media"));
        assertThrows(DriveGateway.DriveFailure.class,
            () -> GoogleDriveGateway.documentContentUri("copy-id", "application/pdf"));
    }
}
