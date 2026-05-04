-- Script para sincronizar la tabla 'pacientes' con la entidad JPA
-- Ejecutar en PostgreSQL para resolver ERROR: column p1_0.aceptacion_privacidad does not exist

ALTER TABLE pacientes 
ADD COLUMN IF NOT EXISTS aceptacion_privacidad BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS fecha_aceptacion_privacidad TIMESTAMPTZ,
ADD COLUMN IF NOT EXISTS antecedentes_heredofamiliares TEXT,
ADD COLUMN IF NOT EXISTS antecedentes_no_patologicos TEXT,
ADD COLUMN IF NOT EXISTS codigo_postal_fiscal VARCHAR(5),
ADD COLUMN IF NOT EXISTS email_verificado BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS pin_cambiado BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS pin_hash VARCHAR(255),
ADD COLUMN IF NOT EXISTS razon_social_fiscal VARCHAR(255),
ADD COLUMN IF NOT EXISTS regimen_fiscal VARCHAR(255),
ADD COLUMN IF NOT EXISTS rfc VARCHAR(13);

-- Actualizaciones para servicios dentales
ALTER TABLE servicios_dentales ADD COLUMN IF NOT EXISTS procedimiento_quirurgico BOOLEAN DEFAULT FALSE;
