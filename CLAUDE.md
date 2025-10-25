# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

JavaFX 21 desktop application for inventory management with PostgreSQL database (hosted on Supabase). The application supports product catalog management, sales tracking, variant products (color/size), and export functionality (Excel/PDF). It includes an auto-update system that checks GitHub releases.

**Main Class**: `SORT_PROYECTS.AppInventario.Launcher`
**Entry Point**: `Launcher.main()` → `App.launch()` → loads `/fxml/principal.fxml`

## Build & Run Commands

### Build
```bash
mvn clean package
```
This compiles the application and copies dependencies to `target/libs/`.

### Run (Development)
```bash
mvn clean javafx:run
```

### Run (Packaged JAR)
```bash
java -jar target/ArielCardales-1.0.0.jar
```
Note: Requires dependencies in `target/libs/` directory (created by `mvn package`).

### Testing
No test suite currently exists. The project includes JUnit 5 dependencies but `src/test/java` is not present.

## Architecture

### Layer Structure

**DAO Layer** (`com.arielcardales.AppInventario.DAO`)
- Generic CRUD interface: `CrudDAO<T, ID>` defines `findAll()`, `findById()`, `insert()`, `update()`, `deleteById()`
- Database connection: `Database.get()` returns HikariCP pooled connection
- Connection configured via environment variables (`PG_URL`, `PG_USER`, `PG_PASSWORD`) with hardcoded defaults
- DAOs: `ProductoDAO`, `ProductoVarianteDAO`, `CategoriaDAO`, `UnidadDAO`, `VentaDAO`, `InventarioDAO`, `LicenciaDAO`

**Service Layer** (`com.arielcardales.AppInventario.service`)
- `InventarioService`: Business logic for inventory operations, product selection dialogs, tree loading with filters

**Controllers** (`com.arielcardales.AppInventario.controller`)
- `AppController`: Main navigation controller, handles view switching and update checks
- `ProductoTreeController`: Complex TreeTableView for hierarchical product/variant display with inline editing
- `VentasController`: Sales management
- `AgregarProductoController`, `EditarProductoController`, `AgregarVarianteController`: Product CRUD forms
- `ExportarController`: Export functionality

**Entities** (`com.arielcardales.AppInventario.Entidades`)
- `Producto`: Base product entity
- `ProductoVariante`: Product variants with color/size
- `ItemInventario`: Unified view model for tree display (combines products and variants)
- `Categoria`, `Unidad`, `Venta`: Supporting entities

**Utilities** (`com.arielcardales.AppInventario.Util`)
- `Mapper`: ResultSet → Entity mapping
- `Arboles`: Helper for creating TreeTableView structures
- `EdicionCeldas`: Custom cell editors for TreeTableView inline editing
- `Tablas`, `TablasDialog`: Table utilities
- `ExportadorExcel`, `ExportadorPDF`, `ExportadorVentas`: Export functionality using Apache POI and OpenPDF

**Updates System** (`com.arielcardales.AppInventario.Updates`)
- `UpdateManager`: Orchestrates update workflow
- `UpdateChecker`: Fetches latest release from GitHub API
- `UpdateDownloader`: Downloads release artifacts
- `UpdateInstaller`: Replaces JAR and dependencies
- `UpdateDialog`: UI dialogs for update notifications
- `UpdateConfig`: Configuration and version management
  - Current version stored in `UpdateConfig.CURRENT_VERSION` (update this on each release)
  - GitHub repo: `agusArram/ArielCardales`
  - Config stored in `~/.appinventario/update.properties`

### Data Flow Pattern

1. Controller calls Service layer
2. Service calls DAO layer
3. DAO uses `Database.get()` for connection
4. Results mapped via `Mapper` utility
5. Controllers bind to JavaFX properties via FXML

### Key Design Patterns

- **TreeTableView Hierarchy**: Products with nested variants displayed using `TreeItem<ItemInventario>`
  - Parent nodes: base products (etiqueta, nombre, categoria)
  - Child nodes: variants (color, talle, stock, precio, costo)
  - `ItemInventario.isEsVariante()` distinguishes node types
  - Service layer clears inappropriate fields from parent nodes (e.g., stock, color, talle on base products)

- **Lazy Loading with Tasks**: Heavy UI loads wrapped in `javafx.concurrent.Task` with progress indicators
  - Example: `ProductoTreeController.cargarArbolAsync()` loads inventory in background thread

- **Preferences API**: User settings stored via `java.util.prefs.Preferences`
  - Example: `ProductoTreeController` stores "expandir_nodos_hijos" preference

## Database

**Connection**: PostgreSQL via HikariCP pool
- Environment variables: `PG_URL`, `PG_USER`, `PG_PASSWORD`, `PG_POOL_SIZE`
- Default pool size: 5 connections
- SSL required for Supabase connection

**Schema**: Uses view `vInventario` for product queries (DAOs reference this view)

**Note**: Database credentials are hardcoded in `Database.java` as fallback values. For production, ensure environment variables are set.

## JavaFX Specifics

**FXML Files**: Located in `src/main/resources/fxml/`
- `principal.fxml`: Main application window (AppController)
- `ProductoTree.fxml`: Inventory tree view (ProductoTreeController)
- `ventas.fxml`: Sales view (VentasController)
- `agregarProducto.fxml`, `agregarVariante.fxml`, `productoEditable.fxml`: Product forms

**Fonts**: Custom fonts loaded from `/Fuentes/static/` (Lora-Regular, Lora-Bold)

**CSS**: Stylesheet at `/Estilos/Estilos.css`

**Module System**: Uses Java Platform Module System (module-info.java)
- Required modules: `javafx.controls`, `javafx.fxml`, `java.sql`, `com.zaxxer.hikari`, `org.apache.poi.poi`, `org.apache.poi.ooxml`, `com.github.librepdf.openpdf`, `org.json`
- Opens packages: `com.arielcardales.AppInventario.controller` (to javafx.fxml), `com.arielcardales.AppInventario.Entidades` (to javafx.base for PropertyValueFactory)

**Launch Pattern**: `Launcher.main()` launches `App` to avoid JavaFX module issues

## Update System Workflow

1. On startup: `App.initUpdateSystem()` checks after 3s delay
2. Checks GitHub API for latest release (respects 24h interval via preferences)
3. If update available: Shows dialog with release notes
4. User can download → `UpdateDownloader` fetches release
5. On restart: `UpdateInstaller` replaces JAR and libs (executed via batch script)

**Manual Check**: Menu item in AppController calls `UpdateManager.checkForUpdatesAsync()`

**Version Management**: Update `UpdateConfig.CURRENT_VERSION` constant before each release (format: "vX.Y.Z")

## Common Development Tasks

### Adding a New Entity/DAO

1. Create entity class in `Entidades` package with JavaFX properties
2. Implement DAO in `DAO` package extending `CrudDAO<Entity, ID>`
3. Add mapping method in `Mapper` utility
4. Open entity package in `module-info.java` if used in TableView/TreeTableView
5. Create/update service layer if business logic needed

### Adding a New View

1. Create FXML file in `src/main/resources/fxml/`
2. Create controller in `controller` package
3. Open controller package in `module-info.java` if not already open
4. Add navigation method in `AppController` to load view

### Modifying Database Connection

Set environment variables before running:
```bash
set PG_URL=jdbc:postgresql://host:port/database?sslmode=require
set PG_USER=username
set PG_PASSWORD=password
set PG_POOL_SIZE=10
```

Or modify hardcoded defaults in `Database.java` static initializer.

## Dependencies

- JavaFX 21.0.6 (controls, fxml)
- PostgreSQL JDBC 42.7.3
- HikariCP 5.1.0 (connection pooling)
- Apache POI 5.2.5 (Excel export)
- OpenPDF 1.3.32 (PDF generation)
- ControlsFX 11.2.1 (notifications, dialogs)
- org.json 20231013 (GitHub API parsing)
- JUnit 5.12.1 (test scope, not currently used)

## Compiler Configuration

- Java 21 with preview features enabled (`--enable-preview`)
- UTF-8 encoding
- Main class: `SORT_PROYECTS.AppInventario.Launcher`

## License System

The application includes a database-based license validation system:

**Components:**
- `LicenciaDAO`: Database access for license records
- `LicenciaManager`: License validation logic (validates on startup in <100ms)
- `LicenciaConfig`: Configuration loader from `cliente.properties`
- `Licencia`: Entity with `EstadoLicencia` (ACTIVO/SUSPENDIDO/EXPIRADO/DEMO) and `PlanLicencia` (DEMO/BASE/FULL)

**Database table:** `licencia` (see `CrearDB.md` for schema)
- Primary key: `dni` (client's DNI as unique identifier)
- Validates: `estado = 'ACTIVO'` AND `fecha_expiracion >= CURRENT_DATE`

**Configuration:** Set client DNI in `src/main/resources/cliente.properties`:
```properties
cliente.id=46958104
cliente.nombre=Client Name
cliente.email=client@example.com
```

**Validation flow:**
1. App startup → `LicenciaManager.validarLicencia()`
2. Single SQL query: `SELECT * FROM licencia WHERE dni = ?`
3. If valid → continue; if invalid/expired → exit app

**Administration:** Manage licenses directly in Supabase or via SQL queries. See `SISTEMA_LICENCIAS_DB.md` for details.

**TENE EN CUENTA LAS VARIANTES DE PRODUCTOS,**