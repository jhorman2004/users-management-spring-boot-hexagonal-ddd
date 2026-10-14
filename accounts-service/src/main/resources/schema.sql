-- =============================================
-- Base de datos del microservicio accounts
-- (en TiDB: ejecutar una vez en el SQL Editor; en local lo carga compose.yaml)
-- =============================================

CREATE DATABASE IF NOT EXISTS crud_cuentas
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE crud_cuentas;

-- Una cuenta por usuario. user_id es el id del usuario en crud_usuarios.users
-- (otra base, otro servicio: por eso no hay FOREIGN KEY).
CREATE TABLE IF NOT EXISTS accounts (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id     VARCHAR(36)  NOT NULL,
    status      ENUM('ACTIVE', 'SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_accounts_user_id UNIQUE (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Cuenta del administrador inicial (el usuario semilla de crud_usuarios)
INSERT IGNORE INTO accounts (id, user_id, status)
VALUES ('a0000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'ACTIVE');
