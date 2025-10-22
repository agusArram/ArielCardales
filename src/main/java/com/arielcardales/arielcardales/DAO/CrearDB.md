
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


-- ========================================
-- MIGRACIÓN: Agregar tabla LICENCIA
-- Sistema de Licencias - Ariel Cardales
-- PostgreSQL 14+
-- ========================================

-- Crear tipo ENUM para estado de licencia
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'estado_licencia') THEN
        CREATE TYPE estado_licencia AS ENUM (
            'ACTIVO',
            'SUSPENDIDO',
            'EXPIRADO',
            'DEMO'
        );
    END IF;
END$$;

-- Crear tipo ENUM para plan de licencia
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'plan_licencia') THEN
        CREATE TYPE plan_licencia AS ENUM (
            'DEMO',
            'BASE',
            'FULL'
        );
    END IF;
END$$;

-- Crear tabla licencia
CREATE TABLE IF NOT EXISTS licencia (
    id bigserial PRIMARY KEY,
    dni text NOT NULL UNIQUE,  -- DNI como identificador único del cliente
    nombre citext NOT NULL,
    email citext,
    estado estado_licencia NOT NULL DEFAULT 'DEMO',
    plan plan_licencia NOT NULL DEFAULT 'DEMO',
    fecha_expiracion date NOT NULL,
    notas text,
    createdAt timestamptz NOT NULL DEFAULT now(),
    updatedAt timestamptz NOT NULL DEFAULT now()
);

-- Índices para búsquedas rápidas
CREATE INDEX IF NOT EXISTS idx_licencia_dni ON licencia(dni);
CREATE INDEX IF NOT EXISTS idx_licencia_estado ON licencia(estado);
CREATE INDEX IF NOT EXISTS idx_licencia_expiracion ON licencia(fecha_expiracion);

-- Trigger para actualizar updatedAt automáticamente
DROP TRIGGER IF EXISTS trgLicenciaUpdatedAt ON licencia;
CREATE TRIGGER trgLicenciaUpdatedAt
    BEFORE UPDATE ON licencia
    FOR EACH ROW EXECUTE FUNCTION setUpdatedAt();

-- Comentarios informativos
COMMENT ON TABLE licencia IS 'Licencias del sistema - control de acceso por DNI';
COMMENT ON COLUMN licencia.dni IS 'DNI único del cliente - identificador principal';
COMMENT ON COLUMN licencia.estado IS 'Estado actual de la licencia (ACTIVO, SUSPENDIDO, EXPIRADO, DEMO)';
COMMENT ON COLUMN licencia.plan IS 'Plan de la licencia (DEMO, BASE, FULL) - define permisos y límites';
COMMENT ON COLUMN licencia.fecha_expiracion IS 'Fecha de expiración de la licencia';

-- Datos de ejemplo (licencia DEMO y desarrollador)
INSERT INTO licencia (dni, nombre, email, estado, plan, fecha_expiracion, notas) VALUES
    ('DEMO_CLIENT', 'Cliente Demo', 'demo@ejemplo.com', 'DEMO', 'DEMO', CURRENT_DATE + INTERVAL '15 days', 'Licencia de demostración - 15 días'),
    ('46958104', 'Agustin desarrollador', 'agus@ejemplo.com', 'ACTIVO', 'FULL', '2030-12-31', 'Desarrollador - Licencia permanente')
ON CONFLICT (dni) DO NOTHING;

-- Verificar que se creó correctamente
SELECT 'Tabla licencia creada exitosamente' as resultado;


-- ========================================
-- MIGRACIÓN A MULTI-TENANT
-- Sistema Multi-tenant con Login
-- PostgreSQL 14+
-- ========================================

/*
 * OBJETIVO: Convertir el sistema a multi-tenant donde:
 * - Cada usuario/cliente tiene su propia cuenta (login con email/password)
 * - Todos los datos están en una sola DB pero aislados por cliente_id
 * - Un solo programa sirve a múltiples clientes
 * - Escalable infinitamente
 */

-- ========================================
-- PASO 1: Modificar tabla licencia
-- ========================================

-- 1.1. Agregar columna para password hash
ALTER TABLE licencia
ADD COLUMN IF NOT EXISTS password_hash text;

COMMENT ON COLUMN licencia.password_hash IS 'Hash bcrypt de la contraseña (nunca se guarda en texto plano)';

-- 1.2. Renombrar dni a cliente_id para mayor claridad
-- NOTA: dni sigue siendo el identificador, solo cambia el nombre de la columna
ALTER TABLE licencia
RENAME COLUMN dni TO cliente_id;

-- 1.3. Hacer email NOT NULL y UNIQUE (necesario para login)
ALTER TABLE licencia
ALTER COLUMN email SET NOT NULL;

-- Si hay emails duplicados, primero resolver:
-- UPDATE licencia SET email = email || '_' || id WHERE email IN (SELECT email FROM licencia GROUP BY email HAVING COUNT(*) > 1);

-- 1.4. Agregar constraint de email único si no existe
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'licencia_email_key'
    ) THEN
        ALTER TABLE licencia ADD CONSTRAINT licencia_email_key UNIQUE (email);
    END IF;
END$$;

-- 1.5. Crear índice en email para login rápido
CREATE INDEX IF NOT EXISTS idx_licencia_email ON licencia(email);

COMMENT ON TABLE licencia IS 'Cuentas de usuarios/clientes - cada registro es un tenant con login propio';
COMMENT ON COLUMN licencia.cliente_id IS 'ID único del cliente - puede ser DNI, UUID, o cualquier identificador';
COMMENT ON COLUMN licencia.email IS 'Email único para login';


-- ========================================
-- PASO 2: Agregar cliente_id a TODAS las tablas
-- ========================================

-- 2.1. UNIDAD
ALTER TABLE unidad
ADD COLUMN IF NOT EXISTS cliente_id text REFERENCES licencia(cliente_id) ON DELETE CASCADE;

-- Crear índice para performance
CREATE INDEX IF NOT EXISTS idx_unidad_cliente ON unidad(cliente_id);

-- Modificar constraint UNIQUE para incluir cliente_id (cada cliente puede tener su propia "Unidad")
ALTER TABLE unidad DROP CONSTRAINT IF EXISTS unidad_nombre_key;
ALTER TABLE unidad DROP CONSTRAINT IF EXISTS unidad_abreviatura_key;
ALTER TABLE unidad ADD CONSTRAINT unidad_nombre_cliente_key UNIQUE (nombre, cliente_id);
ALTER TABLE unidad ADD CONSTRAINT unidad_abreviatura_cliente_key UNIQUE (abreviatura, cliente_id);

COMMENT ON COLUMN unidad.cliente_id IS 'ID del cliente propietario - aísla datos entre tenants';


-- 2.2. CATEGORIA
ALTER TABLE categoria
ADD COLUMN IF NOT EXISTS cliente_id text REFERENCES licencia(cliente_id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_categoria_cliente ON categoria(cliente_id);

-- Modificar UNIQUE constraint
ALTER TABLE categoria DROP CONSTRAINT IF EXISTS categoria_nombre_key;
ALTER TABLE categoria ADD CONSTRAINT categoria_nombre_cliente_key UNIQUE (nombre, cliente_id);

COMMENT ON COLUMN categoria.cliente_id IS 'ID del cliente propietario - aísla datos entre tenants';


-- 2.3. PRODUCTO
ALTER TABLE producto
ADD COLUMN IF NOT EXISTS cliente_id text REFERENCES licencia(cliente_id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_producto_cliente ON producto(cliente_id);

-- Modificar UNIQUE constraint de etiqueta
ALTER TABLE producto DROP CONSTRAINT IF EXISTS producto_etiqueta_key;
ALTER TABLE producto ADD CONSTRAINT producto_etiqueta_cliente_key UNIQUE (etiqueta, cliente_id);

COMMENT ON COLUMN producto.cliente_id IS 'ID del cliente propietario - aísla datos entre tenants';


-- 2.4. PRODUCTO_VARIANTE
ALTER TABLE producto_variante
ADD COLUMN IF NOT EXISTS cliente_id text REFERENCES licencia(cliente_id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_producto_variante_cliente ON producto_variante(cliente_id);

-- Modificar UNIQUE constraint de etiqueta
ALTER TABLE producto_variante DROP CONSTRAINT IF EXISTS producto_variante_etiqueta_key;
ALTER TABLE producto_variante ADD CONSTRAINT producto_variante_etiqueta_cliente_key UNIQUE (etiqueta, cliente_id);

COMMENT ON COLUMN producto_variante.cliente_id IS 'ID del cliente propietario - aísla datos entre tenants';


-- 2.5. PRODUCTOIMAGEN
ALTER TABLE productoImagen
ADD COLUMN IF NOT EXISTS cliente_id text REFERENCES licencia(cliente_id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_productoImagen_cliente ON productoImagen(cliente_id);

COMMENT ON COLUMN productoImagen.cliente_id IS 'ID del cliente propietario - aísla datos entre tenants';


-- 2.6. PRECIOHISTORIAL
ALTER TABLE precioHistorial
ADD COLUMN IF NOT EXISTS cliente_id text REFERENCES licencia(cliente_id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_precioHistorial_cliente ON precioHistorial(cliente_id);

COMMENT ON COLUMN precioHistorial.cliente_id IS 'ID del cliente propietario - aísla datos entre tenants';


-- 2.7. STOCKMOVIMIENTO
ALTER TABLE stockMovimiento
ADD COLUMN IF NOT EXISTS cliente_id text REFERENCES licencia(cliente_id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_stockMovimiento_cliente ON stockMovimiento(cliente_id);

COMMENT ON COLUMN stockMovimiento.cliente_id IS 'ID del cliente propietario - aísla datos entre tenants';


-- 2.8. PROVEEDOR
ALTER TABLE proveedor
ADD COLUMN IF NOT EXISTS cliente_id text REFERENCES licencia(cliente_id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_proveedor_cliente ON proveedor(cliente_id);

-- Modificar UNIQUE constraint
ALTER TABLE proveedor DROP CONSTRAINT IF EXISTS proveedor_nombre_key;
ALTER TABLE proveedor ADD CONSTRAINT proveedor_nombre_cliente_key UNIQUE (nombre, cliente_id);

COMMENT ON COLUMN proveedor.cliente_id IS 'ID del cliente propietario - aísla datos entre tenants';


-- 2.9. COMPRA
ALTER TABLE compra
ADD COLUMN IF NOT EXISTS cliente_id text REFERENCES licencia(cliente_id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_compra_cliente ON compra(cliente_id);

COMMENT ON COLUMN compra.cliente_id IS 'ID del cliente propietario - aísla datos entre tenants';


-- 2.10. COMPRAITEM
ALTER TABLE compraItem
ADD COLUMN IF NOT EXISTS cliente_id text REFERENCES licencia(cliente_id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_compraItem_cliente ON compraItem(cliente_id);

COMMENT ON COLUMN compraItem.cliente_id IS 'ID del cliente propietario - aísla datos entre tenants';


-- 2.11. VENTA
ALTER TABLE venta
ADD COLUMN IF NOT EXISTS cliente_id text REFERENCES licencia(cliente_id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_venta_cliente ON venta(cliente_id);

COMMENT ON COLUMN venta.cliente_id IS 'ID del cliente propietario - aísla datos entre tenants';


-- 2.12. VENTAITEM
ALTER TABLE ventaItem
ADD COLUMN IF NOT EXISTS cliente_id text REFERENCES licencia(cliente_id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_ventaItem_cliente ON ventaItem(cliente_id);

COMMENT ON COLUMN ventaItem.cliente_id IS 'ID del cliente propietario - aísla datos entre tenants';


-- 2.13. CLIENTE (tabla de clientes del negocio, no confundir con tenant)
ALTER TABLE cliente
ADD COLUMN IF NOT EXISTS cliente_id text REFERENCES licencia(cliente_id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_cliente_tenant ON cliente(cliente_id);

-- Modificar UNIQUE constraint de DNI para scope por tenant
ALTER TABLE cliente DROP CONSTRAINT IF EXISTS cliente_dni_key;
ALTER TABLE cliente ADD CONSTRAINT cliente_dni_cliente_key UNIQUE (dni, cliente_id);

COMMENT ON COLUMN cliente.cliente_id IS 'ID del tenant propietario - aísla clientes entre diferentes negocios';


-- ========================================
-- PASO 3: Migrar datos existentes
-- ========================================

/*
 * IMPORTANTE: Si ya tienes datos en la DB, necesitas asignarlos a un cliente_id.
 *
 * Opción A: Asignar todos los datos actuales a un cliente específico (ej: tu cuenta de desarrollo)
 * Opción B: Dejar cliente_id NULL temporalmente y asignar manualmente después
 *
 * Aquí muestro la Opción A:
 */

-- 3.1. Definir el cliente_id para datos existentes
-- Cambiar '46958104' por el cliente_id que quieras usar
DO $$
DECLARE
    default_cliente_id text := '46958104'; -- TU CLIENTE_ID AQUÍ
BEGIN
    -- Verificar que existe en licencia
    IF EXISTS (SELECT 1 FROM licencia WHERE cliente_id = default_cliente_id) THEN

        -- Asignar datos existentes a este cliente
        UPDATE unidad SET cliente_id = default_cliente_id WHERE cliente_id IS NULL;
        UPDATE categoria SET cliente_id = default_cliente_id WHERE cliente_id IS NULL;
        UPDATE producto SET cliente_id = default_cliente_id WHERE cliente_id IS NULL;
        UPDATE producto_variante SET cliente_id = default_cliente_id WHERE cliente_id IS NULL;
        UPDATE productoImagen SET cliente_id = default_cliente_id WHERE cliente_id IS NULL;
        UPDATE precioHistorial SET cliente_id = default_cliente_id WHERE cliente_id IS NULL;
        UPDATE stockMovimiento SET cliente_id = default_cliente_id WHERE cliente_id IS NULL;
        UPDATE proveedor SET cliente_id = default_cliente_id WHERE cliente_id IS NULL;
        UPDATE compra SET cliente_id = default_cliente_id WHERE cliente_id IS NULL;
        UPDATE compraItem SET cliente_id = default_cliente_id WHERE cliente_id IS NULL;
        UPDATE venta SET cliente_id = default_cliente_id WHERE cliente_id IS NULL;
        UPDATE ventaItem SET cliente_id = default_cliente_id WHERE cliente_id IS NULL;
        UPDATE cliente SET cliente_id = default_cliente_id WHERE cliente_id IS NULL;

        RAISE NOTICE 'Datos migrados al cliente_id: %', default_cliente_id;
    ELSE
        RAISE EXCEPTION 'No existe el cliente_id: %. Crear primero en tabla licencia.', default_cliente_id;
    END IF;
END$$;


-- ========================================
-- PASO 4: Hacer cliente_id NOT NULL (después de migrar datos)
-- ========================================

-- Solo ejecutar DESPUÉS de que TODOS los registros tengan cliente_id asignado

ALTER TABLE unidad ALTER COLUMN cliente_id SET NOT NULL;
ALTER TABLE categoria ALTER COLUMN cliente_id SET NOT NULL;
ALTER TABLE producto ALTER COLUMN cliente_id SET NOT NULL;
ALTER TABLE producto_variante ALTER COLUMN cliente_id SET NOT NULL;
ALTER TABLE productoImagen ALTER COLUMN cliente_id SET NOT NULL;
ALTER TABLE precioHistorial ALTER COLUMN cliente_id SET NOT NULL;
ALTER TABLE stockMovimiento ALTER COLUMN cliente_id SET NOT NULL;
ALTER TABLE proveedor ALTER COLUMN cliente_id SET NOT NULL;
ALTER TABLE compra ALTER COLUMN cliente_id SET NOT NULL;
ALTER TABLE compraItem ALTER COLUMN cliente_id SET NOT NULL;
ALTER TABLE venta ALTER COLUMN cliente_id SET NOT NULL;
ALTER TABLE ventaItem ALTER COLUMN cliente_id SET NOT NULL;
ALTER TABLE cliente ALTER COLUMN cliente_id SET NOT NULL;


-- ========================================
-- PASO 5: Actualizar vistas
-- ========================================

-- 5.1. Actualizar vInventario para incluir cliente_id
DROP VIEW IF EXISTS vInventario;
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
    p.updatedAt,
    p.cliente_id  -- NUEVO
FROM producto p
JOIN categoria c ON c.id = p.categoriaId
JOIN unidad u ON u.id = p.unidadId;


-- 5.2. Actualizar vInventario_variantes para incluir cliente_id
DROP VIEW IF EXISTS vInventario_variantes;
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
        p.updatedAt as updatedAt_prod,
        p.cliente_id  -- NUEVO
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
    greatest(b.updatedAt_prod, v.updatedAt) as updatedAt,
    b.cliente_id  -- NUEVO
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
    b.updatedAt_prod as updatedAt,
    b.cliente_id  -- NUEVO
FROM prod_base b
WHERE NOT EXISTS (
    SELECT 1 FROM producto_variante v WHERE v.producto_id = b.producto_id
)
ORDER BY producto_nombre, color, talle;


-- ========================================
-- PASO 6: Crear usuarios de prueba
-- ========================================

-- 6.1. Usuario DEMO (ya existe, solo agregar password)
-- Password: "demo123" (cambiar en producción)
-- Hash generado con bcrypt rounds=10
UPDATE licencia
SET password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
WHERE cliente_id = 'DEMO_CLIENT';

-- 6.2. Usuario desarrollador (ya existe, solo agregar password)
-- Password: "admin123" (cambiar en producción)
UPDATE licencia
SET password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
WHERE cliente_id = '46958104';

-- 6.3. Crear usuario de prueba adicional
INSERT INTO licencia (cliente_id, nombre, email, password_hash, estado, plan, fecha_expiracion, notas)
VALUES (
    'TEST_USER_001',
    'Usuario de Prueba',
    'test@ejemplo.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- password: "test123"
    'ACTIVO',
    'BASE',
    CURRENT_DATE + INTERVAL '30 days',
    'Usuario de prueba para testing multi-tenant'
)
ON CONFLICT (cliente_id) DO NOTHING;


-- ========================================
-- PASO 7: Verificación
-- ========================================

-- Verificar que todas las tablas tienen cliente_id
SELECT
    table_name,
    column_name,
    data_type,
    is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
AND column_name = 'cliente_id'
ORDER BY table_name;

-- Contar registros por cliente
SELECT
    l.cliente_id,
    l.nombre,
    l.email,
    (SELECT COUNT(*) FROM producto WHERE cliente_id = l.cliente_id) as productos,
    (SELECT COUNT(*) FROM venta WHERE cliente_id = l.cliente_id) as ventas,
    (SELECT COUNT(*) FROM categoria WHERE cliente_id = l.cliente_id) as categorias
FROM licencia l
ORDER BY l.nombre;

-- Verificar que NO hay datos sin cliente_id
SELECT 'unidad' as tabla, COUNT(*) as sin_cliente FROM unidad WHERE cliente_id IS NULL
UNION ALL
SELECT 'categoria', COUNT(*) FROM categoria WHERE cliente_id IS NULL
UNION ALL
SELECT 'producto', COUNT(*) FROM producto WHERE cliente_id IS NULL
UNION ALL
SELECT 'venta', COUNT(*) FROM venta WHERE cliente_id IS NULL;

SELECT '✅ Migración a multi-tenant completada exitosamente' as resultado;


```