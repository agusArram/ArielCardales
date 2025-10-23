-- ========================================
-- ESQUEMA SQLite - Sistema de Backup Local
-- Sistema de Inventario Ariel Cardales
-- Adaptado de PostgreSQL a SQLite
-- ========================================

-- SQLite no soporta CREATE TYPE ENUM
-- Los enums se manejan con CHECK constraints

-- ========================================
-- TABLAS PRINCIPALES
-- ========================================

-- Unidades de medida
CREATE TABLE IF NOT EXISTS unidad (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL COLLATE NOCASE,
    abreviatura TEXT NOT NULL COLLATE NOCASE,
    cliente_id TEXT NOT NULL,
    createdAt TEXT NOT NULL DEFAULT (datetime('now')),
    UNIQUE (nombre, cliente_id),
    UNIQUE (abreviatura, cliente_id)
);

-- Categorías (con soporte para jerarquía)
CREATE TABLE IF NOT EXISTS categoria (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL COLLATE NOCASE,
    parentId INTEGER REFERENCES categoria(id),
    cliente_id TEXT NOT NULL,
    createdAt TEXT NOT NULL DEFAULT (datetime('now')),
    UNIQUE (nombre, cliente_id)
);

-- Productos base
CREATE TABLE IF NOT EXISTS producto (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    etiqueta TEXT NOT NULL COLLATE NOCASE,
    nombre TEXT NOT NULL COLLATE NOCASE,
    descripcion TEXT,
    categoriaId INTEGER NOT NULL REFERENCES categoria(id) ON UPDATE CASCADE,
    unidadId INTEGER NOT NULL REFERENCES unidad(id) ON UPDATE CASCADE,
    precio REAL NOT NULL CHECK (precio >= 0),
    costo REAL NOT NULL DEFAULT 0 CHECK (costo >= 0),
    stockOnHand INTEGER NOT NULL DEFAULT 0,
    active INTEGER NOT NULL DEFAULT 1,
    cliente_id TEXT NOT NULL,
    createdAt TEXT NOT NULL DEFAULT (datetime('now')),
    updatedAt TEXT NOT NULL DEFAULT (datetime('now')),
    UNIQUE (etiqueta, cliente_id)
);

-- Variantes de productos (colores, talles, etc.)
CREATE TABLE IF NOT EXISTS producto_variante (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    producto_id INTEGER NOT NULL REFERENCES producto(id) ON DELETE CASCADE,
    color TEXT COLLATE NOCASE,
    talle TEXT COLLATE NOCASE,
    precio REAL NOT NULL DEFAULT 0,
    costo REAL NOT NULL DEFAULT 0,
    stock INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
    etiqueta TEXT COLLATE NOCASE,
    active INTEGER NOT NULL DEFAULT 1,
    cliente_id TEXT NOT NULL,
    createdAt TEXT NOT NULL DEFAULT (datetime('now')),
    updatedAt TEXT NOT NULL DEFAULT (datetime('now')),
    UNIQUE (etiqueta, cliente_id),
    UNIQUE (producto_id, color, talle)
);

-- Imágenes de producto
CREATE TABLE IF NOT EXISTS productoImagen (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    productoId INTEGER NOT NULL REFERENCES producto(id) ON DELETE CASCADE,
    url TEXT NOT NULL,
    isPrimary INTEGER NOT NULL DEFAULT 0,
    cliente_id TEXT NOT NULL,
    createdAt TEXT NOT NULL DEFAULT (datetime('now'))
);

-- Historial de precios
CREATE TABLE IF NOT EXISTS precioHistorial (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    productoId INTEGER NOT NULL REFERENCES producto(id) ON DELETE CASCADE,
    precioPrevio REAL NOT NULL CHECK (precioPrevio >= 0),
    precioNuevo REAL NOT NULL CHECK (precioNuevo >= 0),
    changedAt TEXT NOT NULL DEFAULT (datetime('now')),
    changedBy TEXT,
    cliente_id TEXT NOT NULL
);

-- Movimientos de stock (log de cambios)
CREATE TABLE IF NOT EXISTS stockMovimiento (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    productoId INTEGER NOT NULL REFERENCES producto(id) ON DELETE CASCADE,
    cantidad INTEGER NOT NULL,
    tipo TEXT NOT NULL CHECK (tipo IN ('compra', 'venta', 'ajuste', 'devolucionIn', 'devolucionOut')),
    referencia TEXT,
    nota TEXT,
    cliente_id TEXT NOT NULL,
    createdAt TEXT NOT NULL DEFAULT (datetime('now'))
);

-- ========================================
-- PROVEEDORES Y COMPRAS
-- ========================================

CREATE TABLE IF NOT EXISTS proveedor (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL COLLATE NOCASE,
    telefono TEXT,
    email TEXT COLLATE NOCASE,
    notas TEXT,
    cliente_id TEXT NOT NULL,
    createdAt TEXT NOT NULL DEFAULT (datetime('now')),
    UNIQUE (nombre, cliente_id)
);

CREATE TABLE IF NOT EXISTS compra (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    proveedorId INTEGER NOT NULL REFERENCES proveedor(id),
    fecha TEXT NOT NULL DEFAULT (datetime('now')),
    total REAL NOT NULL DEFAULT 0 CHECK (total >= 0),
    cliente_id TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS compraItem (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    compraId INTEGER NOT NULL REFERENCES compra(id) ON DELETE CASCADE,
    productoId INTEGER NOT NULL REFERENCES producto(id),
    qty INTEGER NOT NULL CHECK (qty > 0),
    unitCost REAL NOT NULL CHECK (unitCost >= 0),
    cliente_id TEXT NOT NULL
);

-- ========================================
-- VENTAS
-- ========================================

CREATE TABLE IF NOT EXISTS venta (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    clienteNombre TEXT,
    fecha TEXT NOT NULL DEFAULT (datetime('now')),
    medioPago TEXT,
    total REAL NOT NULL DEFAULT 0 CHECK (total >= 0),
    cliente_id TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS ventaItem (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ventaId INTEGER NOT NULL REFERENCES venta(id) ON DELETE CASCADE,
    productoId INTEGER NOT NULL REFERENCES producto(id),
    variante_id INTEGER REFERENCES producto_variante(id),
    qty INTEGER NOT NULL CHECK (qty > 0),
    precioUnit REAL NOT NULL CHECK (precioUnit >= 0),
    productoNombre TEXT,
    cliente_id TEXT NOT NULL
);

-- ========================================
-- CLIENTES (del negocio, no confundir con tenant)
-- ========================================

CREATE TABLE IF NOT EXISTS cliente (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL COLLATE NOCASE,
    dni TEXT,
    telefono TEXT,
    email TEXT COLLATE NOCASE,
    notas TEXT,
    cliente_id TEXT NOT NULL,
    createdAt TEXT NOT NULL DEFAULT (datetime('now')),
    UNIQUE (dni, cliente_id)
);

-- ========================================
-- ÍNDICES PARA RENDIMIENTO
-- ========================================

CREATE INDEX IF NOT EXISTS idx_producto_nombre ON producto(nombre);
CREATE INDEX IF NOT EXISTS idx_producto_categoria ON producto(categoriaId);
CREATE INDEX IF NOT EXISTS idx_producto_cliente ON producto(cliente_id);

CREATE INDEX IF NOT EXISTS idx_variante_producto ON producto_variante(producto_id);
CREATE INDEX IF NOT EXISTS idx_variante_cliente ON producto_variante(cliente_id);

CREATE INDEX IF NOT EXISTS idx_categoria_cliente ON categoria(cliente_id);
CREATE INDEX IF NOT EXISTS idx_unidad_cliente ON unidad(cliente_id);

CREATE INDEX IF NOT EXISTS idx_venta_fecha ON venta(fecha);
CREATE INDEX IF NOT EXISTS idx_venta_cliente ON venta(cliente_id);

CREATE INDEX IF NOT EXISTS idx_compra_fecha ON compra(fecha);
CREATE INDEX IF NOT EXISTS idx_compra_cliente ON compra(cliente_id);

CREATE INDEX IF NOT EXISTS idx_stockMov_producto ON stockMovimiento(productoId);
CREATE INDEX IF NOT EXISTS idx_stockMov_cliente ON stockMovimiento(cliente_id);

CREATE INDEX IF NOT EXISTS idx_precioHist_producto ON precioHistorial(productoId);
CREATE INDEX IF NOT EXISTS idx_precioHist_cliente ON precioHistorial(cliente_id);

CREATE INDEX IF NOT EXISTS idx_proveedor_cliente ON proveedor(cliente_id);
CREATE INDEX IF NOT EXISTS idx_cliente_tenant ON cliente(cliente_id);

-- ========================================
-- FIN DEL ESQUEMA
-- ========================================
