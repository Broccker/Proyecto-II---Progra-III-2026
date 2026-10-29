-- =====================================================================
-- Proyecto #2 - Sistema de Reserva de Recursos
-- EIF206 Programación 3 (2026-II) - Universidad Nacional
--
-- Script de creación de la base de datos (MySQL 8.4 LTS).
--
-- ADVERTENCIA: este script elimina la base "proyectoII_reservas" si ya
-- existe, junto con todos sus datos. Es la forma de garantizar que el
-- script se pueda ejecutar repetidamente desde cero.
--
-- Uso:
--   mysql -u root -p --default-character-set=utf8mb4 < schema.sql
-- =====================================================================

DROP DATABASE IF EXISTS proyectoII_reservas;

CREATE DATABASE proyectoII_reservas
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE proyectoII_reservas;

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- usuarios
-- Administradores y funcionarios en una sola tabla (el rol los distingue).
-- La clave se guarda SIEMPRE como hash BCrypt, nunca en texto plano.
-- Un administrador no tiene nombre ni teléfono; un funcionario sí.
-- ---------------------------------------------------------------------
CREATE TABLE usuarios (
    id          VARCHAR(20)  NOT NULL,
    clave_hash  VARCHAR(100) NOT NULL,
    rol         ENUM('ADMINISTRADOR', 'FUNCIONARIO') NOT NULL,
    nombre      VARCHAR(100) NULL,
    telefono    VARCHAR(20)  NULL,
    creado_en   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_usuarios_datos_funcionario CHECK (
        (rol = 'FUNCIONARIO' AND nombre IS NOT NULL AND telefono IS NOT NULL)
        OR (rol = 'ADMINISTRADOR' AND nombre IS NULL AND telefono IS NULL)
    ),
    CONSTRAINT chk_usuarios_telefono CHECK (
        telefono IS NULL OR telefono REGEXP '^[+]?[0-9]{7,15}$'
    )
) ENGINE = InnoDB
  COMMENT = 'Administradores y funcionarios del sistema.';

-- ---------------------------------------------------------------------
-- categorias
-- La descripción es única (comparación sin distinguir mayúsculas ni
-- acentos, por la colación utf8mb4_0900_ai_ci).
-- ---------------------------------------------------------------------
CREATE TABLE categorias (
    id          VARCHAR(20)  NOT NULL,
    descripcion VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_categorias_descripcion UNIQUE (descripcion)
) ENGINE = InnoDB
  COMMENT = 'Categorías de recursos (proyectores, salas, computadoras, etc.).';

-- ---------------------------------------------------------------------
-- recursos
-- ON UPDATE CASCADE permite renombrar el id de una categoría o recurso
-- propagando el cambio a las referencias, como hacía el Proyecto #1.
-- ON DELETE RESTRICT impide borrar una categoría con recursos.
-- ---------------------------------------------------------------------
CREATE TABLE recursos (
    id           VARCHAR(20)  NOT NULL,
    categoria_id VARCHAR(20)  NOT NULL,
    descripcion  VARCHAR(150) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_recursos_categoria_descripcion UNIQUE (categoria_id, descripcion),
    CONSTRAINT fk_recursos_categoria FOREIGN KEY (categoria_id)
        REFERENCES categorias (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE = InnoDB
  COMMENT = 'Recursos físicos reservables, agrupados por categoría.';

-- ---------------------------------------------------------------------
-- reservas
-- El identificador se genera en el backend (RES-000001, ...).
-- El estado CANCELADA conserva el histórico; las estadísticas y la
-- disponibilidad solo consideran las reservas ACTIVAS.
--
-- La detección de traslapes NO se puede expresar como constraint en
-- MySQL: se valida en el backend dentro de una transacción con bloqueo
-- (SELECT ... FOR UPDATE) para que dos usuarios no reserven el mismo
-- recurso a la misma hora.
-- ---------------------------------------------------------------------
CREATE TABLE reservas (
    id             VARCHAR(20)  NOT NULL,
    funcionario_id VARCHAR(20)  NOT NULL,
    actividad      VARCHAR(150) NOT NULL,
    fecha          DATE         NOT NULL,
    hora_inicio    TIME         NOT NULL,
    hora_fin       TIME         NOT NULL,
    estado         ENUM('ACTIVA', 'CANCELADA') NOT NULL DEFAULT 'ACTIVA',
    creada_en      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizada_en TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
                                ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_reservas_horario CHECK (hora_fin > hora_inicio),
    CONSTRAINT fk_reservas_funcionario FOREIGN KEY (funcionario_id)
        REFERENCES usuarios (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    INDEX idx_reservas_fecha (fecha),
    INDEX idx_reservas_estado_fecha (estado, fecha)
) ENGINE = InnoDB
  COMMENT = 'Reservas de uno o varios recursos para una actividad.';

-- ---------------------------------------------------------------------
-- reserva_recursos
-- Relación N:M entre reservas y recursos (detalle de la reserva).
-- ON DELETE CASCADE borra el detalle si se elimina la reserva; los
-- recursos con reservas asociadas no se pueden borrar (RESTRICT).
-- ---------------------------------------------------------------------
CREATE TABLE reserva_recursos (
    reserva_id VARCHAR(20) NOT NULL,
    recurso_id VARCHAR(20) NOT NULL,
    PRIMARY KEY (reserva_id, recurso_id),
    CONSTRAINT fk_reserva_recursos_reserva FOREIGN KEY (reserva_id)
        REFERENCES reservas (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_reserva_recursos_recurso FOREIGN KEY (recurso_id)
        REFERENCES recursos (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    INDEX idx_reserva_recursos_recurso (recurso_id)
) ENGINE = InnoDB
  COMMENT = 'Detalle de los recursos incluidos en cada reserva.';
