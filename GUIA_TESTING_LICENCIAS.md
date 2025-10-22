# 🧪 Guía de Testing del Sistema de Licencias

## 📋 Índice
1. [Preparación del Entorno](#1-preparación-del-entorno)
2. [Test 1: Modo DEMO](#test-1-modo-demo)
3. [Test 2: Plan BASE](#test-2-plan-base)
4. [Test 3: Plan FULL](#test-3-plan-full)
5. [Test 4: Modo Offline](#test-4-modo-offline)
6. [Test 5: Licencia Expirada](#test-5-licencia-expirada)
7. [Test 6: Cliente Suspendido](#test-6-cliente-suspendido)
8. [Test 7: Cliente No Registrado](#test-7-cliente-no-registrado)
9. [Test 8: Advertencia de Expiración](#test-8-advertencia-de-expiración)
10. [Verificar Logs](#verificar-logs)

---

## 1. 🛠 Preparación del Entorno

### Paso 1: Firmar el JSON de Licencias

```bash
cd C:\arielcardales
python generar_firma_licencias.py
```

**Salida esperada:**
```
[OK] Archivo firmado exitosamente
Firma: 6d1dd39d9dafa7fe5ba77ef02fad2124bb8f5550239c76c69d76f85a09710503
```

### Paso 2: Subir a GitHub

```bash
git add licencias.json
git commit -m "Actualizar licencias para testing"
git push
```

### Paso 3: Obtener URL RAW

1. Ve a GitHub: `https://github.com/agusArram/ArielCardales`
2. Clic en `licencias.json`
3. Clic en botón **"Raw"**
4. Copiar URL (debe terminar en `.../licencias.json`)

### Paso 4: Configurar URL en el Código

**Editar `LicenciaConfig.java` línea 15:**

```java
public static final String LICENCIAS_JSON_URL =
    "https://raw.githubusercontent.com/agusArram/ArielCardales/main/licencias.json";
    // ↑ TU URL REAL
```

### Paso 5: Compilar

```bash
mvn clean compile
```

---

## Test 1: 🧪 Modo DEMO

**Objetivo:** Verificar que la licencia DEMO funciona con límites

### Configuración

**Editar `cliente.properties`:**
```properties
cliente.id=DEMO_CLIENT
cliente.nombre=Cliente Demo
cliente.email=demo@ejemplo.com
```

### Ejecutar

```bash
mvn javafx:run
```

### Verificaciones

✅ **La app debe iniciar correctamente**

✅ **Debe aparecer notificación:**
```
ℹ Información de Licencia
✅ Licencia DEMO activa
Cliente: Cliente Demo
```

✅ **En consola debe aparecer:**
```
✓ Configuración de cliente cargada:
  - ID: DEMO_CLIENT
  - Nombre: Cliente Demo
  - Email: demo@ejemplo.com

✅ Validación online exitosa
```

✅ **Limitaciones del plan DEMO:**
- Máximo 15 productos
- Máximo 10 ventas
- NO puede exportar a PDF/Excel
- NO tiene métricas avanzadas

### Cómo Verificar Límites

1. Ir a Productos → Agregar Producto
2. Agregar 16 productos
3. El sistema debe **bloquear** al intentar agregar el #16

*(Nota: Esta validación la debes implementar en el código)*

---

## Test 2: 📦 Plan BASE

**Objetivo:** Verificar plan BASE con funcionalidades completas pero sin avanzadas

### Configuración

**Editar `cliente.properties`:**
```properties
cliente.id=CLI_001_BASE
cliente.nombre=Comercio Ejemplo SRL
cliente.email=comercio@ejemplo.com
```

### Ejecutar

```bash
mvn clean javafx:run
```

### Verificaciones

✅ **Debe iniciar sin notificación** (solo DEMO y offline muestran notif)

✅ **En consola:**
```
✓ Configuración de cliente cargada:
  - ID: CLI_001_BASE
  - Nombre: Comercio Ejemplo SRL

✅ Validación online exitosa
```

✅ **Permisos del plan BASE:**
- ✅ Productos ilimitados
- ✅ Ventas ilimitadas
- ✅ Exportar a PDF/Excel
- ❌ NO métricas avanzadas
- ❌ NO multi-usuario
- ❌ NO backup automático

### Probar Exportación

```java
// En el código, verificar:
Licencia lic = LicenciaManager.getLicencia();
if (lic.permiteAcceso("exportar_pdf")) {
    // Debe ser TRUE en BASE
}
if (lic.permiteAcceso("metricas_avanzadas")) {
    // Debe ser FALSE en BASE
}
```

---

## Test 3: 🚀 Plan FULL

**Objetivo:** Verificar plan FULL con todas las funcionalidades

### Configuración

**Opción A: Usar tu DNI (recomendado)**

1. Editar `licencias.json`:
   ```json
   {
     "clienteId": "DNI_12345678",  // ← TU DNI
     "nombre": "Tu Nombre",
     "email": "tu@email.com",
     "estado": "ACTIVO",
     "plan": "FULL",
     "expira": "2099-12-31"
   }
   ```

2. Firmar y subir a GitHub
3. Editar `cliente.properties`:
   ```properties
   cliente.id=DNI_12345678
   cliente.nombre=Tu Nombre
   cliente.email=tu@email.com
   ```

**Opción B: Usar cliente de ejemplo**

```properties
cliente.id=CLI_002_FULL
cliente.nombre=Empresa Premium SA
cliente.email=premium@ejemplo.com
```

### Ejecutar

```bash
mvn javafx:run
```

### Verificaciones

✅ **Permisos del plan FULL:**
- ✅ TODO ilimitado
- ✅ Exportar a PDF/Excel
- ✅ Métricas avanzadas
- ✅ Multi-usuario
- ✅ Backup automático

```java
Licencia lic = LicenciaManager.getLicencia();
lic.permiteAcceso("metricas_avanzadas");  // TRUE
lic.permiteAcceso("multi_usuario");       // TRUE
lic.permiteAcceso("backup_auto");         // TRUE
```

---

## Test 4: 📡 Modo Offline

**Objetivo:** Verificar que funciona sin internet hasta 7 días

### Preparación

1. **Ejecutar app CON internet** (valida online y guarda cache)
   ```bash
   mvn javafx:run
   ```

2. **Cerrar la app**

3. **Desconectar internet** (WiFi off o cable desconectado)

### Ejecutar

```bash
mvn javafx:run
```

### Verificaciones

✅ **Debe iniciar correctamente**

✅ **Notificación:**
```
ℹ Información de Licencia
✅ Licencia DEMO activa (offline)
```

✅ **En consola:**
```
⚠ Usando validación offline (cache local)
```

✅ **Archivo cache creado:**
```
C:\Users\TuUsuario\.arielcardales\licencia.dat
```

### Verificar Límite de 7 Días

**Simular 8 días sin internet:**

1. Modificar fecha del cache manualmente (archivo encriptado, difícil)
2. O esperar 8 días reales (no práctico)
3. O modificar `LicenciaConfig.MAX_DIAS_SIN_VALIDACION` a `1` para testing:

```java
public static final int MAX_DIAS_SIN_VALIDACION = 1;  // Temporal para testing
```

4. Ejecutar app 2 días después → debe **bloquear**

**Error esperado:**
```
❌ Hace más de 1 días sin validación online. Conecte a internet.
```

---

## Test 5: ⏰ Licencia Expirada

**Objetivo:** Verificar que bloquea licencias vencidas

### Configuración

**Editar `licencias.json`:**
```json
{
  "clienteId": "CLI_TEST_EXPIRADO",
  "nombre": "Cliente Test Expirado",
  "email": "test@expirado.com",
  "estado": "ACTIVO",
  "plan": "BASE",
  "expira": "2025-10-20",  // ← FECHA PASADA
  "notas": "Para testing de expiración"
}
```

**Firmar y subir a GitHub**

**Configurar app:**
```properties
cliente.id=CLI_TEST_EXPIRADO
```

### Ejecutar

```bash
mvn javafx:run
```

### Verificación

❌ **App debe cerrarse inmediatamente**

❌ **Diálogo de error:**
```
Licencia no válida
No se pudo validar la licencia

Posibles causas:
• Licencia expirada
• Cliente no registrado
• Sin conexión a internet por más de 7 días
```

✅ **En log:**
```
~/.arielcardales/validaciones.log
❌ Licencia expirada (fecha: 2025-10-20)
```

---

## Test 6: 🚫 Cliente Suspendido

**Objetivo:** Verificar bloqueo de clientes suspendidos

### Configuración

**Usar cliente suspendido del JSON:**
```properties
cliente.id=CLI_004_SUSPENDIDO
cliente.nombre=Cliente Moroso
```

### Ejecutar

```bash
mvn javafx:run
```

### Verificación

❌ **App debe cerrarse**

❌ **Diálogo:** "Licencia no válida"

✅ **En log:** Estado SUSPENDIDO detectado

---

## Test 7: ❓ Cliente No Registrado

**Objetivo:** Verificar error cuando el ID no existe en el JSON

### Configuración

```properties
cliente.id=CLI_999_NOEXISTE
cliente.nombre=Cliente Falso
```

### Ejecutar

```bash
mvn javafx:run
```

### Verificación

❌ **App debe cerrarse**

✅ **En log:**
```
❌ Cliente no encontrado en el sistema de licencias
```

---

## Test 8: ⚠ Advertencia de Expiración

**Objetivo:** Verificar notificación cuando quedan menos de 7 días

### Configuración

**Editar `licencias.json`:**
```json
{
  "clienteId": "CLI_TEST_EXPIRA",
  "expira": "2025-10-28",  // ← 7 días desde hoy (ajustar según fecha actual)
  "plan": "BASE",
  "estado": "ACTIVO"
}
```

**Firmar, subir, configurar:**
```properties
cliente.id=CLI_TEST_EXPIRA
```

### Ejecutar

```bash
mvn javafx:run
```

### Verificación

✅ **App debe iniciar**

⚠ **Notificación naranja:**
```
⚠ Licencia por expirar
Su licencia vence en 7 días.
Contacte al administrador para renovar.
```

---

## 📊 Verificar Logs

**Ubicación:**
```
Windows: C:\Users\TuUsuario\.arielcardales\validaciones.log
Linux: ~/.arielcardales/validaciones.log
```

**Contenido esperado:**

```
[2025-10-21T23:30:00] ✅ Validación online exitosa
[2025-10-21T23:30:00] Cliente: DEMO_CLIENT
[2025-10-21T23:30:00] Plan: DEMO
[2025-10-21T23:30:00] Expira: 2025-11-30
[2025-10-21T23:30:00] Días restantes: 40
[2025-10-21T23:30:05] Cache local guardado
```

---

## 🔍 Verificar Fecha Desde API Externa

**Objetivo:** Comprobar que NO usa el reloj del sistema

### Prueba

1. **Cambiar fecha del Windows** a 2030
2. **Ejecutar app**
3. **Verificar log:**
   ```
   Fecha real obtenida desde API: 2025-10-21  ← Debe ser la fecha REAL
   Fecha del sistema: 2030-01-01              ← Detecta manipulación
   ```

4. **La licencia debe validarse con la fecha REAL**, no la del sistema

---

## ✅ Checklist Completo de Testing

| Test | Configuración | Resultado Esperado | Estado |
|------|---------------|-------------------|--------|
| DEMO | `DEMO_CLIENT` | Inicia con notificación | ⬜ |
| BASE | `CLI_001_BASE` | Inicia sin notificación | ⬜ |
| FULL | `CLI_002_FULL` o tu DNI | Inicia con todos los permisos | ⬜ |
| Offline | Sin internet después de validar | Inicia con "(offline)" | ⬜ |
| Expirada | Fecha pasada | App se cierra | ⬜ |
| Suspendido | Estado SUSPENDIDO | App se cierra | ⬜ |
| No existe | ID falso | App se cierra | ⬜ |
| Por expirar | Menos de 7 días | Inicia con advertencia | ⬜ |
| Manipulación fecha | Cambiar reloj Windows | Usa fecha real de API | ⬜ |
| Cache > 7 días | Sin internet 8 días | App se cierra | ⬜ |

---

## 🐛 Depuración

### Ver Salida Completa

**Ejecutar con logs visibles:**
```bash
mvn javafx:run > output.log 2>&1
```

**Ver en tiempo real:**
```bash
tail -f ~/.arielcardales/validaciones.log
```

### Forzar Revalidación

**Borrar cache local:**
```bash
rm ~/.arielcardales/licencia.dat
```

**Próxima ejecución** → forzará validación online

---

## 📞 Soporte

Si algún test falla inesperadamente, revisar:

1. ✅ `licencias.json` subido a GitHub (URL RAW accesible)
2. ✅ JSON firmado correctamente
3. ✅ `cliente.id` coincide exactamente (case-sensitive)
4. ✅ Conexión a internet
5. ✅ APIs de fecha funcionando (worldtimeapi.org)
6. ✅ Logs en `~/.arielcardales/validaciones.log`

---

**Última actualización:** 2025-10-21
**Versión:** 1.0
