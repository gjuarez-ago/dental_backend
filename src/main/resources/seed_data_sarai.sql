-- SCRIPT DE CARGA INICIAL: CLIENTE SARAI RIOS
-- FECHA: 2026-04-12
-- NOTA: Se ha añadido el campo EMAIL para notificaciones y contacto.

-- 1. LIMPIEZA PREVIA (Opcional)
-- DELETE FROM usuarios WHERE tenant_id = '550e8400-e29b-41d4-a716-446655440000';
-- DELETE FROM sucursales WHERE tenant_id = '550e8400-e29b-41d4-a716-446655440000';
-- DELETE FROM empresas WHERE id = '550e8400-e29b-41d4-a716-446655440000';

-- 2. CREAR EMPRESA (TENANT)
INSERT INTO empresas (id, nombre_comercial, plan_suscripcion, estado_cuenta, reg_borrado, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440000', 
    'Sarai Rios - Dental Sonrisana', 
    'ELITE', 
    true, 
    1, 
    NOW(), 
    NOW()
);

-- 3. CREAR SUCURSAL (Con datos bancarios y contacto)
INSERT INTO sucursales (id, tenant_id, nombre_sucursal, horarios_laborales, banco, cuenta_bancaria, clabe_interbancaria, telefono, ventana_cancelacion, reg_borrado, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440001', 
    '550e8400-e29b-41d4-a716-446655440000', 
    'Matriz Centro', 
    '[{"day": "Lunes", "enabled": false, "startTime": "09:00", "endTime": "18:00"}, {"day": "Martes", "enabled": true, "startTime": "09:00", "endTime": "18:00"}, {"day": "Miércoles", "enabled": true, "startTime": "09:00", "endTime": "18:00"}, {"day": "Jueves", "enabled": true, "startTime": "09:00", "endTime": "18:00"}, {"day": "Viernes", "enabled": true, "startTime": "09:00", "endTime": "18:00"}, {"day": "Sábado", "enabled": true, "startTime": "09:00", "endTime": "14:00"}, {"day": "Domingo", "enabled": false, "startTime": "09:00", "endTime": "18:00"}]', 
    'BBVA México',
    '1234567890',
    '012345678901234567',
    '9991234567',
    24, 
    1, 
    NOW(), 
    NOW()
);

-- 4. CREAR USUARIOS
-- Sarai Rios (DUEÑA / ADMIN)
INSERT INTO usuarios (id, tenant_id, sucursal_id_principal, rol, telefono_contacto, email, nip_hash, requiere_cambio_nip, nombre_completo, cedula_profesional, es_personal_clinico, reg_borrado, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440002', 
    '550e8400-e29b-41d4-a716-446655440000', 
    '550e8400-e29b-41d4-a716-446655440001', 
    'OWNER', 
    '9991234567', 
    'sarai.rios@sonrisana.site',
    '$2a$10$8.IlnEVC.O776G1/Z1lV2uW3r5E.iGkH.0e/ZzU8D9O3a.0E6VpEi', 
    false, 
    'Sarai Rios', 
    'DENT-SR-2024-X', 
    true,
    1, 
    NOW(), 
    NOW()
);

-- Dr. Julián Galavis (MÉDICO)
INSERT INTO usuarios (id, tenant_id, sucursal_id_principal, rol, telefono_contacto, email, nip_hash, requiere_cambio_nip, nombre_completo, cedula_profesional, es_personal_clinico, reg_borrado, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440003', 
    '550e8400-e29b-41d4-a716-446655440000', 
    '550e8400-e29b-41d4-a716-446655440001', 
    'DOCTOR', 
    '9997654321', 
    'julian.galavis@dentalstudio.com',
    '$2a$10$8.IlnEVC.O776G1/Z1lV2uW3r5E.iGkH.0e/ZzU8D9O3a.0E6VpEi', 
    true, 
    'Dr. Julián Galavis', 
    'MED-JG-1234567', 
    true,
    1, 
    NOW(), 
    NOW()
);

-- Recepcionista Elena (REPCESIONISTA)
INSERT INTO usuarios (id, tenant_id, sucursal_id_principal, rol, telefono_contacto, email, nip_hash, requiere_cambio_nip, nombre_completo, es_personal_clinico, reg_borrado, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440004', 
    '550e8400-e29b-41d4-a716-446655440000', 
    '550e8400-e29b-41d4-a716-446655440001', 
    'RECEPTIONIST', 
    '9990001122', 
    'recepcion.centro@dentalstudio.com',
    '$2a$10$8.IlnEVC.O776G1/Z1lV2uW3r5E.iGkH.0e/ZzU8D9O3a.0E6VpEi', 
    false, 
    'Elena Ruiz', 
    false,
    1, 
    NOW(), 
    NOW()
);

-- 5. CREAR SERVICIOS DENTALES
INSERT INTO servicios_dentales (id, tenant_id, nombre, descripcion, precio_base, duracion_minutos, color_etiqueta, reg_borrado, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440005',
    '550e8400-e29b-41d4-a716-446655440000',
    'Limpieza Dental Profunda',
    'Eliminación de sarro y placa bacteriana con ultrasonido.',
    550.00,
    45,
    '#10B981',
    1,
    NOW(),
    NOW()
);

INSERT INTO servicios_dentales (id, tenant_id, nombre, descripcion, precio_base, duracion_minutos, color_etiqueta, reg_borrado, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440006',
    '550e8400-e29b-41d4-a716-446655440000',
    'Consulta General',
    'Evaluación diagnóstica y presupuesto de tratamiento.',
    200.00,
    30,
    '#6366F1',
    1,
    NOW(),
    NOW()
);
