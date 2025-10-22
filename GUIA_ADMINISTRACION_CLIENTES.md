# 🔧 Guía de Administración de Clientes

## 📋 Índice
1. [Configurar Tu Licencia (FULL)](#1-configurar-tu-licencia-full)
2. [Generar Build para Cliente Nuevo](#2-generar-build-para-cliente-nuevo)
3. [Gestionar Múltiples Clientes](#3-gestionar-múltiples-clientes)
4. [Administrar Licencias en GitHub](#4-administrar-licencias-en-github)
5. [Conexión a DB por Cliente](#5-conexión-a-db-por-cliente)

---

## 1. 🏠 Configurar Tu Licencia (FULL)

### Paso 1: Agregar tu DNI al JSON

**Editar `licencias.json`:**

```json
{
  "clienteId": "DNI_12345678",  // ← TU DNI REAL
  "nombre": "Tu Nombre Completo",
  "email": "tu@email.com",
  "estado": "ACTIVO",
  "plan": "FULL",
  "expira": "2099-12-31",  // ← Licencia perpetua
  "notas": "Licencia FULL para desarrollador"
}
```

### Paso 2: Firmar el JSON

```bash
python generar_firma_licencias.py
```

### Paso 3: Subir a GitHub

```bash
git add licencias.json
git commit -m "Agregar licencia FULL de desarrollador"
git push
```

### Paso 4: Configurar tu proyecto local

**Editar `src/main/resources/cliente.properties`:**

```properties
# Tu configuración personal (FULL)
cliente.id=DNI_12345678
cliente.nombre=Tu Nombre
cliente.email=tu@email.com
```

### Paso 5: Probar

```bash
mvn clean javafx:run
```

Deberías ver:
```
✓ Configuración de cliente cargada:
  - ID: DNI_12345678
  - Nombre: Tu Nombre
  - Email: tu@email.com

[INFO] Licencia FULL activa
```

---

## 2. 📦 Generar Build para Cliente Nuevo

### Opción A: Usando Script Automático (Recomendado)

```bash
# Sintaxis:
generar_build_cliente.bat CLIENTE_ID "NOMBRE" EMAIL

# Ejemplos:
generar_build_cliente.bat CLI_001_BASE "Comercio La Esperanza" comercio@ejemplo.com
generar_build_cliente.bat DNI_87654321 "Juan Pérez" juan@gmail.com
generar_build_cliente.bat CLI_002_FULL "Empresa Premium" premium@empresa.com
```

**El script automáticamente:**
1. ✅ Actualiza `cliente.properties`
2. ✅ Ejecuta `mvn clean package`
3. ✅ Renombra el JAR con el ID del cliente
4. ✅ Te dice qué archivos copiar

**Resultado:**
```
target/
├── ArielCardales-CLI_001_BASE-1.0.0.jar  ← JAR personalizado
└── libs/                                  ← Dependencias (copiar también)
```

### Opción B: Manual

1. **Editar `cliente.properties`:**
   ```properties
   cliente.id=CLI_001_BASE
   cliente.nombre=Comercio La Esperanza
   cliente.email=comercio@ejemplo.com
   ```

2. **Compilar:**
   ```bash
   mvn clean package -DskipTests
   ```

3. **Copiar al cliente:**
   - `target/ArielCardales-1.0.0.jar`
   - `target/libs/` (toda la carpeta)

4. **Instrucción para el cliente:**
   ```bash
   java -jar ArielCardales-1.0.0.jar
   ```

---

## 3. 🗂 Gestionar Múltiples Clientes

### Escenario: Tienes 5 clientes, cada uno con su propia DB

#### Estructura Recomendada

```
C:\Proyectos\
│
├── ArielCardales-DEV/           ← Tu versión de desarrollo (FULL)
│   └── cliente.properties → DNI_TU_DNI
│
├── ArielCardales-Cliente1/      ← Build para Cliente 1 (BASE)
│   └── cliente.properties → CLI_001_BASE
│
├── ArielCardales-Cliente2/      ← Build para Cliente 2 (FULL)
│   └── cliente.properties → CLI_002_FULL
│
├── ArielCardales-Cliente3/      ← Build para Cliente 3 (BASE)
│   └── cliente.properties → CLI_003_BASE
│
└── ArielCardales-DEMO/          ← Demo para mostrar
    └── cliente.properties → DEMO_CLIENT
```

#### Workflow de Generación

**Para cada cliente:**

1. **Configurar DB en cada proyecto:**

   Editar `src/main/java/.../DAO/Database.java`:

   ```java
   // Cliente 1 (BASE) - DB compartida en Supabase
   static {
       DB_URL = "jdbc:postgresql://tu-supabase.com:5432/db_cliente1";
       DB_USER = "cliente1_user";
       DB_PASSWORD = System.getenv("PG_PASSWORD_CLI1");
   }
   ```

   O mejor aún, usar **variables de entorno** por proyecto en IntelliJ:
   - Run → Edit Configurations
   - Environment variables:
     ```
     PG_URL=jdbc:postgresql://supabase.com:5432/db_cliente1
     PG_USER=cliente1_user
     PG_PASSWORD=password_cliente1
     ```

2. **Generar JAR personalizado:**

   En cada carpeta de proyecto:
   ```bash
   generar_build_cliente.bat CLI_001_BASE "Cliente 1" cliente1@email.com
   ```

3. **Entregar al cliente:**
   - JAR personalizado
   - Carpeta `libs/`
   - Instrucciones de ejecución
   - (Opcional) Script `.bat` para facilitar inicio

#### Ejemplo de Script de Inicio para Cliente

**Crear `Iniciar_ArielCardales.bat`:**

```batch
@echo off
echo Iniciando Ariel Cardales - Comercio La Esperanza...
echo.

java -jar ArielCardales-CLI_001_BASE-1.0.0.jar

if errorlevel 1 (
    echo.
    echo Error al iniciar la aplicacion
    pause
)
```

---

## 4. 🌐 Administrar Licencias en GitHub

### Agregar Nuevo Cliente

1. **Editar `licencias.json`:**

```json
{
  "clienteId": "CLI_005_BASE",
  "nombre": "Nuevo Cliente SRL",
  "email": "nuevo@cliente.com",
  "estado": "ACTIVO",
  "plan": "BASE",
  "expira": "2026-12-31",
  "notas": "Plan BASE anual - contacto: Juan 3512-123456"
}
```

2. **Firmar:**
```bash
python generar_firma_licencias.py
```

3. **Subir:**
```bash
git add licencias.json
git commit -m "Agregar cliente CLI_005_BASE"
git push
```

### Suspender/Reactivar Cliente

**Suspender (por falta de pago):**

```json
{
  "clienteId": "CLI_003_BASE",
  "estado": "SUSPENDIDO",  // ← Cambiar a SUSPENDIDO
  ...
}
```

Firmar y subir → el cliente NO podrá usar el sistema.

**Reactivar:**

```json
{
  "estado": "ACTIVO",  // ← Volver a ACTIVO
  ...
}
```

### Renovar Licencia

```json
{
  "clienteId": "CLI_001_BASE",
  "expira": "2027-12-31",  // ← Extender 1 año más
  ...
}
```

### Cambiar Plan (Upgrade/Downgrade)

**Upgrade de BASE → FULL:**

```json
{
  "clienteId": "CLI_001_BASE",
  "plan": "FULL",  // ← Cambiar plan
  "expira": "2027-12-31",
  "notas": "Upgrade a FULL - pagado el 2025-10-21"
}
```

**Downgrade de FULL → BASE:**

```json
{
  "plan": "BASE",
  "notas": "Downgrade solicitado por el cliente"
}
```

---

## 5. 🗄 Conexión a DB por Cliente

### Opción 1: Variables de Entorno (Recomendado)

**En cada proyecto de IntelliJ:**

1. Run → Edit Configurations
2. Application → ArielCardales
3. Environment variables:

```
PG_URL=jdbc:postgresql://tu-supabase.com:5432/db_cliente1?sslmode=require
PG_USER=cliente1_user
PG_PASSWORD=password_seguro_cliente1
PG_POOL_SIZE=5
```

**Ventaja:** No tocas el código, solo cambias variables.

### Opción 2: Archivo `database.properties` por Cliente

**Crear `src/main/resources/database.properties`:**

```properties
# Configuración DB Cliente 1
db.url=jdbc:postgresql://tu-supabase.com:5432/db_cliente1?sslmode=require
db.user=cliente1_user
db.password=password_cliente1
db.pool.size=5
```

**Modificar `Database.java` para leer del archivo:**

```java
static {
    try {
        Properties props = new Properties();
        InputStream input = Database.class.getResourceAsStream("/database.properties");
        props.load(input);

        DB_URL = props.getProperty("db.url");
        DB_USER = props.getProperty("db.user");
        DB_PASSWORD = props.getProperty("db.password");
    } catch (Exception e) {
        // Usar valores por defecto
    }
}
```

### Opción 3: Base de Datos por Plan

**Arquitectura:**

- **Plan DEMO:** DB compartida con límites
- **Plan BASE:** DB compartida multicliente (con `cliente_id` en todas las tablas)
- **Plan FULL:** DB dedicada por cliente

**Ejemplo de tabla en DB compartida:**

```sql
CREATE TABLE producto (
    id SERIAL PRIMARY KEY,
    cliente_id VARCHAR(50) NOT NULL,  -- ← ID del cliente
    nombre VARCHAR(100),
    precio NUMERIC(10,2),
    ...
);

CREATE INDEX idx_producto_cliente ON producto(cliente_id);
```

**DAO modificado:**

```java
public List<Producto> findAll() throws SQLException {
    String sql = "SELECT * FROM producto WHERE cliente_id = ?";
    // Usar LicenciaConfig.CLIENTE_ID para filtrar
}
```

---

## 📋 Checklist Completo

### Al Generar Build para Nuevo Cliente

- [ ] Cliente agregado a `licencias.json` con ID, plan y fecha
- [ ] JSON firmado con `python generar_firma_licencias.py`
- [ ] JSON subido a GitHub (`git push`)
- [ ] URL RAW copiada y configurada en `LicenciaConfig.java`
- [ ] `cliente.properties` configurado con ID del cliente
- [ ] DB configurada (variables de entorno o properties)
- [ ] Build generado: `mvn clean package` o script `.bat`
- [ ] JAR y `libs/` copiados para entregar
- [ ] Script de inicio `.bat` creado (opcional)
- [ ] Probado localmente antes de entregar

### Al Entregar al Cliente

- [ ] JAR personalizado
- [ ] Carpeta `libs/` completa
- [ ] Script de inicio `.bat`
- [ ] Instrucciones de instalación
- [ ] Contacto para soporte
- [ ] Verificar que su `clienteId` esté en GitHub

---

## 🆘 Problemas Comunes

### Error: "Cliente no encontrado en el sistema de licencias"

**Causa:** El `cliente.id` en `cliente.properties` NO existe en `licencias.json`

**Solución:**
1. Verificar `cliente.properties` → ¿qué ID tiene?
2. Verificar `licencias.json` → ¿ese ID existe?
3. Firmar y subir JSON si falta el cliente

### Error: "Licencia expirada"

**Causa:** La fecha `expira` es anterior a hoy

**Solución:**
1. Editar `licencias.json` → extender fecha
2. Firmar y subir
3. Cliente debe cerrar y reabrir app

### Error: "Hace más de 7 días sin validación online"

**Causa:** Cliente sin internet por más de 7 días

**Solución:**
- Cliente debe conectarse a internet
- Si es necesario, aumentar `MAX_DIAS_SIN_VALIDACION` en `LicenciaConfig`

### App se cierra inmediatamente

**Causa:** Validación de licencia falla

**Solución:**
1. Ver logs en `~/.arielcardales/validaciones.log`
2. Verificar conexión a internet
3. Verificar que GitHub esté accesible
4. Verificar `cliente.properties` bien configurado

---

## 📞 Contacto

**Desarrollador:** Agus
**Email:** [tu-email]
**GitHub:** [tu-github]

---

**Última actualización:** 2025-10-21
