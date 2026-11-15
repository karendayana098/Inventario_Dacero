-- D-ACERO | MySQL 8+
-- En DBeaver configure Statements delimiter = $$ y blank line delimiter = Never.
-- Ejecute el archivo como script (Alt+X). No agregue comandos DELIMITER.
-- CREATE TABLE IF NOT EXISTS conserva las tablas y los datos existentes.
-- Los DROP/CREATE siguientes solo reemplazan rutinas con estos mismos nombres.

CREATE DATABASE IF NOT EXISTS dacero
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci$$

USE dacero$$

-- TABLAS

CREATE TABLE IF NOT EXISTS sede (
    sede_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(80) NOT NULL,
    municipio VARCHAR(80) NOT NULL,
    direccion VARCHAR(160) NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (sede_id),
    UNIQUE KEY uq_sede_nombre (nombre)
) ENGINE=InnoDB$$

CREATE TABLE IF NOT EXISTS categoria (
    categoria_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(80) NOT NULL,
    descripcion VARCHAR(250) NULL,
    PRIMARY KEY (categoria_id),
    UNIQUE KEY uq_categoria_nombre (nombre)
) ENGINE=InnoDB$$

CREATE TABLE IF NOT EXISTS usuario_app (
    usuario_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    usuario VARCHAR(50) NOT NULL,
    clave_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL DEFAULT 'OPERADOR',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (usuario_id),
    UNIQUE KEY uq_usuario_app_usuario (usuario),
    CONSTRAINT chk_usuario_app_rol
        CHECK (rol IN ('ADMIN', 'OPERADOR', 'CONSULTA'))
) ENGINE=InnoDB$$

CREATE TABLE IF NOT EXISTS producto (
    producto_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(30) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    categoria_id BIGINT UNSIGNED NULL,
    unidad_medida VARCHAR(20) NOT NULL,
    stock_minimo DECIMAL(12,2) NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (producto_id),
    UNIQUE KEY uq_producto_codigo (codigo),
    KEY ix_producto_categoria (categoria_id),
    CONSTRAINT fk_producto_categoria
        FOREIGN KEY (categoria_id) REFERENCES categoria(categoria_id),
    CONSTRAINT chk_producto_stock_minimo CHECK (stock_minimo >= 0)
) ENGINE=InnoDB$$

CREATE TABLE IF NOT EXISTS inventario (
    sede_id BIGINT UNSIGNED NOT NULL,
    producto_id BIGINT UNSIGNED NOT NULL,
    existencia DECIMAL(12,2) NOT NULL DEFAULT 0,
    actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (sede_id, producto_id),
    CONSTRAINT fk_inventario_sede
        FOREIGN KEY (sede_id) REFERENCES sede(sede_id),
    CONSTRAINT fk_inventario_producto
        FOREIGN KEY (producto_id) REFERENCES producto(producto_id),
    CONSTRAINT chk_inventario_existencia CHECK (existencia >= 0)
) ENGINE=InnoDB$$

CREATE TABLE IF NOT EXISTS movimiento (
    movimiento_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    sede_id BIGINT UNSIGNED NOT NULL,
    tipo VARCHAR(10) NOT NULL,
    direccion_ajuste VARCHAR(10) NULL,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    referencia VARCHAR(60) NULL,
    observacion VARCHAR(300) NULL,
    usuario_id BIGINT UNSIGNED NULL,
    estado VARCHAR(12) NOT NULL DEFAULT 'CONFIRMADO',
    PRIMARY KEY (movimiento_id),
    KEY ix_movimiento_fecha_sede (sede_id, fecha),
    KEY ix_movimiento_usuario (usuario_id),
    CONSTRAINT fk_movimiento_sede
        FOREIGN KEY (sede_id) REFERENCES sede(sede_id),
    CONSTRAINT fk_movimiento_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario_app(usuario_id),
    CONSTRAINT chk_movimiento_tipo
        CHECK (tipo IN ('ENTRADA', 'SALIDA', 'AJUSTE')),
    CONSTRAINT chk_movimiento_direccion CHECK (
        (tipo = 'AJUSTE' AND direccion_ajuste IN ('ENTRADA', 'SALIDA'))
        OR (tipo <> 'AJUSTE' AND direccion_ajuste IS NULL)
    ),
    CONSTRAINT chk_movimiento_estado
        CHECK (estado IN ('BORRADOR', 'CONFIRMADO', 'ANULADO'))
) ENGINE=InnoDB$$

CREATE TABLE IF NOT EXISTS movimiento_detalle (
    detalle_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    movimiento_id BIGINT UNSIGNED NOT NULL,
    producto_id BIGINT UNSIGNED NOT NULL,
    cantidad DECIMAL(12,2) NOT NULL,
    costo_unitario DECIMAL(14,2) NULL,
    PRIMARY KEY (detalle_id),
    UNIQUE KEY uq_detalle_movimiento_producto (movimiento_id, producto_id),
    KEY ix_detalle_producto (producto_id),
    CONSTRAINT fk_detalle_movimiento
        FOREIGN KEY (movimiento_id) REFERENCES movimiento(movimiento_id),
    CONSTRAINT fk_detalle_producto
        FOREIGN KEY (producto_id) REFERENCES producto(producto_id),
    CONSTRAINT chk_detalle_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_detalle_costo
        CHECK (costo_unitario IS NULL OR costo_unitario >= 0)
) ENGINE=InnoDB$$

-- VISTAS

CREATE OR REPLACE VIEW vw_existencias AS
SELECT
    s.sede_id,
    s.nombre AS sede,
    p.producto_id,
    p.codigo,
    p.nombre AS producto,
    c.nombre AS categoria,
    p.unidad_medida,
    i.existencia,
    p.stock_minimo,
    (i.existencia <= p.stock_minimo) AS requiere_reposicion
FROM inventario i
JOIN sede s ON s.sede_id = i.sede_id
JOIN producto p ON p.producto_id = i.producto_id
LEFT JOIN categoria c ON c.categoria_id = p.categoria_id$$

CREATE OR REPLACE VIEW vw_stock_bajo AS
SELECT
    s.nombre AS sede,
    p.codigo,
    p.nombre AS producto,
    i.existencia,
    p.stock_minimo
FROM inventario i
JOIN sede s ON s.sede_id = i.sede_id
JOIN producto p ON p.producto_id = i.producto_id
WHERE p.activo = TRUE
  AND i.existencia <= p.stock_minimo$$

CREATE OR REPLACE VIEW vw_movimientos AS
SELECT
    m.movimiento_id,
    m.fecha,
    s.nombre AS sede,
    m.tipo,
    m.estado,
    m.referencia,
    p.codigo,
    p.nombre AS producto,
    d.cantidad,
    d.costo_unitario,
    u.usuario
FROM movimiento m
JOIN sede s ON s.sede_id = m.sede_id
JOIN movimiento_detalle d ON d.movimiento_id = m.movimiento_id
JOIN producto p ON p.producto_id = d.producto_id
LEFT JOIN usuario_app u ON u.usuario_id = m.usuario_id$$

-- FUNCION

DROP FUNCTION IF EXISTS fn_stock_bajo_count$$

CREATE FUNCTION fn_stock_bajo_count()
RETURNS BIGINT
READS SQL DATA
BEGIN
    DECLARE v_total BIGINT DEFAULT 0;
    SELECT COUNT(*) INTO v_total FROM vw_stock_bajo;
    RETURN v_total;
END$$

-- TRIGGER: validacion de existencia antes del detalle

DROP TRIGGER IF EXISTS trg_detalle_validar_stock$$

CREATE TRIGGER trg_detalle_validar_stock
BEFORE INSERT ON movimiento_detalle
FOR EACH ROW
BEGIN
    DECLARE v_sede_id BIGINT UNSIGNED DEFAULT NULL;
    DECLARE v_tipo VARCHAR(10) DEFAULT NULL;
    DECLARE v_direccion VARCHAR(10) DEFAULT NULL;
    DECLARE v_existencia DECIMAL(12,2) DEFAULT 0;
    DECLARE v_es_entrada BOOLEAN DEFAULT FALSE;

    SELECT sede_id, tipo, direccion_ajuste
    INTO v_sede_id, v_tipo, v_direccion
    FROM movimiento
    WHERE movimiento_id = NEW.movimiento_id;

    IF v_sede_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No existe la cabecera del movimiento';
    END IF;

    INSERT INTO inventario (sede_id, producto_id, existencia)
    VALUES (v_sede_id, NEW.producto_id, 0)
    ON DUPLICATE KEY UPDATE existencia = inventario.existencia;

    SELECT existencia INTO v_existencia
    FROM inventario
    WHERE sede_id = v_sede_id
      AND producto_id = NEW.producto_id
    FOR UPDATE;

    SET v_es_entrada =
        (v_tipo = 'ENTRADA')
        OR (v_tipo = 'AJUSTE' AND v_direccion = 'ENTRADA');

    IF NOT v_es_entrada AND v_existencia < NEW.cantidad THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Existencia insuficiente para registrar la salida';
    END IF;
END$$

-- TRIGGER: aplicar entrada/salida al inventario

DROP TRIGGER IF EXISTS trg_detalle_actualizar_inventario$$

CREATE TRIGGER trg_detalle_actualizar_inventario
AFTER INSERT ON movimiento_detalle
FOR EACH ROW
BEGIN
    DECLARE v_sede_id BIGINT UNSIGNED;
    DECLARE v_tipo VARCHAR(10);
    DECLARE v_direccion VARCHAR(10);
    DECLARE v_delta DECIMAL(12,2);

    SELECT sede_id, tipo, direccion_ajuste
    INTO v_sede_id, v_tipo, v_direccion
    FROM movimiento
    WHERE movimiento_id = NEW.movimiento_id;

    IF v_tipo = 'ENTRADA'
       OR (v_tipo = 'AJUSTE' AND v_direccion = 'ENTRADA') THEN
        SET v_delta = NEW.cantidad;
    ELSE
        SET v_delta = -NEW.cantidad;
    END IF;

    UPDATE inventario
    SET existencia = existencia + v_delta
    WHERE sede_id = v_sede_id
      AND producto_id = NEW.producto_id;
END$$

-- TRIGGERS: conservar el historial

DROP TRIGGER IF EXISTS trg_detalle_no_actualizar$$

CREATE TRIGGER trg_detalle_no_actualizar
BEFORE UPDATE ON movimiento_detalle
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT =
            'No edite detalles históricos; registre un movimiento compensatorio';
END$$

DROP TRIGGER IF EXISTS trg_detalle_no_borrar$$

CREATE TRIGGER trg_detalle_no_borrar
BEFORE DELETE ON movimiento_detalle
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT =
            'No borre detalles históricos; registre un movimiento compensatorio';
END$$

-- PROCEDIMIENTO: registrar movimiento y detalle en una transaccion

DROP PROCEDURE IF EXISTS sp_registrar_movimiento$$

CREATE PROCEDURE sp_registrar_movimiento(
    IN p_sede_id BIGINT UNSIGNED,
    IN p_tipo VARCHAR(10),
    IN p_direccion_ajuste VARCHAR(10),
    IN p_producto_id BIGINT UNSIGNED,
    IN p_cantidad DECIMAL(12,2),
    IN p_usuario_id BIGINT UNSIGNED,
    IN p_referencia VARCHAR(60),
    IN p_observacion VARCHAR(300),
    IN p_costo_unitario DECIMAL(14,2)
)
BEGIN
    DECLARE v_movimiento_id BIGINT UNSIGNED;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    IF p_tipo IS NULL
       OR p_tipo NOT IN ('ENTRADA', 'SALIDA', 'AJUSTE') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Tipo de movimiento inválido';
    END IF;

    IF p_cantidad IS NULL OR p_cantidad <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La cantidad debe ser mayor que cero';
    END IF;

    IF p_tipo = 'AJUSTE'
       AND (p_direccion_ajuste IS NULL
            OR p_direccion_ajuste NOT IN ('ENTRADA', 'SALIDA')) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'El ajuste debe indicar si suma o resta existencias';
    END IF;

    IF p_tipo <> 'AJUSTE' AND p_direccion_ajuste IS NOT NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Solo los ajustes llevan dirección de ajuste';
    END IF;

    START TRANSACTION;

    INSERT INTO movimiento (
        sede_id, tipo, direccion_ajuste, referencia, observacion, usuario_id
    )
    VALUES (
        p_sede_id, p_tipo, p_direccion_ajuste,
        p_referencia, p_observacion, p_usuario_id
    );

    SET v_movimiento_id = LAST_INSERT_ID();

    INSERT INTO movimiento_detalle (
        movimiento_id, producto_id, cantidad, costo_unitario
    )
    VALUES (
        v_movimiento_id, p_producto_id, p_cantidad, p_costo_unitario
    );

    COMMIT;

    SELECT v_movimiento_id AS movimiento_id_registrado;
END$$

-- CONSULTAS DE VERIFICACION

SHOW FULL TABLES FROM dacero$$
SHOW TRIGGERS FROM dacero$$
SHOW FUNCTION STATUS WHERE Db = 'dacero'$$
SHOW PROCEDURE STATUS WHERE Db = 'dacero'$$

-- Consultas de datos (inicialmente pueden no mostrar filas):
SELECT * FROM vw_existencias$$
SELECT * FROM vw_stock_bajo$$
SELECT fn_stock_bajo_count() AS referencias_bajo_minimo$$
SELECT * FROM vw_movimientos ORDER BY fecha DESC LIMIT 100$$
