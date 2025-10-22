@echo off
REM ============================================================================
REM Script para generar JAR personalizado por cliente
REM ============================================================================
REM Uso: generar_build_cliente.bat CLIENTE_ID NOMBRE_CLIENTE EMAIL
REM Ejemplo: generar_build_cliente.bat CLI_001_BASE "Comercio SRL" comercio@ejemplo.com
REM ============================================================================

setlocal enabledelayedexpansion

REM Colores para Windows (opcional)
set INFO=[INFO]
set ERROR=[ERROR]
set OK=[OK]

echo ============================================================================
echo    GENERADOR DE BUILD POR CLIENTE - Ariel Cardales
echo ============================================================================
echo.

REM --- Verificar parámetros ---
if "%~1"=="" (
    echo %ERROR% Falta el ID del cliente
    echo.
    echo Uso: %0 CLIENTE_ID NOMBRE EMAIL
    echo Ejemplo: %0 CLI_001_BASE "Comercio SRL" comercio@ejemplo.com
    echo.
    pause
    exit /b 1
)

set CLIENTE_ID=%~1
set CLIENTE_NOMBRE=%~2
set CLIENTE_EMAIL=%~3

if "%CLIENTE_NOMBRE%"=="" set CLIENTE_NOMBRE=Cliente Sin Nombre
if "%CLIENTE_EMAIL%"=="" set CLIENTE_EMAIL=sin-email@ejemplo.com

echo %INFO% Configurando build para:
echo   - ID:     %CLIENTE_ID%
echo   - Nombre: %CLIENTE_NOMBRE%
echo   - Email:  %CLIENTE_EMAIL%
echo.

REM --- Modificar cliente.properties ---
echo %INFO% Actualizando cliente.properties...

set PROPERTIES_FILE=src\main\resources\cliente.properties

(
echo # ============================================================================
echo # CONFIGURACION DEL CLIENTE
echo # ============================================================================
echo # Generado automaticamente por generar_build_cliente.bat
echo # Fecha: %date% %time%
echo # ============================================================================
echo.
echo cliente.id=%CLIENTE_ID%
echo cliente.nombre=%CLIENTE_NOMBRE%
echo cliente.email=%CLIENTE_EMAIL%
echo.
echo # ============================================================================
echo # ESTE ARCHIVO FUE CONFIGURADO PARA: %CLIENTE_NOMBRE%
echo # ============================================================================
) > %PROPERTIES_FILE%

echo %OK% cliente.properties actualizado
echo.

REM --- Limpiar builds anteriores ---
echo %INFO% Limpiando builds anteriores...
call mvn clean
if errorlevel 1 (
    echo %ERROR% Error al ejecutar mvn clean
    pause
    exit /b 1
)
echo.

REM --- Compilar proyecto ---
echo %INFO% Compilando proyecto...
call mvn package -DskipTests
if errorlevel 1 (
    echo %ERROR% Error al compilar el proyecto
    pause
    exit /b 1
)
echo.

REM --- Renombrar JAR ---
set JAR_ORIGINAL=target\ArielCardales-1.0.0.jar
set JAR_CLIENTE=target\ArielCardales-%CLIENTE_ID%-1.0.0.jar

if exist "%JAR_ORIGINAL%" (
    echo %INFO% Renombrando JAR...
    copy "%JAR_ORIGINAL%" "%JAR_CLIENTE%"
    echo %OK% JAR generado: %JAR_CLIENTE%
) else (
    echo %ERROR% No se encontró el JAR compilado
    pause
    exit /b 1
)

echo.
echo ============================================================================
echo    BUILD COMPLETADO EXITOSAMENTE
echo ============================================================================
echo.
echo %OK% JAR listo para: %CLIENTE_NOMBRE%
echo %OK% Archivo: %JAR_CLIENTE%
echo.
echo Proximos pasos:
echo 1. Copiar %JAR_CLIENTE% al cliente
echo 2. Copiar carpeta target\libs\ (dependencias)
echo 3. Verificar que %CLIENTE_ID% este en licencias.json
echo 4. Cliente debe ejecutar: java -jar ArielCardales-%CLIENTE_ID%-1.0.0.jar
echo.
pause
