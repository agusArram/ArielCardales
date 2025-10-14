package com.arielcardales.arielcardales.Updates;

import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Updater {

    // ⚠️ ACTUALIZAR ESTOS VALORES:
    private static final String GITHUB_USER = "agusArram";
    private static final String REPO_NAME = "ArielCardales";
    private static final String CURRENT_VERSION = "v2.1.0"; // 🔴 Cambiar esto en cada release

    private static final String API_URL =
            "https://api.github.com/repos/" + GITHUB_USER + "/" + REPO_NAME + "/releases/latest";

    private String latestVersion;
    private String downloadUrl;
    private String changelogUrl;

    /**
     * Verifica si hay una nueva versión disponible en GitHub
     * @return true si hay actualización disponible
     */
    public boolean hayNuevaVersion() {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();

            if (conn.getResponseCode() != 200) {
                System.err.println("Error al consultar GitHub API: " + conn.getResponseCode());
                return false;
            }

            // Leer respuesta JSON
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            reader.close();

            // Parsear JSON
            JSONObject json = new JSONObject(jsonBuilder.toString());
            latestVersion = json.getString("tag_name");
            changelogUrl = json.getString("html_url");

            // Buscar el asset .zip
            var assets = json.getJSONArray("assets");
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                String name = asset.getString("name");
                if (name.endsWith(".zip")) {
                    downloadUrl = asset.getString("browser_download_url");
                    break;
                }
            }

            // Comparar versiones (elimina 'v' si existe)
            String current = CURRENT_VERSION.replace("v", "");
            String latest = latestVersion.replace("v", "");

            return !current.equals(latest) && downloadUrl != null;

        } catch (Exception e) {
            System.err.println("Error al verificar actualizaciones: " + e.getMessage());
            return false;
        }
    }

    /**
     * Descarga la nueva versión
     * @param progressCallback función que recibe el progreso (0-100)
     * @return true si la descarga fue exitosa
     */
    public boolean descargarNuevaVersion(ProgressCallback progressCallback) {
        if (downloadUrl == null) {
            System.err.println("No hay URL de descarga disponible");
            return false;
        }

        try {
            Path tempDir = Files.createTempDirectory("ariel_update_");
            Path zipPath = tempDir.resolve("ArielCardales_" + latestVersion + ".zip");

            URL url = new URL(downloadUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Accept", "application/octet-stream");
            conn.connect();

            long fileSize = conn.getContentLengthLong();

            try (InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(zipPath.toFile())) {

                byte[] buffer = new byte[8192];
                long totalBytesRead = 0;
                int bytesRead;

                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;

                    if (progressCallback != null && fileSize > 0) {
                        int progress = (int) ((totalBytesRead * 100) / fileSize);
                        progressCallback.onProgress(progress);
                    }
                }
            }

            System.out.println("✅ Actualización descargada en: " + zipPath);
            System.out.println("📝 Changelog: " + changelogUrl);

            // Abrir carpeta con el ZIP descargado
            abrirCarpeta(tempDir);

            return true;

        } catch (Exception e) {
            System.err.println("Error al descargar actualización: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Abre la carpeta donde se descargó el ZIP
     */
    private void abrirCarpeta(Path path) {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                Runtime.getRuntime().exec("explorer " + path.toString());
            } else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                Runtime.getRuntime().exec("open " + path.toString());
            } else {
                Runtime.getRuntime().exec("xdg-open " + path.toString());
            }
        } catch (IOException e) {
            System.err.println("No se pudo abrir la carpeta automáticamente");
        }
    }

    // Getters
    public String getLatestVersion() {
        return latestVersion;
    }

    public String getCurrentVersion() {
        return CURRENT_VERSION;
    }

    public String getChangelogUrl() {
        return changelogUrl;
    }

    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(int percentage);
    }

    // En UpdateTester.java o crea un método temporal
    public static void testConnection() {
        try {
            String apiUrl = "https://api.github.com/repos/agusArram/ArielCardales/releases/latest";
            System.out.println("🔍 Probando conexión a: " + apiUrl);

            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Test");
            conn.setConnectTimeout(10000);
            conn.connect();

            int code = conn.getResponseCode();
            System.out.println("📡 Código de respuesta: " + code);

            if (code == 200) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                String response = reader.lines()
                        .collect(java.util.stream.Collectors.joining("\n"));
                System.out.println("✅ Respuesta:");
                System.out.println(response.substring(0, Math.min(500, response.length())));
            } else {
                System.err.println("❌ Error: " + code);
            }

        } catch (Exception e) {
            System.err.println("❌ Excepción: " + e.getMessage());
            e.printStackTrace();
        }
    }
}