package com.arielcardales.arielcardales.Updates;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * Configuración centralizada para el sistema de actualizaciones
 */
public class UpdateConfig {

    // ⚠️ ACTUALIZAR ESTOS VALORES EN CADA RELEASE
    private static final String CURRENT_VERSION = "v2.2.2";
    private static final String GITHUB_USER = "agusArram";
    private static final String REPO_NAME = "ArielCardales";

    // URLs
    public static final String API_URL =
            "https://api.github.com/repos/" + GITHUB_USER + "/" + REPO_NAME + "/releases/latest";

    public static final String DOWNLOAD_BASE_URL =
            "https://github.com/" + GITHUB_USER + "/" + REPO_NAME + "/releases/download/";

    // Configuración local
    private static final Path CONFIG_DIR = Paths.get(System.getProperty("user.home"), ".appinventario");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("update.properties");
    private static final Path BACKUP_DIR = CONFIG_DIR.resolve("backup");
    private static final Path DOWNLOAD_DIR = CONFIG_DIR.resolve("downloads");

    private Properties properties;

    public UpdateConfig() {
        this.properties = new Properties();
        cargarConfiguracion();
        crearDirectorios();
    }

    private void cargarConfiguracion() {
        try {
            if (Files.exists(CONFIG_FILE)) {
                try (InputStream input = Files.newInputStream(CONFIG_FILE)) {
                    properties.load(input);
                }
            }
            // Valores por defecto
            properties.putIfAbsent("auto_check", "true");
            properties.putIfAbsent("check_interval_hours", "24");
            properties.putIfAbsent("auto_download", "false");
            properties.putIfAbsent("keep_backups", "false");
            properties.putIfAbsent("max_backups", "3");

            guardarConfiguracion();
        } catch (IOException e) {
            System.err.println("Error al cargar configuración: " + e.getMessage());
        }
    }

    public void guardarConfiguracion() {
        try {
            Files.createDirectories(CONFIG_DIR);
            try (OutputStream output = Files.newOutputStream(CONFIG_FILE)) {
                properties.store(output, "App Inventario - Configuración de actualizaciones");
            }
        } catch (IOException e) {
            System.err.println("Error al guardar configuración: " + e.getMessage());
        }
    }

    private void crearDirectorios() {
        try {
            Files.createDirectories(CONFIG_DIR);
            Files.createDirectories(BACKUP_DIR);
            Files.createDirectories(DOWNLOAD_DIR);
        } catch (IOException e) {
            System.err.println("Error al crear directorios: " + e.getMessage());
        }
    }

    // Getters estáticos
    public static String getCurrentVersion() {
        return CURRENT_VERSION;
    }

    public static String getGithubUser() {
        return GITHUB_USER;
    }

    public static String getRepoName() {
        return REPO_NAME;
    }

    public static Path getBackupDir() {
        return BACKUP_DIR;
    }

    public static Path getDownloadDir() {
        return DOWNLOAD_DIR;
    }

    // Getters de configuración
    public boolean isAutoCheckEnabled() {
        return Boolean.parseBoolean(properties.getProperty("auto_check"));
    }

    public int getCheckIntervalHours() {
        return Integer.parseInt(properties.getProperty("check_interval_hours"));
    }

    public boolean isAutoDownloadEnabled() {
        return Boolean.parseBoolean(properties.getProperty("auto_download"));
    }

    public boolean isKeepBackupsEnabled() {
        return Boolean.parseBoolean(properties.getProperty("keep_backups"));
    }

    public int getMaxBackups() {
        return Integer.parseInt(properties.getProperty("max_backups"));
    }

    public long getLastCheckTimestamp() {
        return Long.parseLong(properties.getProperty("last_check", "0"));
    }

    public void setLastCheckTimestamp(long timestamp) {
        properties.setProperty("last_check", String.valueOf(timestamp));
        guardarConfiguracion();
    }

    public String getSkippedVersion() {
        return properties.getProperty("skipped_version", "");
    }

    public void setSkippedVersion(String version) {
        properties.setProperty("skipped_version", version);
        guardarConfiguracion();
    }

    public void clearSkippedVersion() {
        properties.remove("skipped_version");
        guardarConfiguracion();
    }
}