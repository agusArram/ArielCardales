package com.arielcardales.arielcardales.Updates;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Instala actualizaciones y maneja backups
 */
public class UpdateInstaller {

    private final UpdateConfig config;
    private Path currentAppDir;
    private Path backupDir;

    public UpdateInstaller(UpdateConfig config) {
        this.config = config;
        this.currentAppDir = detectAppDirectory();
    }

    /**
     * Detecta el directorio de la aplicación actual
     */
    private Path detectAppDirectory() {
        try {
            // Obtener ubicación del JAR/clases en ejecución
            String location = UpdateInstaller.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();

            // En Windows, eliminar el '/' inicial si existe (ej: /C:/...)
            if (location.startsWith("/") && location.contains(":")) {
                location = location.substring(1);
            }

            Path jarFile = Paths.get(location);

            // ⭐ NUEVO: Detectar si estamos en entorno de desarrollo
            if (location.contains("target/classes") || location.contains("target\\classes")) {
                System.err.println("⚠️ MODO DESARROLLO DETECTADO");
                System.err.println("⚠️ El sistema de actualización está diseñado para el .exe empaquetado");
                System.err.println("⚠️ No se recomienda actualizar desde el IDE");

                // Retornar directorio de proyecto (pero no actualizar)
                return jarFile.getParent().getParent();
            }

            // Si estamos en un JAR dentro de app/
            if (jarFile.toString().contains("app")) {
                return jarFile.getParent().getParent();
            }

            // Fallback: directorio actual
            return jarFile.getParent();

        } catch (Exception e) {
            System.err.println("⚠️ No se pudo detectar directorio de app: " + e.getMessage());
            return Paths.get(System.getProperty("user.dir"));
        }
    }

    /**
     * Crea un backup del directorio actual de la aplicación
     */
    public Path createBackup(ProgressCallback callback) throws UpdateException {
        if (!config.isKeepBackupsEnabled()) {
            System.out.println("⏭️ Backups deshabilitados, omitiendo...");
            return null;
        }

        try {
            // Crear nombre de backup con timestamp
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupName = "backup_" + UpdateConfig.getCurrentVersion() + "_" + timestamp;
            backupDir = UpdateConfig.getBackupDir().resolve(backupName);

            Files.createDirectories(backupDir);

            System.out.println("📦 Creando backup en: " + backupDir);

            // Copiar todos los archivos
            copyDirectory(currentAppDir, backupDir, callback);

            // Limpiar backups antiguos si excede el límite
            cleanOldBackups();

            System.out.println("✅ Backup creado exitosamente");
            return backupDir;

        } catch (IOException e) {
            throw new UpdateException(
                    UpdateException.ErrorType.BACKUP_ERROR,
                    "No se pudo crear backup",
                    e
            );
        }
    }

    /**
     * Instala la actualización reemplazando archivos
     */
    public void install(Path extractedDir, ProgressCallback callback) throws UpdateException {
        // ⭐ NUEVO: Validar que no estemos en modo desarrollo
        if (currentAppDir.toString().contains("target")) {
            throw new UpdateException(
                    UpdateException.ErrorType.INSTALL_ERROR,
                    "No se puede actualizar desde el IDE. Usa el .exe empaquetado."
            );
        }

        try {
            System.out.println("🔧 Preparando actualización...");

            // Verificar que existe la carpeta extraída
            if (!Files.exists(extractedDir) || !Files.isDirectory(extractedDir)) {
                throw new UpdateException(
                        UpdateException.ErrorType.INSTALL_ERROR,
                        "Directorio de actualización no existe: " + extractedDir
                );
            }

            // Buscar la carpeta raíz del ZIP
            Path sourceDir = findAppDirectory(extractedDir);

            if (sourceDir == null) {
                throw new UpdateException(
                        UpdateException.ErrorType.INSTALL_ERROR,
                        "No se encontró directorio válido de aplicación en el ZIP"
                );
            }

            // ⭐ NUEVO: En lugar de copiar directamente, crear script de actualización
            createUpdateScript(sourceDir, currentAppDir);

            System.out.println("✅ Script de actualización creado");

        } catch (IOException e) {
            throw new UpdateException(
                    UpdateException.ErrorType.INSTALL_ERROR,
                    "Error al preparar actualización",
                    e
            );
        }
    }

    /**
     * Crea un script de actualización que se ejecutará después de cerrar la app
     */
    private void createUpdateScript(Path sourceDir, Path targetDir) throws IOException {
        Path scriptPath = UpdateConfig.getDownloadDir().resolve("update.bat");

        StringBuilder script = new StringBuilder();
        script.append("@echo off\n");
        script.append("title Actualizando App Inventario\n");
        script.append("echo ========================================\n");
        script.append("echo   Actualizando App Inventario\n");
        script.append("echo ========================================\n");
        script.append("echo.\n");
        script.append("\n");
        script.append("REM Esperar a que la app se cierre\n");
        script.append("echo Esperando a que la aplicacion se cierre...\n");
        script.append("timeout /t 3 /nobreak >nul\n");
        script.append("\n");
        script.append("REM Copiar archivos nuevos\n");
        script.append("echo Copiando archivos nuevos...\n");
        script.append(String.format("robocopy \"%s\" \"%s\" /E /IS /IT /NFL /NDL /NJH /NJS /nc /ns /np\n",
                sourceDir.toString(), targetDir.toString()));
        script.append("if %%ERRORLEVEL%% LEQ 3 echo Archivos copiados exitosamente\n");
        script.append("\n");
        script.append("REM Limpiar archivos temporales\n");
        script.append("echo Limpiando archivos temporales...\n");
        script.append(String.format("rd /s /q \"%s\" 2>nul\n",
                UpdateConfig.getDownloadDir().toString()));
        script.append("\n");
        script.append("REM Reiniciar aplicación\n");
        script.append("echo Reiniciando aplicacion...\n");
        script.append(String.format("cd /d \"%s\"\n", targetDir.toString()));
        script.append("if exist \"AppInventario.exe\" (\n");
        script.append("    start \"\" \"AppInventario.exe\"\n");
        script.append("    echo Aplicacion reiniciada exitosamente\n");
        script.append(") else (\n");
        script.append("    echo ERROR: No se encontro AppInventario.exe\n");
        script.append("    pause\n");
        script.append(")\n");
        script.append("\n");
        script.append("echo.\n");
        script.append("echo Actualizacion completada!\n");
        script.append("timeout /t 2 >nul\n");
        script.append("\n");
        script.append("REM Auto-eliminar este script\n");
        script.append("(goto) 2>nul & del \"%%~f0\"\n");

        // Escribir script
        Files.writeString(scriptPath, script.toString());

        System.out.println("📝 Script creado en: " + scriptPath);
    }

    /**
     * Busca el directorio raíz de la aplicación dentro del ZIP extraído
     */
    private Path findAppDirectory(Path extractedDir) throws IOException {
        // Buscar carpeta que contenga "app" o archivos .exe
        try (Stream<Path> paths = Files.walk(extractedDir, 3)) {
            return paths
                    .filter(Files::isDirectory)
                    .filter(p -> {
                        try {
                            return Files.exists(p.resolve("app")) ||
                                    Files.list(p).anyMatch(f -> f.toString().endsWith(".exe"));
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .findFirst()
                    .orElse(extractedDir); // Si no encuentra, usar raíz
        }
    }

    /**
     * Copia recursivamente un directorio
     */
    private void copyDirectory(Path source, Path target, ProgressCallback callback) throws IOException {

        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            private int filesCopied = 0;

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                // ⭐ NUEVO: Saltar directorios excluidos
                if (shouldExcludeFromBackup(dir)) {
                    System.out.println("⏭️ Excluyendo directorio: " + dir.getFileName());
                    return FileVisitResult.SKIP_SUBTREE;
                }

                Path targetDir = target.resolve(source.relativize(dir));
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                // ⭐ NUEVO: Saltar archivos excluidos
                if (shouldExcludeFromBackup(file)) {
                    System.out.println("⏭️ Excluyendo: " + file.getFileName());
                    return FileVisitResult.CONTINUE;
                }

                Path targetFile = target.resolve(source.relativize(file));

                // No sobrescribir archivos de configuración del usuario
                if (shouldPreserveFile(file)) {
                    System.out.println("⏭️ Preservando: " + file.getFileName());
                } else {
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    filesCopied++;

                    if (callback != null && filesCopied % 10 == 0) {
                        callback.onProgress(filesCopied);
                    }
                }

                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Determina si un archivo debe EXCLUIRSE del backup
     */
    private boolean shouldExcludeFromBackup(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        String fullPath = file.toString().toLowerCase();

        // Excluir directorios de desarrollo
        if (fullPath.contains(File.separator + ".git" + File.separator) ||
                fullPath.contains(File.separator + "target" + File.separator) ||
                fullPath.contains(File.separator + "node_modules" + File.separator)) {
            return true;
        }

        // Excluir archivos ejecutables en uso
        if (fileName.endsWith(".exe") ||
                fileName.endsWith(".dll") ||
                fullPath.contains("runtime" + File.separator)) {
            return true;
        }

        return false;
    }

    /**
     * Determina si un archivo debe preservarse (no sobrescribirse)
     */
    private boolean shouldPreserveFile(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();

        // Preservar archivos de configuración del usuario
        return fileName.endsWith(".properties") ||
                fileName.endsWith(".config") ||
                fileName.contains("user") ||
                fileName.contains("settings");
    }



    /**
     * Restaura desde un backup en caso de error
     */
    public void rollback() throws UpdateException {
        if (backupDir == null || !Files.exists(backupDir)) {
            throw new UpdateException(
                    UpdateException.ErrorType.BACKUP_ERROR,
                    "No hay backup disponible para restaurar"
            );
        }

        try {
            System.out.println("⏮️ Restaurando desde backup: " + backupDir);

            // Eliminar archivos actuales (excepto configuración)
            deleteDirectory(currentAppDir, true);

            // Copiar desde backup
            copyDirectory(backupDir, currentAppDir, null);

            System.out.println("✅ Rollback completado");

        } catch (IOException e) {
            throw new UpdateException(
                    UpdateException.ErrorType.BACKUP_ERROR,
                    "Error al restaurar backup",
                    e
            );
        }
    }

    /**
     * Elimina backups antiguos manteniendo solo los últimos N
     */
    private void cleanOldBackups() throws IOException {
        try (Stream<Path> backups = Files.list(UpdateConfig.getBackupDir())) {
            backups
                    .filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().startsWith("backup_"))
                    .sorted(Comparator.comparing(this::getLastModifiedTime).reversed())
                    .skip(config.getMaxBackups())
                    .forEach(oldBackup -> {
                        try {
                            deleteDirectory(oldBackup, false);
                            System.out.println("🗑️ Backup antiguo eliminado: " +
                                    oldBackup.getFileName());
                        } catch (IOException e) {
                            System.err.println("No se pudo eliminar backup: " + e.getMessage());
                        }
                    });
        }
    }

    /**
     * Elimina un directorio recursivamente
     */
    private void deleteDirectory(Path directory, boolean preserveConfig) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                if (!preserveConfig || !shouldPreserveFile(file)) {
                    Files.delete(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                if (exc == null && !dir.equals(directory)) {
                    try {
                        Files.delete(dir);
                    } catch (DirectoryNotEmptyException e) {
                        // Ignorar si tiene archivos preservados
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Obtiene el timestamp de última modificación de un archivo
     */
    private long getLastModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0;
        }
    }


    /**
     * Ejecuta el script de actualización y cierra la app
     */
    public void executeUpdateAndRestart() throws UpdateException {
        try {
            Path scriptPath = UpdateConfig.getDownloadDir().resolve("update.bat");

            if (!Files.exists(scriptPath)) {
                throw new UpdateException(
                        UpdateException.ErrorType.INSTALL_ERROR,
                        "Script de actualización no encontrado"
                );
            }

            System.out.println("🚀 Ejecutando script de actualización...");
            System.out.println("📋 Script: " + scriptPath);

            // Ejecutar script en un proceso separado
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "start", "\"\"", scriptPath.toString());
            pb.start();

            // Esperar un momento
            Thread.sleep(1000);

            // Cerrar aplicación actual
            System.out.println("👋 Cerrando aplicación para actualizar...");
            System.exit(0);

        } catch (Exception e) {
            throw new UpdateException(
                    UpdateException.ErrorType.INSTALL_ERROR,
                    "Error al ejecutar actualización",
                    e
            );
        }
    }

    /**
     * Busca el archivo ejecutable de la aplicación
     */
    private Path findExecutable() {
        try {
            // Buscar directamente por nombre fijo
            Path exePath = currentAppDir.resolve("AppInventario.exe");

            if (Files.exists(exePath)) {
                return exePath;
            }

            // Fallback: buscar cualquier .exe con "AppInventario" en el nombre
            try (Stream<Path> files = Files.walk(currentAppDir, 2)) {
                return files
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".exe"))
                        .filter(p -> p.getFileName().toString().toLowerCase().contains("appinventario"))
                        .findFirst()
                        .orElse(null);
            }
        } catch (IOException e) {
            return null;
        }
    }

    public Path getCurrentAppDir() {
        return currentAppDir;
    }

    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(int itemsProcessed);
    }
}