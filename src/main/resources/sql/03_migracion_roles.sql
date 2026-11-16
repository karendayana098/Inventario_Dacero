-- Ejecutar en bases existentes que tengan los roles antiguos.
-- Convierte ADMIN -> ADMINISTRADOR, OPERADOR -> BODEGUERO y CONSULTA -> VENDEDOR.
USE dacero;

-- El esquema viejo puede tener CHECK o ENUM. El CHECK se elimina si existe;
-- ENUM se convierte a VARCHAR en el ALTER siguiente.
SET @role_check_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'usuario_app'
      AND CONSTRAINT_NAME = 'chk_usuario_app_rol'
      AND CONSTRAINT_TYPE = 'CHECK'
);
SET @role_drop_sql = IF(@role_check_exists > 0,
    'ALTER TABLE usuario_app DROP CHECK chk_usuario_app_rol',
    'SELECT 1'
);
PREPARE role_drop_stmt FROM @role_drop_sql;
EXECUTE role_drop_stmt;
DEALLOCATE PREPARE role_drop_stmt;

ALTER TABLE usuario_app
    MODIFY rol VARCHAR(20) NOT NULL DEFAULT 'BODEGUERO';

UPDATE usuario_app
SET rol = CASE UPPER(rol)
    WHEN 'ADMIN' THEN 'ADMINISTRADOR'
    WHEN 'OPERADOR' THEN 'BODEGUERO'
    WHEN 'CONSULTA' THEN 'VENDEDOR'
    ELSE UPPER(rol)
END;

ALTER TABLE usuario_app
    ADD CONSTRAINT chk_usuario_app_rol
    CHECK (rol IN ('ADMINISTRADOR', 'VENDEDOR', 'BODEGUERO'));
