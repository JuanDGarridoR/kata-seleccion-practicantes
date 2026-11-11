-- Esquema de la base de datos (SQLite). Se ejecuta en cada arranque; IF NOT EXISTS lo hace idempotente.
-- Se define a mano en lugar de dejar que Hibernate lo genere porque el dialecto SQLite de
-- Hibernate 7 crea la columna id sin tipo, y en SQLite solo una PK INTEGER es autoincremental.

CREATE TABLE IF NOT EXISTS practicante (
    id                        INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre_completo           VARCHAR(120) NOT NULL,
    correo                    VARCHAR(150) NOT NULL UNIQUE,
    carrera                   VARCHAR(120) NOT NULL,
    semestre                  INTEGER      NOT NULL CHECK (semestre BETWEEN 1 AND 12),
    ruta_hoja_vida            VARCHAR(255) NOT NULL,
    nombre_original_hoja_vida VARCHAR(255) NOT NULL,
    estado                    VARCHAR(20)  NOT NULL CHECK (estado IN ('PENDIENTE', 'VIABLE', 'NO_VIABLE')),
    observacion               VARCHAR(500),
    fecha_registro            TIMESTAMP    NOT NULL,
    fecha_decision            TIMESTAMP,
    texto_extraible           BOOLEAN      NOT NULL DEFAULT 0
);

-- Habilidades detectadas en la hoja de vida (@ElementCollection de Practicante).
-- La PK compuesta evita repetir la misma habilidad para un practicante.
CREATE TABLE IF NOT EXISTS practicante_habilidad (
    practicante_id INTEGER     NOT NULL REFERENCES practicante (id),
    habilidad      VARCHAR(60) NOT NULL,
    PRIMARY KEY (practicante_id, habilidad)
);

-- Acelera el filtro "practicantes con la habilidad X".
CREATE INDEX IF NOT EXISTS idx_practicante_habilidad_habilidad ON practicante_habilidad (habilidad);
