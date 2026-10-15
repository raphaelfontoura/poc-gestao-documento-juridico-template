package br.com.exemplo.documentos;

public interface DriveGateway {
    record Copy(String fileId, String editUrl) {}
    boolean configured(String templateId);
    Copy copy(String templateId) throws DriveFailure;
    byte[] exportDocx(String fileId) throws DriveFailure;

    class DriveFailure extends Exception {
        public DriveFailure(String message) { super(message); }
        public DriveFailure(String message, Throwable cause) { super(message, cause); }
    }
}
