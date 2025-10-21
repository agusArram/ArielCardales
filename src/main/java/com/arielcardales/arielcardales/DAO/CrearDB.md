
``` SQL
-- ========================================

-- SCRIPT COMPLETO BASE DE DATOS

-- Sistema de Inventario Ariel Cardales

-- PostgreSQL 14+

-- ========================================

  

-- Extensiones útiles

CREATE EXTENSION IF NOT EXISTS pg_trgm;    -- Para búsquedas difusas (trigram)

CREATE EXTENSION IF NOT EXISTS citext;     -- Para texto case-insensitive

  

-- ========================================

-- TIPOS PERSONALIZADOS

-- ========================================

  

DO $$

BEGIN

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tipoMovimiento') THEN

        CREATE TYPE "tipoMovimiento" AS ENUM (

            'compra',

            'venta',

            'ajuste',

            'devolucionIn',

            'devolucionOut'

        );

    END IF;

END$$;

  

-- ========================================

-- TABLAS PRINCIPALES

-- ========================================

  

-- Unidades de medida

CREATE TABLE IF NOT EXISTS unidad (

    id bigserial PRIMARY KEY,

    nombre citext NOT NULL UNIQUE,

    abreviatura citext NOT NULL UNIQUE,

    createdAt timestamptz NOT NULL DEFAULT now()

);

  

-- Categorías (con soporte para jerarquía)

CREATE TABLE IF NOT EXISTS categoria (

    id bigserial PRIMARY KEY,

    nombre citext NOT NULL UNIQUE,

    parentId bigint REFERENCES categoria(id),

    createdAt timestamptz NOT NULL DEFAULT now()

);

  

-- Productos base

CREATE TABLE IF NOT EXISTS producto (

    id bigserial PRIMARY KEY,

    etiqueta citext NOT NULL UNIQUE,

    nombre citext NOT NULL,

    descripcion text,

    categoriaId bigint NOT NULL REFERENCES categoria(id) ON UPDATE CASCADE,

    unidadId bigint NOT NULL REFERENCES unidad(id) ON UPDATE CASCADE,

    precio numeric(12,2) NOT NULL CHECK (precio >= 0),

    costo numeric(12,2) NOT NULL DEFAULT 0 CHECK (costo >= 0),

    stockOnHand integer NOT NULL DEFAULT 0,

    active boolean NOT NULL DEFAULT true,

    createdAt timestamptz NOT NULL DEFAULT now(),

    updatedAt timestamptz NOT NULL DEFAULT now()

);

  

-- Variantes de productos (colores, talles, etc.)

CREATE TABLE IF NOT EXISTS producto_variante (

    id bigserial PRIMARY KEY,

    producto_id bigint NOT NULL REFERENCES producto(id) ON DELETE CASCADE,

    color citext,

    talle citext,

    precio numeric(12,2) NOT NULL DEFAULT 0,

    costo numeric(12,2) NOT NULL DEFAULT 0,

    stock integer NOT NULL DEFAULT 0 CHECK (stock >= 0),

    etiqueta citext UNIQUE,

    active boolean NOT NULL DEFAULT true,

    createdAt timestamptz NOT NULL DEFAULT now(),

    updatedAt timestamptz NOT NULL DEFAULT now(),

    -- Evita duplicados de la misma variante para un producto

    UNIQUE (producto_id, color, talle)

);

  

-- Imágenes de producto

CREATE TABLE IF NOT EXISTS productoImagen (

    id bigserial PRIMARY KEY,

    productoId bigint NOT NULL REFERENCES producto(id) ON DELETE CASCADE,

    url text NOT NULL,

    isPrimary boolean NOT NULL DEFAULT false,

    createdAt timestamptz NOT NULL DEFAULT now()

);

  

-- Historial de precios

CREATE TABLE IF NOT EXISTS precioHistorial (

    id bigserial PRIMARY KEY,

    productoId bigint NOT NULL REFERENCES producto(id) ON DELETE CASCADE,

    precioPrevio numeric(12,2) NOT NULL CHECK (precioPrevio >= 0),

    precioNuevo numeric(12,2) NOT NULL CHECK (precioNuevo >= 0),

    changedAt timestamptz NOT NULL DEFAULT now(),

    changedBy text

);

  

-- Movimientos de stock (log de cambios)

CREATE TABLE IF NOT EXISTS stockMovimiento (

    id bigserial PRIMARY KEY,

    productoId bigint NOT NULL REFERENCES producto(id) ON DELETE CASCADE,

    cantidad integer NOT NULL,

    tipo "tipoMovimiento" NOT NULL,

    referencia text,

    nota text,

    createdAt timestamptz NOT NULL DEFAULT now()

);

  

-- ========================================

-- PROVEEDORES Y COMPRAS

-- ========================================

  

CREATE TABLE IF NOT EXISTS proveedor (

    id bigserial PRIMARY KEY,

    nombre citext NOT NULL UNIQUE,

    telefono text,

    email citext,

    notas text,

    createdAt timestamptz NOT NULL DEFAULT now()

);

  

CREATE TABLE IF NOT EXISTS compra (

    id bigserial PRIMARY KEY,

    proveedorId bigint NOT NULL REFERENCES proveedor(id),

    fecha timestamptz NOT NULL DEFAULT now(),

    total numeric(12,2) NOT NULL DEFAULT 0 CHECK (total >= 0)

);

  

CREATE TABLE IF NOT EXISTS compraItem (

    id bigserial PRIMARY KEY,

    compraId bigint NOT NULL REFERENCES compra(id) ON DELETE CASCADE,

    productoId bigint NOT NULL REFERENCES producto(id),

    qty integer NOT NULL CHECK (qty > 0),

    unitCost numeric(12,2) NOT NULL CHECK (unitCost >= 0),

    subtotal numeric(12,2) GENERATED ALWAYS AS (qty * unitCost) STORED

);

  

-- ========================================

-- VENTAS

-- ========================================

  

CREATE TABLE IF NOT EXISTS venta (

    id bigserial PRIMARY KEY,

    clienteNombre text,

    fecha timestamptz NOT NULL DEFAULT now(),

    medioPago text,

    total numeric(12,2) NOT NULL DEFAULT 0 CHECK (total >= 0)

);

  

CREATE TABLE IF NOT EXISTS ventaItem (

    id bigserial PRIMARY KEY,

    ventaId bigint NOT NULL REFERENCES venta(id) ON DELETE CASCADE,

    productoId bigint NOT NULL REFERENCES producto(id),

    variante_id bigint REFERENCES producto_variante(id), -- ⚠️ COLUMNA NUEVA: referencia a variante

    qty integer NOT NULL CHECK (qty > 0),

    precioUnit numeric(12,2) NOT NULL CHECK (precioUnit >= 0),

    subtotal numeric(12,2) GENERATED ALWAYS AS (qty * precioUnit) STORED

);

  

-- ========================================

-- ÍNDICES PARA RENDIMIENTO

-- ========================================

  

-- Búsqueda difusa en nombres de producto

CREATE INDEX IF NOT EXISTS idxProductoNombreTrgm

    ON producto USING gin (nombre gin_trgm_ops);

  

-- Índices básicos

CREATE INDEX IF NOT EXISTS idxProductoCategoria ON producto (categoriaId);

CREATE INDEX IF NOT EXISTS idxStockMovProducto ON stockMovimiento (productoId);

CREATE INDEX IF NOT EXISTS idxPrecioHistProducto ON precioHistorial (productoId);

CREATE INDEX IF NOT EXISTS idxVentaFecha ON venta (fecha);

CREATE INDEX IF NOT EXISTS idxCompraFecha ON compra (fecha);

  

-- Índices para variantes

CREATE INDEX IF NOT EXISTS idx_variante_producto ON producto_variante(producto_id);

CREATE INDEX IF NOT EXISTS idx_variante_color ON producto_variante(lower(color));

CREATE INDEX IF NOT EXISTS idx_variante_talle ON producto_variante(lower(talle));

  

-- ========================================

-- FUNCIONES Y TRIGGERS

-- ========================================

  

-- Función para actualizar updatedAt automáticamente (productos)

CREATE OR REPLACE FUNCTION setUpdatedAt()

RETURNS trigger LANGUAGE plpgsql AS $$

BEGIN

    new.updatedAt := now();

    RETURN new;

END$$;

  

DROP TRIGGER IF EXISTS trgProductoUpdatedAt ON producto;

CREATE TRIGGER trgProductoUpdatedAt

    BEFORE UPDATE ON producto

    FOR EACH ROW EXECUTE FUNCTION setUpdatedAt();

  

-- Función para actualizar updatedAt en variantes

CREATE OR REPLACE FUNCTION setUpdatedAt_variante()

RETURNS trigger LANGUAGE plpgsql AS $$

BEGIN

    new.updatedAt := now();

    RETURN new;

END$$;

  

DROP TRIGGER IF EXISTS trgVarianteUpdatedAt ON producto_variante;

CREATE TRIGGER trgVarianteUpdatedAt

    BEFORE UPDATE ON producto_variante

    FOR EACH ROW EXECUTE FUNCTION setUpdatedAt_variante();

  

-- Función para registrar historial de precios

CREATE OR REPLACE FUNCTION logPrecioHistorial()

RETURNS trigger LANGUAGE plpgsql AS $$

BEGIN

    IF new.precio IS DISTINCT FROM old.precio THEN

        INSERT INTO precioHistorial(productoId, precioPrevio, precioNuevo, changedBy)

        VALUES (old.id, old.precio, new.precio, current_user);

    END IF;

    RETURN new;

END$$;

  

DROP TRIGGER IF EXISTS trgLogPrecio ON producto;

CREATE TRIGGER trgLogPrecio

    BEFORE UPDATE ON producto

    FOR EACH ROW EXECUTE FUNCTION logPrecioHistorial();

  

-- Función para aplicar movimientos de stock

CREATE OR REPLACE FUNCTION aplicarMovimientoStock()

RETURNS trigger LANGUAGE plpgsql AS $$

DECLARE

    nuevoStock integer;

BEGIN

    UPDATE producto

    SET stockOnHand = stockOnHand + new.cantidad,

        updatedAt = now()

    WHERE id = new.productoId

    RETURNING stockOnHand INTO nuevoStock;

    IF nuevoStock < 0 THEN

        RAISE EXCEPTION 'Stock quedaría negativo para producto % (resultado: %)',

            new.productoId, nuevoStock

        USING errcode = '23514';

    END IF;

    RETURN new;

END$$;

  

DROP TRIGGER IF EXISTS trgApplyStock ON stockMovimiento;

CREATE TRIGGER trgApplyStock

    AFTER INSERT ON stockMovimiento

    FOR EACH ROW EXECUTE FUNCTION aplicarMovimientoStock();

  

-- ========================================

-- TRIGGERS PARA DESCUENTO DE STOCK EN VENTAS

-- ========================================

  

-- Trigger para descontar stock al insertar ventaItem

CREATE OR REPLACE FUNCTION descontar_stock_venta()

RETURNS trigger LANGUAGE plpgsql AS $$

BEGIN

    -- Si es una variante, descontar stock de la variante

    IF NEW.variante_id IS NOT NULL THEN

        UPDATE producto_variante

        SET stock = stock - NEW.qty,

            updatedAt = now()

        WHERE id = NEW.variante_id;

        -- Verificar si quedó stock negativo

        IF (SELECT stock FROM producto_variante WHERE id = NEW.variante_id) < 0 THEN

            RAISE EXCEPTION 'Stock insuficiente en variante ID %', NEW.variante_id

            USING errcode = '23514';

        END IF;

    ELSE

        -- Si no es variante, descontar del producto base

        UPDATE producto

        SET stockOnHand = stockOnHand - NEW.qty,

            updatedAt = now()

        WHERE id = NEW.productoId;

        -- Verificar si quedó stock negativo

        IF (SELECT stockOnHand FROM producto WHERE id = NEW.productoId) < 0 THEN

            RAISE EXCEPTION 'Stock insuficiente en producto ID %', NEW.productoId

            USING errcode = '23514';

        END IF;

    END IF;

    RETURN NEW;

END$$;

  

DROP TRIGGER IF EXISTS trgDescontarStockVenta ON ventaItem;

CREATE TRIGGER trgDescontarStockVenta

    AFTER INSERT ON ventaItem

    FOR EACH ROW EXECUTE FUNCTION descontar_stock_venta();

  

-- ========================================

-- VISTAS

-- ========================================

  

-- Vista de inventario (productos base)

CREATE OR REPLACE VIEW vInventario AS

SELECT

    p.id,

    p.etiqueta,

    p.nombre,

    c.nombre as categoria,

    u.abreviatura as unidad,

    p.precio,

    p.costo,

    p.stockOnHand,

    p.active,

    p.updatedAt

FROM producto p

JOIN categoria c ON c.id = p.categoriaId

JOIN unidad u ON u.id = p.unidadId;

  

-- Vista de inventario con variantes (unificada)

CREATE OR REPLACE VIEW vInventario_variantes AS

WITH prod_base AS (

    SELECT

        p.id as producto_id,

        p.etiqueta as producto_etiqueta,

        p.nombre as producto_nombre,

        p.descripcion,

        c.nombre as categoria,

        u.abreviatura as unidad,

        p.precio as precio_base,

        p.costo as costo_base,

        p.stockOnHand as stock_base,

        p.active as active_base,

        p.createdAt as createdAt_prod,

        p.updatedAt as updatedAt_prod

    FROM producto p

    JOIN categoria c ON c.id = p.categoriaId

    JOIN unidad u ON u.id = p.unidadId

)

-- 1) Variantes reales (si existen)

SELECT

    b.producto_id,

    b.producto_etiqueta,

    b.producto_nombre,

    b.descripcion,

    b.categoria,

    b.unidad,

    v.id as variante_id,

    coalesce(v.color, '-') as color,

    coalesce(v.talle, '-') as talle,

    coalesce(v.precio, b.precio_base) as precio,

    coalesce(v.costo, b.costo_base) as costo,

    coalesce(v.stock, b.stock_base) as stockOnHand,

    coalesce(v.active, b.active_base) as active,

    greatest(b.updatedAt_prod, v.updatedAt) as updatedAt

FROM prod_base b

JOIN producto_variante v ON v.producto_id = b.producto_id

  

UNION ALL

  

-- 2) Productos sin variantes (queda la base)

SELECT

    b.producto_id,

    b.producto_etiqueta,

    b.producto_nombre,

    b.descripcion,

    b.categoria,

    b.unidad,

    null::bigint as variante_id,

    '-' as color,

    '-' as talle,

    b.precio_base as precio,

    b.costo_base as costo,

    b.stock_base as stockOnHand,

    b.active_base as active,

    b.updatedAt_prod as updatedAt

FROM prod_base b

WHERE NOT EXISTS (

    SELECT 1 FROM producto_variante v WHERE v.producto_id = b.producto_id

)

ORDER BY producto_nombre, color, talle;

  

-- Vista de historial de precios recientes

CREATE OR REPLACE VIEW vPreciosRecientes AS

SELECT ph.*

FROM precioHistorial ph

ORDER BY ph.changedAt DESC;

  

-- ========================================

-- DATOS INICIALES (SEEDS)

-- ========================================

  

-- Unidades básicas

INSERT INTO unidad (nombre, abreviatura) VALUES

    ('Unidad', 'u'),

    ('Par', 'par'),

    ('Juego', 'jgo')

ON CONFLICT (nombre) DO NOTHING;

  

-- Categorías básicas

INSERT INTO categoria (nombre) VALUES

    ('Cuchillos'),

    ('Mates'),

    ('Materas'),

    ('Accesorios')

ON CONFLICT (nombre) DO NOTHING;

  

-- ========================================

-- COMENTARIOS INFORMATIVOS

-- ========================================

  

COMMENT ON TABLE producto IS 'Productos base del inventario';

COMMENT ON TABLE producto_variante IS 'Variantes de productos (color, talle, etc.)';

COMMENT ON TABLE ventaItem IS 'Items de venta - descontan stock automáticamente vía trigger';

COMMENT ON TABLE precioHistorial IS 'Registro automático de cambios de precio';

COMMENT ON TABLE stockMovimiento IS 'Log de todos los movimientos de inventario';

  

COMMENT ON TRIGGER trgDescontarStockVenta ON ventaItem IS

    'Descuenta stock automáticamente al registrar venta (variante o producto base)';

  

-- ========================================

-- FIN DEL SCRIPT

-- ========================================

  
  

ALTER TABLE ventaItem

ADD COLUMN productoNombre text;



-- ========================================
-- MIGRACIÓN: Agregar tabla CLIENTE
-- Sistema de Inventario Ariel Cardales
-- PostgreSQL 14+
-- ========================================

-- Crear tabla cliente
CREATE TABLE IF NOT EXISTS cliente (
    id bigserial PRIMARY KEY,
    nombre citext NOT NULL,
    dni text UNIQUE,
    telefono text,
    email citext,
    notas text,
    createdAt timestamptz NOT NULL DEFAULT now()
);

-- Índices para búsquedas rápidas
CREATE INDEX IF NOT EXISTS idx_cliente_nombre ON cliente(lower(nombre));
CREATE INDEX IF NOT EXISTS idx_cliente_dni ON cliente(dni);
CREATE INDEX IF NOT EXISTS idx_cliente_telefono ON cliente(telefono);

-- Comentario informativo
COMMENT ON TABLE cliente IS 'Clientes del sistema - gestión de contactos y seguimiento de ventas';
COMMENT ON COLUMN cliente.dni IS 'DNI único del cliente (opcional)';
COMMENT ON COLUMN cliente.telefono IS 'Teléfono de contacto';
COMMENT ON COLUMN cliente.email IS 'Email de contacto';
COMMENT ON COLUMN cliente.notas IS 'Notas adicionales sobre el cliente';

-- Verificar que se creó correctamente
SELECT 'Tabla cliente creada exitosamente' as resultado;


```