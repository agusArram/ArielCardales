package SORT_PROYECTS.AppInventario.Updates;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Descarga y valida archivos de actualización
 */
public class UpdateDownloader {

    private final UpdateConfig config;
    private Path downloadedFile;

    public UpdateDownloader(UpdateConfig config) {
        this.config = config;
    }

    /**
     * Descarga el archivo de actualización
     */
    public Path download(String downloadUrl, ProgressCallback callback) throws UpdateException {
        try {
            // Crear nombre de archivo temporal
            String fileName = "ArielCardales_update_" + System.currentTimeMillis() + ".zip";
            downloadedFile = UpdateConfig.getDownloadDir().resolve(fileName);

            // Conectar y descargar
            URL url = new URL(downloadUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Accept", "application/octet-stream");
            conn.setRequestProperty("User-Agent", "ArielCardales-Updater");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.connect();

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new UpdateException(
                        UpdateException.ErrorType.DOWNLOAD_ERROR,
                        "Servidor respondió con código: " + responseCode
                );
            }

            long fileSize = conn.getContentLengthLong();
            long totalBytesRead = 0;
            int lastProgress = 0;

            // Descargar con barra de progreso
            try (InputStream input = new BufferedInputStream(conn.getInputStream());
                 OutputStream output = new BufferedOutputStream(
                         Files.newOutputStream(downloadedFile))) {

                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;

                    if (callback != null && fileSize > 0) {
                        int currentProgress = (int) ((totalBytesRead * 100) / fileSize);
                        if (currentProgress > lastProgress) {
                            lastProgress = currentProgress;
                            callback.onProgress(
                                    currentProgress,
                                    totalBytesRead,
                                    fileSize
                            );
                        }
                    }
                }
            }

            // Verificar que se descargó completamente
            if (fileSize > 0 && Files.size(downloadedFile) != fileSize) {
                Files.deleteIfExists(downloadedFile);
                throw new UpdateException(
                        UpdateException.ErrorType.DOWNLOAD_ERROR,
                        "Archivo incompleto (esperado: " + fileSize +
                                ", descargado: " + Files.size(downloadedFile) + ")"
                );
            }

            System.out.println("✅ Descarga completada: " + downloadedFile);
            return downloadedFile;

        } catch (IOException e) {
            if (downloadedFile != null) {
                try {
                    Files.deleteIfExists(downloadedFile);
                } catch (IOException ignored) {}
            }
            throw new UpdateException(
                    UpdateException.ErrorType.DOWNLOAD_ERROR,
                    "Error durante la descarga",
                    e
            );
        }
    }

    /**
     * Descomprime el archivo ZIP descargado
     */
    public Path extract(Path zipFile, ProgressCallback callback) throws UpdateException {
        try {
            Path extractDir = UpdateConfig.getDownloadDir().resolve(
                    "extracted_" + System.currentTimeMillis()
            );
            Files.createDirectories(extractDir);

            try (ZipInputStream zis = new ZipInputStream(
                    new BufferedInputStream(Files.newInputStream(zipFile)))) {

                ZipEntry entry;
                int filesExtracted = 0;

                while ((entry = zis.getNextEntry()) != null) {
                    Path filePath = extractDir.resolve(entry.getName());

                    // Prevenir path traversal attacks
                    if (!filePath.normalize().startsWith(extractDir.normalize())) {
                        throw new UpdateException(
                                UpdateException.ErrorType.INSTALL_ERROR,
                                "Path inválido en ZIP: " + entry.getName()
                        );
                    }

                    if (entry.isDirectory()) {
                        Files.createDirectories(filePath);
                    } else {
                        Files.createDirectories(filePath.getParent());

                        try (OutputStream output = new BufferedOutputStream(
                                Files.newOutputStream(filePath))) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = zis.read(buffer)) != -1) {
                                output.write(buffer, 0, bytesRead);
                            }
                        }
                    }

                    filesExtracted++;
                    if (callback != null && filesExtracted % 5 == 0) {
                        callback.onProgress(filesExtracted, 0, 0);
                    }

                    zis.closeEntry();
                }
            }

            System.out.println("✅ Archivos extraídos en: " + extractDir);
            return extractDir;

        } catch (IOException e) {
            throw new UpdateException(
                    UpdateException.ErrorType.INSTALL_ERROR,
                    "Error al descomprimir archivo",
                    e
            );
        }
    }

    /**
     * Calcula checksum SHA-256 de un archivo
     */
    public String calculateChecksum(Path file) throws UpdateException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            try (InputStream input = new BufferedInputStream(
                    Files.newInputStream(file))) {
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (Exception e) {
            throw new UpdateException(
                    UpdateException.ErrorType.CHECKSUM_ERROR,
                    "Error al calcular checksum",
                    e
            );
        }
    }

    /**
     * Limpia archivos temporales de descarga
     */
    public void cleanupDownloads() {
        try {
            if (downloadedFile != null && Files.exists(downloadedFile)) {
                Files.deleteIfExists(downloadedFile);
                System.out.println("🗑️ Archivo de descarga eliminado");
            }
        } catch (IOException e) {
            System.err.println("No se pudo eliminar archivo temporal: " + e.getMessage());
        }
    }

    /**
     * Callback para reportar progreso de descarga
     */
    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(int percentage, long bytesDownloaded, long totalBytes);
    }
}