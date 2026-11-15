-- Datos de demostración para MySQL 8+.
-- En DBeaver usa delimitador ; para este archivo.
USE dacero;

INSERT INTO sede (nombre, municipio)
VALUES ('Sede Sincelejo', 'Sincelejo'), ('Sede Sahagún', 'Sahagún')
ON DUPLICATE KEY UPDATE municipio = VALUES(municipio);

INSERT INTO categoria (nombre, descripcion)
VALUES ('Varillas y perfiles', 'Material de acero'),
       ('Tejas y láminas', 'Cubiertas y láminas'),
       ('Cemento y agregados', 'Materiales de construcción')
ON DUPLICATE KEY UPDATE descripcion = VALUES(descripcion);

INSERT INTO producto (codigo, nombre, categoria_id, unidad_medida, stock_minimo)
SELECT 'VAR-001', 'Varilla corrugada 1/2', categoria_id, 'unidad', 10
FROM categoria WHERE nombre = 'Varillas y perfiles'
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), stock_minimo = VALUES(stock_minimo);

INSERT INTO producto (codigo, nombre, categoria_id, unidad_medida, stock_minimo)
SELECT 'CEM-001', 'Cemento gris 50 kg', categoria_id, 'bulto', 20
FROM categoria WHERE nombre = 'Cemento y agregados'
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), stock_minimo = VALUES(stock_minimo);

INSERT IGNORE INTO inventario (sede_id, producto_id, existencia)
SELECT s.sede_id, p.producto_id, 45
FROM sede s JOIN producto p
WHERE s.nombre = 'Sede Sincelejo' AND p.codigo = 'VAR-001';

INSERT IGNORE INTO inventario (sede_id, producto_id, existencia)
SELECT s.sede_id, p.producto_id, 32
FROM sede s JOIN producto p
WHERE s.nombre = 'Sede Sincelejo' AND p.codigo = 'CEM-001';

INSERT IGNORE INTO inventario (sede_id, producto_id, existencia)
SELECT s.sede_id, p.producto_id, 18
FROM sede s JOIN producto p
WHERE s.nombre = 'Sede Sahagún' AND p.codigo = 'VAR-001';

INSERT IGNORE INTO inventario (sede_id, producto_id, existencia)
SELECT s.sede_id, p.producto_id, 12
FROM sede s JOIN producto p
WHERE s.nombre = 'Sede Sahagún' AND p.codigo = 'CEM-001';

-- Al abrir la app, si usuario_app está vacía, aparecerá el formulario
-- para crear el primer administrador.
