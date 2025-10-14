package com.arielcardales.arielcardales.Updates;

/**
 * Excepción personalizada para errores en el sistema de actualizaciones
 */
public class UpdateException extends Exception {

    public enum ErrorType {
        NETWORK_ERROR("Error de conexión"),
        DOWNLOAD_ERROR("Error al descargar"),
        CHECKSUM_ERROR("Error de integridad"),
        INSTALL_ERROR("Error al instalar"),
        BACKUP_ERROR("Error al crear backup"),
        PERMISSION_ERROR("Error de permisos"),
        UNKNOWN_ERROR("Error desconocido");

        private final String message;

        ErrorType(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    private final ErrorType errorType;

    public UpdateException(ErrorType errorType, String message) {
        super(errorType.getMessage() + ": " + message);
        this.errorType = errorType;
    }

    public UpdateException(ErrorType errorType, String message, Throwable cause) {
        super(errorType.getMessage() + ": " + message, cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}