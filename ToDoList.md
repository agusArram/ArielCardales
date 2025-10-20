
# ✅ **ROADMAP Ariel Cardales — Desarrollo Progresivo**

📅 **Última actualización:** 2025-10-20  
🎯 **Meta general:** Profesionalizar el sistema de inventario hasta nivel vendible como producto completo (desktop → SaaS).  
💡 **Estrategia:** iterar en etapas cortas con entregables visuales y funcionales.

## 📘 Índice rápido
- [Etapa 1 — Núcleo Mejorado](#etapa-1--núcleo-mejorado-octnov-2025)
- [Etapa 2 — Ciclo de Inventario Completo](#etapa-2--ciclo-de-inventario-completo-dicene)
- [Etapa 3 — Profesionalización](#etapa-3--profesionalización-febmar-2026)


---

## 🧭 **Resumen General del Proyecto Ariel Cardales**

**Nombre del sistema:** Ariel Cardales – Gestión de Inventario  
**Versión actual:** 2.2.2 (MVP funcional, desktop standalone)  
**Desarrollado por:** Agus
**Base de datos:** PostgreSQL (Supabase)  
**Arquitectura:** MVC (DAO + Mapper + Controller)  
**Lenguaje:** Java 21 con JavaFX  
**Estado actual:** estable — en etapa de consolidación de métricas y UX

---

### 📋 **Descripción General**

El sistema **Ariel Cardales** permite administrar productos, ventas y métricas de rentabilidad para negocios minoristas.  
Está diseñado inicialmente para uso **local (desktop)**, con una estructura preparada para escalar a **modo SaaS**.

Cuenta con los módulos principales:

- 📦 **Inventario:** control de stock, precios, costos y categorías.
- 🧾 **Ventas:** registro de transacciones y clientes.
- 📊 **Métricas:** análisis de rentabilidad y desempeño de productos.
- 🧍‍♂️ **Clientes:** registro y seguimiento de historial de compras.
- 🧮 **Dashboard:** panel visual con márgenes, ganancias y productos más rentables.

El objetivo es convertirlo en una **solución comercializable**, con sincronización en la nube, modo offline y panel web complementario.

---

### 🧰 **Stack Técnico**
| Capa | Tecnología / Patrón | Descripción |
|------|---------------------|--------------|
| **Presentación (UI)** | JavaFX + CSS + FXML | Interfaz modular, estética moderna, compatible con ControlsFX |
| **Lógica de negocio** | Controladores JavaFX (`AppController`) + Servicios | Manejan interacciones entre vista y datos |
| **Acceso a datos** | DAO Pattern + HikariCP | Conexión eficiente a PostgreSQL (Supabase) |
| **Modelos** | Entidades Java (`Producto`, `Venta`, `Cliente`) | Clases POJO con propiedades observables |
| **Persistencia** | PostgreSQL + vistas SQL (`vInventario`, `vVentas`) | Consultas optimizadas con joins predefinidos |
| **Exportación** | PDF/Excel (OpenPDF + Apache POI) | Reportes generados desde interfaz |
| **Estilos** | `estilos.css` unificado | Tema beige-oscuro con tipografía legible y componentes suaves |

---

### 🧩 **Filosofía del Proyecto**

> “Primero funcional, luego hermoso, y finalmente vendible.”

Cada módulo se desarrolla bajo 3 principios:
1. **Modularidad:** cada clase hace una sola cosa (DAO, Mapper, Controller).
2. **Escalabilidad:** toda función debe poder extenderse a nube / multiusuario.
3. **Experiencia de usuario:** simple, clara y visualmente coherente.

---



## 🧩 **ETAPA 1 — Núcleo Mejorado (Oct–Nov 2025)**

**Objetivo:** reforzar la base técnica, mejorar UX y completar el flujo de ventas.

### 🧠 Base de Datos y Controladores
- [x] Entidad `Venta` y DAO ✅ 2025-10-14
- [x] ABM de productos completo ✅ 2025-10-14
- [x] CRUD de Categorías y Unidades ✅ 2025-10-14
- [ ] Incluir backup de DB y documentación de restauración

### 📁 1. Módulo de Clientes 🟢 (impacto alto)

- [ ] Crear tabla y DAO `ClienteDAO`
- [ ] CRUD completo desde interfaz (nombre, teléfono, email)
- [x] Asociar clientes a ventas ✅ 2025-10-20
- [ ]  Mostrar historial de compras del cliente
- [ ]  Validar duplicados por DNI / teléfono
- [x] Filtro de ventas por cliente ✅ 2025-10-20
- [ ] Agregar búsqueda por nombre o DNI directamente en la tabla

📌 _Mini objetivo:_ poder elegir cliente al vender y ver su historial.

---

### 📈 2. Dashboard y Estadísticas Mejoradas 🟢

- [x] Dashboard de métricas actual ✅ 2025-10-20
- [x] Panel de rentabilidad ✅ 2025-10-20
- [ ] Botón "Refrescar" en cada vista para recargar datos desde DB
- [ ] Agregar filtros por empleado y forma de pago
- [ ] Incluir gráficos (`BarChart` / `PieChart`)
- [ ]  Mostrar comparación de últimos 3 meses
- [ ]   Indicadores de alerta (stock bajo, ventas ↓, etc.)
- [ ]  Tooltip en métricas con explicación
- [ ]  Exportar estadísticas a PDF

📌 _Mini objetivo:_ un panel visual con evolución mensual + alertas.

---

### 🧮 3. Mejoras UX en Tablas

- [ ] Filtros dinámicos por nombre / categoría / precio
- [ ] Ícono de alerta para productos con stock bajo
- [ ] Colores condicionales (margen bajo = rojo)
- [ ] Búsqueda rápida en todas las vistas (`Ctrl+F` interno)

📌 _Mini objetivo:_ navegación más ágil y estética profesional.

---
### 🎨 4. Unificación Visual (UI/UX)

- [ ]  Unificar CSS global (`estilos.css`)
- [ ] Implementar iconos con `Ikonli`
- [ ] Transiciones suaves (`FadeTransition`)
- [ ] Reemplazar alertas con `Notifications` (ControlsFX)
- [ ] Tema oscuro opcional (variable global CSS)
- [ ] Rediseñar cabecera principal con logo + título dinámico


📌 _Mini objetivo:_ interfaz moderna, coherente y fluida.

---
## 🧾 **ETAPA 2 — Ciclo de Inventario Completo (Dic–Ene)**

**Objetivo:** completar el circuito _Compra → Stock → Venta → Rentabilidad_.

### 🧱 4.5. Optimización de Base de Datos
- [ ] Crear índices en columnas de búsqueda (nombre, categoría)
- [ ] Normalizar vistas SQL (`vInventario`, `vMovimientosStock`)
- [ ] Crear `vVentasDetalladas` (JOIN cliente, producto, empleado)

### 🧺 5. Módulo de Compras y Proveedores 🟠

- [ ] Crear DAOs y DB `ProveedorDAO` y `CompraDAO`
- [ ]  CRUD de proveedores
- [ ]  Registrar compras con items
- [ ] Actualizar stock y costo automáticamente
- [ ] Historial de compras por proveedor
- [ ] Exportar compras a PDF
- [ ] Validar precios negativos o stock incoherente al registrar compra


📌 _Mini objetivo:_ controlar origen de stock y costos reales.

---
### 🔄 6. Historial de Movimientos de Stock

- [ ] Crear vista `vMovimientosStock` (entradas/salidas/ajustes)
- [ ] Mostrar motivo de movimiento (venta, compra, ajuste)
- [ ] Filtros por tipo y fecha
- [ ] Botón para registrar ajustes manuales
- [ ] Incluir botón “Ver movimientos recientes” desde el dashboard


📌 _Mini objetivo:_ trazabilidad completa del inventario.

---

### 🧑‍💻 7. Gestión de Usuarios / Roles
- [ ] Crear tabla `usuario` (nombre, rol, password hash)
- [ ]  Pantalla de login inicial
- [ ] Control de permisos (admin / vendedor)
- [ ] Auditoría básica: “venta registrada por X usuario”
- [ ] Cifrar contraseñas con SHA-256 o BCrypt


📌 _Mini objetivo:_ seguridad básica y trazabilidad.

---
## ☁️ **ETAPA 3 — Profesionalización (Feb–Mar 2026)**

**Objetivo:** llevarlo a un nivel vendible y escalable.

### ☁️ 8. Sincronización y modo offline

- [ ] SQLite local + sincronización con Supabase
- [ ] Modo “feria” sin conexión
- [ ] Auto-sync al reconectarse

📌 _Mini objetivo:_ uso fluido sin internet.

---

### 📲 9. Dashboard Web / App Móvil

- [ ] API con Spring Boot o Supabase REST
- [ ] Panel web o app con Flutter
- [ ] Visualización de ventas y rentabilidad en tiempo real
- [ ] Sincronizar datos con API REST (modo lectura)


📌 _Mini objetivo:_ control remoto del negocio.

---
### 🏪 10. Soporte Multi-Sucursal

- [ ]  Tabla `sucursal`
- [ ]  Filtro de sesión por local
- [ ]  Consolidado general de ventas

📌 _Mini objetivo:_ base lista para expansión SaaS.

---
### 11. Sistema de Licencias y Planes (Mar–Abr 2026)

**Objetivo:** permitir control remoto de licencias, planes y expiración del software para cada cliente, sin servidor dedicado.

---
#### 🧠 Concepto General

El sistema utilizará un **archivo JSON centralizado en GitHub** como registro maestro de usuarios.  
Cada cliente se identifica por nombre o ID único, y su entrada define el estado de su licencia:

```json
{
  "usuarios": [
    {
      "nombre": "Ejemplo Cliente",
      "email": "cliente@ejemplo.com",
      "estado": "activo",
      "plan": "base",
      "expira": "2025-11-30"
    }
  ]
}
```

El programa valida automáticamente al iniciar:

- Si el usuario existe en el JSON
- Si su licencia está activa
- Si la fecha de expiración no venció

De acuerdo al resultado, habilita o bloquea funciones dentro del sistema.

---
##### ⚙️ Estructura técnica del sistema

|Componente|Descripción|
|---|---|
|**LicenciaManager.java**|Clase encargada de leer el JSON remoto (desde GitHub RAW) y validar el estado.|
|**Licencia.json local**|Copia local con la fecha de última verificación, para uso offline temporal.|
|**Config.json**|Permite guardar preferencias personalizadas del cliente (columnas visibles, nombres, tema visual).|
|**AppController**|Verifica al iniciar si `LicenciaManager.isActiva()` y restringe las funciones según el plan.|

---

##### 🧩 Tipos de plan

|Plan|Descripción|Límites|
|---|---|---|
|🧪 **Demo**|Modo de prueba, duración 15–30 días|15 productos, 10 ventas, métricas limitadas|
|⚙️ **Base**|Versión comercial básica|Módulos de inventario y ventas completos, rentabilidad resumida|
|🚀 **Full**|Versión completa y personalizable|Todas las funciones sin límites, configuración avanzada, dashboard extendido|

Cada plan define un conjunto de permisos que se cargan dinámicamente desde un archivo `permisos.json`.

---

##### 🧱 Arquitectura recomendada

- **DB multiusuario (una sola base Supabase):**
    - Usar campo `idUsuario` en todas las tablas principales.
    - Ideal para planes **Demo** y **Base** (mantenimiento centralizado).

- **DB individual (por conexión):**
    - Cada cliente “Full” tiene su propia base o esquema dedicado.
    - Permite independencia total y personalización.

---

#### 🪄 Futuras ampliaciones

- [ ] Validar firma digital del JSON para evitar modificaciones locales
- [ ] Panel web para administrar licencias (interfaz para administrador)
- [ ] Auto-desactivación si no puede validar por X días
- [ ] Sincronización del estado del plan con Supabase (versión SaaS)

---
#### ✅ Mini objetivo
> Implementar un sistema de licencias simple y remoto, capaz de activar, limitar o desactivar funciones sin tocar el código fuente.

---
#### 🧭 Pasos de desarrollo

- [ ] Crear clase `LicenciaManager` con lectura HTTP del JSON en GitHub
- [ ] Implementar estructura local (`licencia.json` + fecha última validación)
- [ ] Definir objeto `Licencia` con propiedades (`nombre`, `plan`, `estado`, `expira`)
- [ ] Crear método global `AppController.verificarLicencia()` que se ejecute al iniciar
- [ ] Integrar restricción visual (ej: deshabilitar botones o limitar cantidad de registros)
- [ ] Crear archivo `permisos.json` con reglas por plan
- [ ] Testear con JSON remoto simulado y casos de expiración
- [ ] (Opcional) Implementar hash o token de verificación por cliente

---
## ⚙️ **MEJORAS TÉCNICAS Y MANTENIMIENTO**

**Objetivo:** estabilidad, trazabilidad y soporte.

- [x] Evitar `SELECT *` ✅ 2025-10-20
- [x] Completar `module-info.java` ✅ 2025-10-20
- [x] Validaciones de stock y precios ✅ 2025-10-20
- [x] Interfaz `GenericDAO<T>` ✅ 2025-10-20
- [ ]  Centralizar configuración DB en `.env`
- [ ]  Implementar logs con SLF4J
- [ ]  Backup automático `.sql` semanal
- [ ]  Tests unitarios básicos (`ProductoDAO`, `VentaDAO`)
- [ ]  Logger centralizado para errores globales
- [ ] Implementar `ProductoService` intermedio (DAO + lógica)


📌 _Mini objetivo:_ robustez y mantenibilidad.

---
## 🎨 **UI / UX — Inspiración y Extras**

**Objetivo:** diferenciar visualmente y mejorar experiencia.

- [ ] Animación inicial de carga (splash screen simple)
- [ ]  Panel de configuración estética (colores / logo)
- [ ]  Gráficos miniatura en tarjetas métricas
- [ ] Animación al abrir secciones (fade o slide)
- [ ] Alertas contextuales (“2 productos sin stock”)

---
## 💬 **IDEAS  (Comercialización)**

**Objetivo:** preparar versión vendible/licenciable.

- [ ] Integración con Google Sheets o Excel online (sincronización liviana)
- [ ] Sistema de licencias / activación por clave
- [ ]  Demo de 15 días
- [ ] Registro de usuarios/clientes del software
- [ ]  Soporte automático (envío de logs / errores por mail)
- [ ] Reportes automáticos por correo semanal
- [ ] Mini asistente interno con notificaciones inteligentes

---

## 🧾 **Versionado del proyecto**
- [x] v2.2.2 — MVP estable ✅ 2025-10-20
- [ ] v2.3 — Cliente / Historial de ventas
- [ ] v2.4 — Dashboard y filtros avanzados
- [ ] v2.5 — Compras y proveedores
- [ ] v3.0 — Licencias y modo SaaS
