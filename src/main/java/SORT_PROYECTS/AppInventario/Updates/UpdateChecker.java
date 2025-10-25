package SORT_PROYECTS.AppInventario.Updates;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Verifica si hay nuevas versiones disponibles en GitHub
 */
public class UpdateChecker {

    private final UpdateConfig config;
    private ReleaseInfo latestRelease;

    public UpdateChecker(UpdateConfig config) {
        this.config = config;
    }

    /**
     * Verifica si hay una nueva versión disponible
     */
    public boolean checkForUpdates() throws UpdateException {
        try {
            // Actualizar timestamp de última verificación
            config.setLastCheckTimestamp(System.currentTimeMillis());

            // Consultar API de GitHub
            System.out.println("🌐 Conectando a: " + UpdateConfig.API_URL);

            URL url = new URL(UpdateConfig.API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setRequestProperty("User-Agent", "AppInventario-Updater");
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            System.out.println("🔗 Conectando...");
            conn.connect();

            int responseCode = conn.getResponseCode();
            System.out.println("📡 Código de respuesta: " + responseCode);

            if (responseCode == 404) {
                System.out.println("ℹ️ No hay releases publicados todavía");
                return false; // No hay actualización, pero no es error crítico
            }

            if (responseCode != 200) {
                // Leer mensaje de error
                try (BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream()))) {
                    String errorMessage = errorReader.lines()
                            .collect(java.util.stream.Collectors.joining("\n"));
                    System.err.println("❌ Error de GitHub: " + errorMessage);
                } catch (Exception ignored) {}

                throw new UpdateException(
                        UpdateException.ErrorType.NETWORK_ERROR,
                        "GitHub API respondió con código: " + responseCode
                );
            }

            // Leer respuesta JSON
            StringBuilder jsonBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }
            }

            // Parsear información del release
            JSONObject json = new JSONObject(jsonBuilder.toString());
            latestRelease = parseReleaseInfo(json);

            // Verificar si fue omitida por el usuario
            String skippedVersion = config.getSkippedVersion();
            if (skippedVersion.equals(latestRelease.getTagName())) {
                System.out.println("Versión " + skippedVersion + " omitida por el usuario");
                return false;
            }

            // Comparar versiones
            boolean isNewer = isNewerVersion(
                    UpdateConfig.getCurrentVersion(),
                    latestRelease.getTagName()
            );

            System.out.println("📊 Comparación de versiones:");
            System.out.println("   Actual: " + UpdateConfig.getCurrentVersion());
            System.out.println("   Nueva: " + latestRelease.getTagName());
            System.out.println("   ¿Es más nueva? " + isNewer);

            return isNewer;

        } catch (UpdateException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Excepción completa: ");
            e.printStackTrace();
            throw new UpdateException(
                    UpdateException.ErrorType.NETWORK_ERROR,
                    "No se pudo verificar actualizaciones: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Parsea la información del release desde JSON
     */
    private ReleaseInfo parseReleaseInfo(JSONObject json) throws UpdateException {
        try {
            String tagName = json.getString("tag_name");
            String name = json.getString("name");
            String body = json.optString("body", "Sin descripción");
            String htmlUrl = json.getString("html_url");
            String publishedAt = json.getString("published_at");
            boolean prerelease = json.getBoolean("prerelease");

            // Buscar asset .zip
            JSONArray assets = json.getJSONArray("assets");
            String downloadUrl = null;
            long fileSize = 0;

            System.out.println("🔍 Buscando archivo .zip en " + assets.length() + " assets:");

            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                String assetName = asset.getString("name");

                System.out.println("   Asset " + i + ": " + assetName);

                // ⭐ CAMBIO: Buscar cualquier .zip (ya no solo "ArielCardales")
                if (assetName.toLowerCase().endsWith(".zip")) {
                    downloadUrl = asset.getString("browser_download_url");
                    fileSize = asset.getLong("size");
                    System.out.println("   ✅ ZIP encontrado: " + assetName);
                    break;
                }
            }

            if (downloadUrl == null) {
                throw new UpdateException(
                        UpdateException.ErrorType.DOWNLOAD_ERROR,
                        "No se encontró archivo .zip en el release. Assets disponibles: " +
                                json.getJSONArray("assets").length()
                );
            }

            System.out.println("📥 URL de descarga: " + downloadUrl);
            System.out.println("📊 Tamaño: " + (fileSize / 1024 / 1024) + " MB");

            return new ReleaseInfo(
                    tagName, name, body, downloadUrl,
                    htmlUrl, publishedAt, fileSize, prerelease
            );

        } catch (Exception e) {
            System.err.println("❌ Error detallado al parsear release:");
            e.printStackTrace();
            throw new UpdateException(
                    UpdateException.ErrorType.UNKNOWN_ERROR,
                    "Error al parsear información del release: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Compara dos versiones (formato: v1.2.3)
     * @return true si 'latest' es más nueva que 'current'
     */
    private boolean isNewerVersion(String current, String latest) {
        try {
            // Quitar 'v' y limpiar
            String currentClean = current.toLowerCase().trim().replace("v", "");
            String latestClean = latest.toLowerCase().trim().replace("v", "");

            System.out.println("🔍 Comparando versiones:");
            System.out.println("   Local: " + current + " → " + currentClean);
            System.out.println("   GitHub: " + latest + " → " + latestClean);

            // Si son iguales, no hay actualización
            if (currentClean.equals(latestClean)) {
                System.out.println("   ℹ️ Versiones idénticas");
                return false;
            }

            // Dividir por puntos
            String[] currentParts = currentClean.split("\\.");
            String[] latestParts = latestClean.split("\\.");

            // Comparar cada parte
            int maxLength = Math.max(currentParts.length, latestParts.length);

            for (int i = 0; i < maxLength; i++) {
                int currentNum = i < currentParts.length ? safeParseInt(currentParts[i]) : 0;
                int latestNum = i < latestParts.length ? safeParseInt(latestParts[i]) : 0;

                System.out.println("   Parte " + i + ": " + currentNum + " vs " + latestNum);

                if (latestNum > currentNum) {
                    System.out.println("   ✅ GitHub tiene versión más nueva");
                    return true;
                } else if (latestNum < currentNum) {
                    System.out.println("   ⬇️ Versión local es más nueva (downgrade no permitido)");
                    return false;
                }
                // Si son iguales, continuar con la siguiente parte
            }

            // Todas las partes son iguales
            System.out.println("   ℹ️ Sin diferencias");
            return false;

        } catch (Exception e) {
            System.err.println("❌ Error al comparar versiones: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Convierte una parte de versión a número de forma segura
     */
    private int safeParseInt(String str) {
        try {
            // Eliminar cualquier caracter no numérico
            String cleaned = str.replaceAll("[^0-9]", "");
            return cleaned.isEmpty() ? 0 : Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }


    /**
     * Verifica si es momento de buscar actualizaciones según intervalo configurado
     */
    public boolean shouldCheckForUpdates() {
        if (!config.isAutoCheckEnabled()) {
            return false;
        }

        long lastCheck = config.getLastCheckTimestamp();
        long intervalMillis = config.getCheckIntervalHours() * 3600L * 1000L;
        long now = System.currentTimeMillis();

        return (now - lastCheck) >= intervalMillis;
    }

    public ReleaseInfo getLatestRelease() {
        return latestRelease;
    }

    /**
     * Clase interna para almacenar información del release
     */
    public static class ReleaseInfo {
        private final String tagName;
        private final String name;
        private final String body;
        private final String downloadUrl;
        private final String htmlUrl;
        private final String publishedAt;
        private final long fileSize;
        private final boolean prerelease;

        public ReleaseInfo(String tagName, String name, String body,
                           String downloadUrl, String htmlUrl, String publishedAt,
                           long fileSize, boolean prerelease) {
            this.tagName = tagName;
            this.name = name;
            this.body = body;
            this.downloadUrl = downloadUrl;
            this.htmlUrl = htmlUrl;
            this.publishedAt = publishedAt;
            this.fileSize = fileSize;
            this.prerelease = prerelease;
        }

        public String getTagName() { return tagName; }
        public String getName() { return name; }
        public String getBody() { return body; }
        public String getDownloadUrl() { return downloadUrl; }
        public String getHtmlUrl() { return htmlUrl; }
        public String getPublishedAt() { return publishedAt; }
        public long getFileSize() { return fileSize; }
        public boolean isPrerelease() { return prerelease; }

        public String getFormattedPublishDate() {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(
                        publishedAt,
                        DateTimeFormatter.ISO_DATE_TIME
                );
                return dateTime.format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                );
            } catch (Exception e) {
                return publishedAt;
            }
        }

        public String getFormattedFileSize() {
            if (fileSize < 1024) {
                return fileSize + " B";
            } else if (fileSize < 1024 * 1024) {
                return String.format("%.1f KB", fileSize / 1024.0);
            } else {
                return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
            }
        }
    }


}