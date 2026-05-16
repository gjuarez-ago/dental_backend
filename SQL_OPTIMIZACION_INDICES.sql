-- ===============================================
-- SCRIPT DE OPTIMIZACIÓN - ÍNDICES PARA SIGNUP
-- ===============================================
-- Ejecutar DESPUÉS de desplegarse la aplicación

-- CRITICAL: Índices para validación de unicidad (Email y Teléfono)
-- Reduce de ~100ms a ~1-5ms por lookup
CREATE INDEX IF NOT EXISTS idx_usuario_email_reg_borrado 
  ON usuario(email, reg_borrado) 
  WHERE reg_borrado = 1;

CREATE INDEX IF NOT EXISTS idx_usuario_telefono_activo 
  ON usuario(telefono_contacto, activo) 
  WHERE activo = true;

CREATE INDEX IF NOT EXISTS idx_paciente_telefono_reg 
  ON paciente(telefono, reg_borrado) 
  WHERE reg_borrado = 1;

CREATE INDEX IF NOT EXISTS idx_paciente_email_reg 
  ON paciente(email, reg_borrado) 
  WHERE reg_borrado = 1;

-- Índices para búsquedas por tenant_id (muy frecuentes)
CREATE INDEX IF NOT EXISTS idx_usuario_tenant_id_reg 
  ON usuario(tenant_id, reg_borrado);

CREATE INDEX IF NOT EXISTS idx_paciente_tenant_id_reg 
  ON paciente(tenant_id, reg_borrado);

CREATE INDEX IF NOT EXISTS idx_sucursal_tenant_id 
  ON sucursal(tenant_id);

-- Índice para búsqueda de OWNER por tenant
CREATE INDEX IF NOT EXISTS idx_usuario_tenant_rol 
  ON usuario(tenant_id, rol, reg_borrado);

-- Índice para verificar duplicados al registrar empresa
CREATE INDEX IF NOT EXISTS idx_empresa_id 
  ON empresa(id);

-- Índice para búsquedas de estado/municipio (si se usan frecuentemente)
-- CREATE INDEX IF NOT EXISTS idx_sucursal_estado 
--   ON sucursal(estado_id);
-- CREATE INDEX IF NOT EXISTS idx_sucursal_municipio 
--   ON sucursal(municipio_id);

-- ===============================================
-- VERIFICAR ÍNDICES CREADOS
-- ===============================================
-- Para verificar que los índices se crearon:
-- SELECT schemaname, tablename, indexname FROM pg_indexes 
-- WHERE tablename IN ('usuario', 'paciente', 'sucursal', 'empresa')
-- ORDER BY tablename, indexname;

-- ===============================================
-- ESTADÍSTICAS DE TABLAS (Importante para query planner)
-- ===============================================
ANALYZE usuario;
ANALYZE paciente;
ANALYZE sucursal;
ANALYZE empresa;

-- ===============================================
-- VERIFICAR TAMAÑO DE ÍNDICES
-- ===============================================
-- SELECT 
--   schemaname,
--   tablename,
--   indexname,
--   pg_size_pretty(pg_relation_size(indexrelid)) as index_size
-- FROM pg_stat_user_indexes
-- WHERE tablename IN ('usuario', 'paciente', 'sucursal', 'empresa')
-- ORDER BY pg_relation_size(indexrelid) DESC;
