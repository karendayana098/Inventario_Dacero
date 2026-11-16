-- Ejecutar una sola vez en bases existentes que tengan los roles antiguos.
-- Convierte ADMIN -> ADMINISTRADOR, OPERADOR -> BODEGUERO y CONSULTA -> VENDEDOR.
USE dacero;

ALTER TABLE usuario_app DROP CHECK chk_usuario_app_rol;

UPDATE usuario_app
SET rol = CASE UPPER(rol)
    WHEN 'ADMIN' THEN 'ADMINISTRADOR'
    WHEN 'OPERADOR' THEN 'BODEGUERO'
    WHEN 'CONSULTA' THEN 'VENDEDOR'
    ELSE UPPER(rol)
END;

ALTER TABLE usuario_app
    MODIFY rol VARCHAR(20) NOT NULL DEFAULT 'BODEGUERO';

ALTER TABLE usuario_app
    ADD CONSTRAINT chk_usuario_app_rol
    CHECK (rol IN ('ADMINISTRADOR', 'VENDEDOR', 'BODEGUERO'));
