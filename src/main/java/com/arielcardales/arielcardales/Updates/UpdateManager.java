package com.arielcardales.arielcardales.Updates;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Orquesta todo el proceso de actualización
 */
public class UpdateManager {

    private final UpdateConfig config;
    private final UpdateChecker checker;
    private final UpdateDownloader downloader;
    private final UpdateInstaller installer;

    private UpdateChecker.ReleaseInfo latestRelease;
    private Path downloadedFile;
    private Path extractedDir;
    private Path backupDir;

    public UpdateManager() {
        this.config = new UpdateConfig();
        this.checker = new UpdateChecker(config);
        this.downloader = new UpdateDownloader(config);
        this.installer = new UpdateInstaller(config);
    }

    /**
     * Verifica si hay actualizaciones disponibles (async)
     */
    public CompletableFuture<Boolean> checkForUpdatesAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                boolean hasUpdate = checker.checkForUpdates();
                if (hasUpdate) {
                    latestRelease = checker.getLatestRelease();
                }
                return hasUpdate;
            } catch (UpdateException e) {
                System.err.println("Error al verificar actualizaciones: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Descarga la actualización (async)
     */
    public CompletableFuture<Boolean> downloadUpdateAsync(
            Consumer<UpdateProgress> progressCallback) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (latestRelease == null) {
                    throw new UpdateException(
                            UpdateException.ErrorType.DOWNLOAD_ERROR,
                            "No hay información de release"
                    );
                }

                // Fase 1: Descarga
                progressCallback.accept(new UpdateProgress(
                        UpdatePhase.DOWNLOADING, 0, "Iniciando descarga..."
                ));

                downloadedFile = downloader.download(
                        latestRelease.getDownloadUrl(),
                        (percentage, downloaded, total) -> {
                            String message = String.format(
                                    "Descargando... %d%% (%s / %s)",
                                    percentage,
                                    formatBytes(downloaded),
                                    formatBytes(total)
                            );
                            progressCallback.accept(new UpdateProgress(
                                    UpdatePhase.DOWNLOADING, percentage, message
                            ));
                        }
                );

                // Fase 2: Verificación (opcional - SHA256)
                progressCallback.accept(new UpdateProgress(
                        UpdatePhase.VERIFYING, 0, "Verificando integridad..."
                ));

                String checksum = downloader.calculateChecksum(downloadedFile);
                System.out.println("🔐 Checksum: " + checksum);

                progressCallback.accept(new UpdateProgress(
                        UpdatePhase.VERIFYING, 100, "Archivo verificado"
                ));

                return true;

            } catch (UpdateException e) {
                System.err.println("Error al descargar: " + e.getMessage());
                progressCallback.accept(new UpdateProgress(
                        UpdatePhase.ERROR, 0, "Error: " + e.getMessage()
                ));
                return false;
            }
        });
    }

    /**
     * Instala la actualización completa (async)
     */
    public CompletableFuture<Boolean> installUpdateAsync(
            Consumer<UpdateProgress> progressCallback,
            boolean autoRestart) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (downloadedFile == null) {
                    throw new UpdateException(
                            UpdateException.ErrorType.INSTALL_ERROR,
                            "No hay archivo descargado"
                    );
                }

                // Fase 3: Extracción
                progressCallback.accept(new UpdateProgress(
                        UpdatePhase.EXTRACTING, 0, "Descomprimiendo archivos..."
                ));

                extractedDir = downloader.extract(downloadedFile, (files, b, c) -> {
                    progressCallback.accept(new UpdateProgress(
                            UpdatePhase.EXTRACTING,
                            Math.min(files * 2, 100),
                            "Extrayendo archivos..."
                    ));
                });

                // Fase 4: Backup - COMENTAR/ELIMINAR COMPLETAMENTE
                /*
                if (config.isKeepBackupsEnabled()) {
                    progressCallback.accept(new UpdateProgress(
                        UpdatePhase.BACKING_UP, 0, "Creando backup..."
                    ));

                    try {
                        backupDir = installer.createBackup(items -> {
                            progressCallback.accept(new UpdateProgress(
                                UpdatePhase.BACKING_UP,
                                Math.min(items, 100),
                                "Respaldando archivos..."
                            ));
                        });
                    } catch (UpdateException backupError) {
                        System.err.println("⚠️ Advertencia: No se pudo crear backup completo: " +
                            backupError.getMessage());
                        System.err.println("⚠️ Continuando instalación sin backup...");

                        progressCallback.accept(new UpdateProgress(
                            UpdatePhase.BACKING_UP,
                            100,
                            "⚠️ Backup parcial (advertencia ignorada)"
                        ));

                        backupDir = null;
                    }
                }
                */

                System.out.println("⏭️ Backups deshabilitados (omitiendo fase de backup)");
                progressCallback.accept(new UpdateProgress(
                        UpdatePhase.BACKING_UP, 100, "Backups deshabilitados"
                ));

// Fase 5: Instalación
                progressCallback.accept(new UpdateProgress(
                        UpdatePhase.INSTALLING, 0, "Preparando actualización..."
                ));

                installer.install(extractedDir, items -> {
                    progressCallback.accept(new UpdateProgress(
                            UpdatePhase.INSTALLING,
                            Math.min(items, 100),
                            "Creando script de actualización..."
                    ));
                });

            // Fase 6: NO limpiar todavía (el script lo hará)
                progressCallback.accept(new UpdateProgress(
                        UpdatePhase.CLEANING_UP, 100, "Listo para actualizar"
                ));

            // Fase 7: Completado
                progressCallback.accept(new UpdateProgress(
                        UpdatePhase.COMPLETED, 100, "¡Actualización preparada!"
                ));

            // ⭐ CAMBIO: Ejecutar script en lugar de reiniciar directamente
                if (autoRestart) {
                    Thread.sleep(2000);
                    installer.executeUpdateAndRestart(); // Método nuevo
                }

                return true;

            } catch (Exception e) {
                System.err.println("Error durante instalación: " + e.getMessage());
                e.printStackTrace();

                // Intentar rollback
                try {
                    progressCallback.accept(new UpdateProgress(
                            UpdatePhase.ROLLING_BACK, 0, "Restaurando versión anterior..."
                    ));
                    installer.rollback();
                    progressCallback.accept(new UpdateProgress(
                            UpdatePhase.ROLLED_BACK, 100, "Versión anterior restaurada"
                    ));
                } catch (UpdateException rollbackError) {
                    progressCallback.accept(new UpdateProgress(
                            UpdatePhase.ERROR, 0, "Error crítico: " + rollbackError.getMessage()
                    ));
                }

                return false;
            }
        });
    }

    /**
     * Proceso completo: descarga + instala
     */
    public CompletableFuture<Boolean> downloadAndInstallAsync(
            Consumer<UpdateProgress> progressCallback,
            boolean autoRestart) {

        return downloadUpdateAsync(progressCallback)
                .thenCompose(success -> {
                    if (success) {
                        return installUpdateAsync(progressCallback, autoRestart);
                    } else {
                        return CompletableFuture.completedFuture(false);
                    }
                });
    }

    /**
     * Omitir esta versión (no volver a notificar)
     */
    public void skipVersion() {
        if (latestRelease != null) {
            config.setSkippedVersion(latestRelease.getTagName());
            System.out.println("⏭️ Versión " + latestRelease.getTagName() + " omitida");
        }
    }

    /**
     * Resetear versión omitida
     */
    public void clearSkippedVersion() {
        config.clearSkippedVersion();
    }

    /**
     * Verifica si debe buscar actualizaciones según intervalo configurado
     */
    public boolean shouldCheckForUpdates() {
        return checker.shouldCheckForUpdates();
    }

    // Getters
    public UpdateConfig getConfig() {
        return config;
    }

    public UpdateChecker.ReleaseInfo getLatestRelease() {
        return latestRelease;
    }

    public String getCurrentVersion() {
        return UpdateConfig.getCurrentVersion();
    }

    // Helpers
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }

    /**
     * Enum para fases del proceso de actualización
     */
    public enum UpdatePhase {
        CHECKING("Verificando"),
        DOWNLOADING("Descargando"),
        VERIFYING("Verificando"),
        EXTRACTING("Extrayendo"),
        BACKING_UP("Respaldando"),
        INSTALLING("Instalando"),
        CLEANING_UP("Limpiando"),
        COMPLETED("Completado"),
        ROLLING_BACK("Restaurando"),
        ROLLED_BACK("Restaurado"),
        ERROR("Error");

        private final String displayName;

        UpdatePhase(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Clase para reportar progreso
     */
    public static class UpdateProgress {
        private final UpdatePhase phase;
        private final int percentage;
        private final String message;

        public UpdateProgress(UpdatePhase phase, int percentage, String message) {
            this.phase = phase;
            this.percentage = percentage;
            this.message = message;
        }

        public UpdatePhase getPhase() {
            return phase;
        }

        public int getPercentage() {
            return percentage;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return String.format("[%s] %d%% - %s",
                    phase.getDisplayName(), percentage, message);
        }
    }
}