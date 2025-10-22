# 🔐 Sistema de Licencias - Ariel Cardales

## 📋 Índice
1. [Descripción General](#descripción-general)
2. [Cómo Funciona](#cómo-funciona)
3. [Medidas de Seguridad](#medidas-de-seguridad)
4. [Tipos de Plan](#tipos-de-plan)
5. [Configuración Inicial](#configuración-inicial)
6. [Administración de Licencias](#administración-de-licencias)
7. [Limitaciones y Consideraciones](#limitaciones-y-consideraciones)

---

## 📖 Descripción General

El sistema de licencias de **Ariel Cardales** permite controlar remotamente el acceso al software, validando licencias mediante un archivo JSON hospedado en GitHub.

**Características principales:**
- ✅ Validación online contra JSON en GitHub
- ✅ Modo offline temporal (máx. 7 días sin conexión)
- ✅ Verificación de fecha real desde API externa (evita manipulación del reloj)
- ✅ Firma digital del JSON para prevenir modificaciones
- ✅ Cache local encriptado
- ✅ 3 planes: DEMO, BASE, FULL
- ✅ Alertas de expiración
- ✅ Log de validaciones

---

## ⚙️ Cómo Funciona

### Flujo de Validación

```
┌─────────────┐
│ App inicia  │
└──────┬──────┘
       │
       v
┌────────────────────┐
│ LicenciaManager    │ ←── Intenta validación ONLINE
│ validarLicencia()  │
└──────┬────────┬────┘
       │        │
  ✅ OK │        │ ❌ Falla
       │        │
       v        v
   ┌──────────────────┐
   │ Validar OFFLINE  │ ←── Usa cache local (máx 7 días)
   │ (cache local)    │
   └────────┬─────────┘
            │
       ✅ OK │  ❌ Falla
            │    │
            v    v
        ┌─────────────┐
        │ App funciona│ ❌ Cerrar app
        └─────────────┘
```

### Paso a Paso

1. **Inicio de la aplicación:**
   - `AppController.initialize()` llama a `LicenciaManager.validarLicencia()`

2. **Validación Online:**
   - Descarga `licencias.json` desde GitHub RAW
   - Verifica firma SHA-256 del JSON
   - Busca el `clienteId` configurado
   - Obtiene fecha real desde `worldtimeapi.org` (o backup APIs)
   - Compara con fecha de expiración

3. **Si falla Online (sin internet):**
   - Lee cache local encriptado (`~/.arielcardales/licencia.dat`)
   - Verifica que no hayan pasado más de 7 días desde última validación online
   - Si es válido, permite continuar en modo offline

4. **Resultado:**
   - ✅ Licencia válida → Carga la aplicación
   - ⚠ Por expirar (< 7 días) → Muestra advertencia
   - ❌ Expirada/inválida → Cierra la aplicación

---

## 🔒 Medidas de Seguridad

### 1. **Firma Digital del JSON** 🔐
- Cada archivo `licencias.json` tiene un campo `firma` con hash SHA-256
- El hash se calcula con: `JSON + SECRET_KEY`
- Si la firma no coincide, se detecta manipulación (pero no bloquea, solo registra)

**Script de firmado:**
```python
python generar_firma_licencias.py licencias.json
```

### 2. **Fecha desde API Externa** 🌐
- **NO usa** el reloj del sistema operativo (fácil de manipular)
- Obtiene fecha real desde:
  - `worldtimeapi.org` (principal)
  - `timeapi.io` (backup 1)
  - `worldclockapi.com` (backup 2)
- Si todas fallan, usa reloj local con log de advertencia

### 3. **Cache Encriptado** 🔑
- El archivo local `licencia.dat` está encriptado con XOR + Base64
- Usa `SECRET_KEY` como clave
- Dificulta (pero no imposibilita) manipulación manual

### 4. **Límite de Días Offline** ⏱
- Máximo 7 días sin validación online
- Después de 7 días, **OBLIGA** a conectarse a internet
- Previene uso perpetuo offline

### 5. **ID de Cliente Único** 🆔
- Cada instalación tiene un `CLIENTE_ID` único
- Se configura al instalar: DNI, CUIT, email, o UUID
- El JSON solo contiene licencias para IDs registrados

---

## 📊 Tipos de Plan

| Plan | Productos | Ventas | Exportar PDF/Excel | Métricas Avanzadas | Multi-Usuario | Backup Auto |
|------|-----------|--------|--------------------|--------------------|---------------|-------------|
| **DEMO** | 15 | 10 | ❌ | ❌ | ❌ | ❌ |
| **BASE** | ∞ | ∞ | ✅ | ❌ | ❌ | ❌ |
| **FULL** | ∞ | ∞ | ✅ | ✅ | ✅ | ✅ |

### Validar Acceso en el Código

```java
Licencia lic = LicenciaManager.getLicencia();

if (lic.permiteAcceso("exportar_pdf")) {
    // Permitir exportar PDF
} else {
    // Mostrar mensaje "Función no disponible en su plan"
}
```

**Funcionalidades validables:**
- `"metricas_avanzadas"`
- `"exportar_pdf"`
- `"exportar_excel"`
- `"multi_usuario"`
- `"backup_auto"`

---

## 🛠 Configuración Inicial

### 1. Configurar Cliente

**Archivo:** `LicenciaConfig.java`

```java
public static String CLIENTE_ID = "DEMO_CLIENT";  // ← CAMBIAR
public static String CLIENTE_NOMBRE = "Cliente Demo";  // ← CAMBIAR
```

**Al instalar en cliente:**
```java
LicenciaConfig.setClienteId("DNI_12345678");
LicenciaConfig.setClienteNombre("Juan Pérez");
```

### 2. Cambiar Clave Secreta (IMPORTANTE)

**Archivo:** `LicenciaConfig.java`

```java
public static final String SECRET_KEY = "ArielCardales2025_SecretKey_ChangeThis!";
// ⚠️ CAMBIAR ESTA CLAVE EN PRODUCCIÓN
```

**También cambiar en:**
- `generar_firma_licencias.py` línea 15

### 3. Subir JSON a GitHub

1. Edita `licencias.json` con los datos reales
2. Firma el JSON:
   ```bash
   python generar_firma_licencias.py licencias.json
   ```
3. Haz commit y push a GitHub:
   ```bash
   git add licencias.json
   git commit -m "Actualizar licencias"
   git push
   ```
4. Copia la URL RAW del archivo (botón "Raw" en GitHub)
5. Actualiza `LicenciaConfig.LICENCIAS_JSON_URL` con esa URL

---

## 👨‍💼 Administración de Licencias

### Agregar un Nuevo Cliente

1. Edita `licencias.json` y agrega:

```json
{
  "clienteId": "CLI_003_NUEVO",
  "nombre": "Nuevo Cliente SRL",
  "email": "nuevo@cliente.com",
  "estado": "ACTIVO",
  "plan": "BASE",
  "expira": "2026-12-31",
  "notas": "Plan básico anual"
}
```

2. **Firma el JSON:**
```bash
python generar_firma_licencias.py
```

3. **Sube a GitHub:**
```bash
git add licencias.json
git commit -m "Agregar cliente CLI_003_NUEVO"
git push
```

4. **Configura en la instalación del cliente:**
```java
LicenciaConfig.setClienteId("CLI_003_NUEVO");
```

### Suspender una Licencia

Cambia el estado a `"SUSPENDIDO"`:

```json
{
  "clienteId": "CLI_001",
  "estado": "SUSPENDIDO",
  ...
}
```

**Firma y sube a GitHub.** La próxima vez que valide, será bloqueado.

### Renovar una Licencia

Extiende la fecha de expiración:

```json
{
  "clienteId": "CLI_001",
  "expira": "2027-12-31",
  ...
}
```

**Firma y sube a GitHub.**

---

## ⚠️ Limitaciones y Consideraciones

### ❗ NO es 100% inviolable

Este sistema dificulta el pirateo, pero un usuario técnico con tiempo podría:
- Modificar el `CLIENTE_ID` en el código fuente
- Recompilar sin validación de licencias
- Manipular el cache local
- Simular respuestas de las APIs

### 🛡 Cómo mejorar la seguridad

Para un sistema más robusto:

1. **Ofuscar el código** con ProGuard o similar
2. **Firmar el JAR** con jarsigner (Java Code Signing)
3. **Usar AES-256** en vez de XOR para encriptar cache
4. **Validar hardware** (MAC address, UUID de disco)
5. **Servidor dedicado** en vez de GitHub (con autenticación)
6. **Licencias por hardware** (solo funciona en una máquina)

### 📌 Recomendaciones

- ✅ Usar para control básico de licencias
- ✅ Combinarlo con contrato legal (términos de uso)
- ✅ Monitorear logs de validación
- ✅ Cambiar `SECRET_KEY` regularmente
- ❌ No confiar 100% en seguridad técnica
- ❌ No usar para datos ultra-sensibles sin reforzar

---

## 📂 Estructura de Archivos

```
ArielCardales/
├── src/main/java/.../Licencia/
│   ├── Licencia.java              # Modelo de licencia
│   ├── LicenciaConfig.java        # Configuración y constantes
│   └── LicenciaManager.java       # Lógica de validación
│
├── licencias.json                 # JSON de licencias (subir a GitHub)
├── generar_firma_licencias.py     # Script para firmar JSON
│
└── ~/.arielcardales/              # Directorio del usuario
    ├── licencia.dat               # Cache encriptado
    └── validaciones.log           # Log de validaciones
```

---

## 🧪 Pruebas

### Probar con DEMO

1. En `LicenciaConfig.java`:
   ```java
   public static String CLIENTE_ID = "DEMO_CLIENT";
   ```

2. Ejecutar aplicación:
   ```bash
   mvn javafx:run
   ```

3. Verificar:
   - ✅ Aparece notificación "Licencia DEMO activa"
   - ✅ Sistema funciona con límites (15 productos, 10 ventas)

### Probar Modo Offline

1. Ejecutar la app CON internet (valida online)
2. Cerrar la app
3. **Desconectar internet**
4. Ejecutar la app → debe funcionar en modo offline
5. Verificar notificación "Licencia... (offline)"

### Probar Expiración

1. Editar `licencias.json`:
   ```json
   "expira": "2025-10-22"  // Mañana
   ```
2. Firmar y actualizar en GitHub
3. Ejecutar app → debe mostrar "7 días restantes" (o menos)

---

## 🆘 Soporte y Contacto

Para dudas sobre el sistema de licencias:
- 📧 Email: [tu-email]
- 📱 WhatsApp: [tu-número]

---

**Fecha de creación:** 2025-10-21
**Versión del sistema:** 1.0
**Autor:** Agus
