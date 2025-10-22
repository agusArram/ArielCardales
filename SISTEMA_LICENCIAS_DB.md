# 🔐 Sistema de Licencias con Base de Datos

## 📋 Índice
1. [Descripción General](#descripción-general)
2. [Cómo Funciona](#cómo-funciona)
3. [Ventajas del Nuevo Sistema](#ventajas-del-nuevo-sistema)
4. [Configuración Inicial](#configuración-inicial)
5. [Administración de Licencias](#administración-de-licencias)
6. [Tipos de Plan](#tipos-de-plan)

---

## 📖 Descripción General

El sistema de licencias de **Ariel Cardales** ahora utiliza **Supabase PostgreSQL** para validar licencias de forma instantánea y eficiente.

**Características principales:**
- ⚡ **Extremadamente rápido**: Una sola consulta SQL (<100ms) vs 5-20 segundos del sistema anterior
- 🔒 **Seguro**: Base de datos protegida con credenciales, más difícil de manipular
- 🎯 **Simple**: Sin cache, sin encriptación, sin APIs externas
- 🏗️ **Consistente**: Todo en la misma base de datos que el resto de la aplicación
- 📊 **Centralizado**: Administración desde Supabase o cualquier cliente SQL

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
│ LicenciaManager    │
│ validarLicencia()  │
└──────┬─────────────┘
       │
       v
┌────────────────────┐
│ Consulta SQL:      │
│ SELECT * FROM      │
│ licencia           │
│ WHERE dni = ?      │
└──────┬─────────────┘
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

2. **Consulta a la base de datos:**
   - `LicenciaDAO.findById(dni)` ejecuta una consulta SQL
   - Busca en la tabla `licencia` por el DNI configurado en `cliente.properties`

3. **Validación:**
   - Verifica que `estado = 'ACTIVO'`
   - Compara `fecha_expiracion` con la fecha actual
   - Si todo es válido, permite continuar

4. **Resultado:**
   - ✅ Licencia válida → Carga la aplicación
   - ⚠ Por expirar (< 7 días) → Muestra advertencia y continúa
   - ❌ Expirada/inválida → Cierra la aplicación

**Tiempo total de validación: <100ms** (vs 5-20 segundos del sistema anterior)

---

## 🚀 Ventajas del Nuevo Sistema

### Comparación con el Sistema Anterior

| Aspecto | Sistema Anterior (GitHub) | Sistema Nuevo (Base de Datos) |
|---------|---------------------------|-------------------------------|
| **Velocidad** | 5-20 segundos | <100 milisegundos |
| **Consultas HTTP** | 4-6 requests (GitHub + APIs tiempo) | 1 consulta SQL |
| **Modo offline** | Cache encriptado (7 días) | No necesario (siempre online) |
| **Complejidad** | 400+ líneas de código | 100 líneas de código |
| **Dependencias** | GitHub API + worldtimeapi.org | Solo PostgreSQL (que ya usabas) |
| **Seguridad** | Firma SHA-256 + encriptación XOR | Credenciales de DB (más seguro) |
| **Administración** | Editar JSON + script Python + git push | INSERT/UPDATE directo en Supabase |

---

## 🛠 Configuración Inicial

### 1. Crear Tabla en Supabase

Ejecuta el script SQL que está al final de `CrearDB.md`:

```sql
-- Crear tipos ENUM
CREATE TYPE estado_licencia AS ENUM ('ACTIVO', 'SUSPENDIDO', 'EXPIRADO', 'DEMO');
CREATE TYPE plan_licencia AS ENUM ('DEMO', 'BASE', 'FULL');

-- Crear tabla licencia
CREATE TABLE licencia (
    id bigserial PRIMARY KEY,
    dni text NOT NULL UNIQUE,
    nombre citext NOT NULL,
    email citext,
    estado estado_licencia NOT NULL DEFAULT 'DEMO',
    plan plan_licencia NOT NULL DEFAULT 'DEMO',
    fecha_expiracion date NOT NULL,
    notas text,
    createdAt timestamptz NOT NULL DEFAULT now(),
    updatedAt timestamptz NOT NULL DEFAULT now()
);

-- Datos de ejemplo
INSERT INTO licencia (dni, nombre, email, estado, plan, fecha_expiracion, notas) VALUES
    ('DEMO_CLIENT', 'Cliente Demo', 'demo@ejemplo.com', 'DEMO', 'DEMO', CURRENT_DATE + INTERVAL '15 days', 'Licencia de demostración'),
    ('46958104', 'Agustin desarrollador', 'agus@ejemplo.com', 'ACTIVO', 'FULL', '2030-12-31', 'Desarrollador');
```

### 2. Configurar DNI del Cliente

**Archivo:** `src/main/resources/cliente.properties`

```properties
# ID único del cliente (DNI como identificador)
cliente.id=46958104

# Nombre del cliente
cliente.nombre=Agustin desarrollador

# Email del cliente
cliente.email=agus@ejemplo.com
```

**Al instalar en un cliente nuevo:**
1. Edita `cliente.properties` con el DNI del cliente
2. Compila el proyecto: `mvn clean package`
3. El JAR generado estará configurado para ese cliente

---

## 👨‍💼 Administración de Licencias

### Agregar un Nuevo Cliente

Opción 1: **Desde Supabase Dashboard** (más fácil)
1. Abre tu proyecto en Supabase
2. Ve a Table Editor → licencia → Insert row
3. Completa los campos:
   - `dni`: DNI del cliente (ej: "12345678")
   - `nombre`: Nombre del cliente
   - `email`: Email del cliente
   - `estado`: ACTIVO
   - `plan`: BASE o FULL
   - `fecha_expiracion`: Fecha de expiración (ej: 2026-12-31)
4. Click en Save

Opción 2: **Con SQL**
```sql
INSERT INTO licencia (dni, nombre, email, estado, plan, fecha_expiracion, notas)
VALUES (
    '12345678',
    'Nuevo Cliente SRL',
    'nuevo@cliente.com',
    'ACTIVO',
    'BASE',
    '2026-12-31',
    'Plan básico anual'
);
```

### Suspender una Licencia

```sql
UPDATE licencia
SET estado = 'SUSPENDIDO'
WHERE dni = '12345678';
```

**Efecto:** La próxima vez que el cliente inicie la app, será bloqueado.

### Renovar una Licencia

```sql
UPDATE licencia
SET fecha_expiracion = '2027-12-31'
WHERE dni = '12345678';
```

### Cambiar Plan de Licencia

```sql
UPDATE licencia
SET plan = 'FULL'
WHERE dni = '12345678';
```

### Ver Licencias por Expirar (próximos 30 días)

```sql
SELECT dni, nombre, plan, fecha_expiracion,
       (fecha_expiracion - CURRENT_DATE) as dias_restantes
FROM licencia
WHERE estado = 'ACTIVO'
AND fecha_expiracion BETWEEN CURRENT_DATE AND (CURRENT_DATE + INTERVAL '30 days')
ORDER BY fecha_expiracion ASC;
```

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

## 🔧 Archivos Modificados

### Nuevos Archivos
- ✅ `LicenciaDAO.java` - DAO para gestión de licencias
- ✅ Sección en `CrearDB.md` - Script SQL para crear tabla

### Archivos Modificados
- ✅ `LicenciaManager.java` - Simplificado de 400 a 100 líneas
- ✅ `LicenciaConfig.java` - Eliminadas URLs y configuración de APIs
- ✅ `Mapper.java` - Agregado método `getLicencia()`
- ✅ `AppController.java` - Eliminada referencia a modo offline

### Archivos Eliminados
- ❌ `licencias.json`
- ❌ `generar_firma_licencias.py`
- ❌ `SISTEMA_LICENCIAS.md` (anterior)
- ❌ `GUIA_TESTING_LICENCIAS.md`
- ❌ `GUIA_ADMINISTRACION_CLIENTES.md`

---

## 🧪 Pruebas

### Probar con DEMO

1. En `cliente.properties`:
   ```properties
   cliente.id=DEMO_CLIENT
   ```

2. Asegúrate de que existe el registro en la base de datos:
   ```sql
   SELECT * FROM licencia WHERE dni = 'DEMO_CLIENT';
   ```

3. Ejecutar aplicación:
   ```bash
   mvn javafx:run
   ```

4. Verificar:
   - ✅ Aparece notificación "Licencia DEMO activa"
   - ✅ Sistema funciona con límites (15 productos, 10 ventas)

### Probar con Tu DNI

1. En `cliente.properties`:
   ```properties
   cliente.id=46958104
   ```

2. Ejecutar aplicación:
   ```bash
   mvn javafx:run
   ```

3. Verificar:
   - ✅ Licencia FULL activa
   - ✅ Sin notificaciones (porque no es DEMO y no está por expirar)

### Probar Licencia Expirada

1. Actualiza la fecha de expiración a ayer:
   ```sql
   UPDATE licencia
   SET fecha_expiracion = CURRENT_DATE - INTERVAL '1 day'
   WHERE dni = 'DEMO_CLIENT';
   ```

2. Ejecutar aplicación
3. Verificar:
   - ❌ Muestra error "Licencia expirada"
   - ❌ La aplicación se cierra

---

## 📈 Métricas de Mejora

### Rendimiento

- **Tiempo de inicio anterior:** 5-20 segundos
- **Tiempo de inicio nuevo:** <1 segundo
- **Mejora:** 5-20x más rápido

### Simplicidad

- **Líneas de código anterior:** 400+ líneas
- **Líneas de código nuevo:** 100 líneas
- **Reducción:** 75% menos código

### Confiabilidad

- **Dependencias externas anterior:** GitHub + 3 APIs de tiempo
- **Dependencias externas nuevo:** 0 (usa DB existente)
- **Puntos de fallo:** De 4-5 a 1

---

## 🎯 Casos de Uso

### Desarrollador: Licencia Permanente
```sql
INSERT INTO licencia (dni, nombre, email, estado, plan, fecha_expiracion)
VALUES ('46958104', 'Agustin', 'agus@ejemplo.com', 'ACTIVO', 'FULL', '2030-12-31');
```

### Cliente Demo: 15 días
```sql
INSERT INTO licencia (dni, nombre, email, estado, plan, fecha_expiracion)
VALUES ('DEMO_CLIENT', 'Demo', 'demo@ejemplo.com', 'DEMO', 'DEMO', CURRENT_DATE + INTERVAL '15 days');
```

### Cliente Comercial: Plan BASE anual
```sql
INSERT INTO licencia (dni, nombre, email, estado, plan, fecha_expiracion)
VALUES ('12345678', 'Comercio SRL', 'comercio@ejemplo.com', 'ACTIVO', 'BASE', CURRENT_DATE + INTERVAL '1 year');
```

### Cliente Premium: Plan FULL mensual
```sql
INSERT INTO licencia (dni, nombre, email, estado, plan, fecha_expiracion)
VALUES ('87654321', 'Empresa SA', 'empresa@ejemplo.com', 'ACTIVO', 'FULL', CURRENT_DATE + INTERVAL '1 month');
```

---

## 🆘 Soporte

Para dudas sobre el sistema de licencias:
- 📧 Email: [tu-email]
- 📱 WhatsApp: [tu-número]

---

**Fecha de migración:** 2025-10-22
**Versión del sistema:** 2.0 (Base de datos)
**Autor:** Agus
