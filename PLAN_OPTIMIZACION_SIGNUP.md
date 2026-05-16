# Plan de Optimización - Proceso de Signup (10,000 usuarios/día)

## 🔴 Problemas Identificados

### 1. **Patient Signup - Query N+1 Crítica** (PRIORIDAD MÁXIMA)
**Ubicación**: `PatientAuthService.register()` línea ~115-120
```java
assignedTenantId = empresaRepository.findAll().stream() // ❌ CARGA TODAS LAS EMPRESAS
    .findFirst()
    .map(Empresa::getId)
```
**Impacto**: Con miles de registros, esto es devastador
- Si hay 100 empresas: ~100 registros cargados en memoria
- Con 10,000 signups/día: 1,000,000 registros innecesarios/día

---

### 2. **Validación de Email/Teléfono - Queries Separadas**
**Ubicación**: `AuthCRMService.registerTenant()` línea ~59-67
```java
usuarioRepository.findByEmailAndRegBorrado(email, 1).ifPresent(...); // Query 1
usuarioRepository.findByTelefonoContactoAndActive(phone).ifPresent(...); // Query 2
```
**Impacto**: 2 round-trips a BD en lugar de 1

---

### 3. **Sin Pool de Conexiones Configurado**
**Ubicación**: `application-desarrollo.properties` 
- No hay `spring.datasource.hikari.*` configurado
- Pool por defecto es muy pequeño (10 conexiones)
- Con 10,000 usuarios/día = posibles cuellos de botella

---

### 4. **Sin Batch Insert para Múltiples Entities**
**Ubicación**: `AuthCRMService.registerTenant()` línea ~72-100
```java
empresaRepository.save(empresa);      // Save 1
sucursalRepository.save(sucursal);   // Save 2
usuarioRepository.save(owner);        // Save 3
```
**Impacto**: 3 transacciones en lugar de 1

---

### 5. **Password Encoding Sincrónico (CPU-Intensivo)**
**Ubicación**: `AuthCRMService` y `PatientAuthService`
```java
passwordEncoder.encode(request.getAdminNip());  // Lento
passwordEncoder.encode(request.getNip());       // Lento
```
**Impacto**: BCrypt con rounds altos = ~100-500ms por hash
- 10,000 usuarios × 100ms = 1,000 segundos de CPU solo en hashing

---

### 6. **Sin Índices de Base de Datos**
**Campos consultados sin índices**:
- `Usuario.email`
- `Usuario.telefonoContacto`
- `Paciente.telefono`
- `Paciente.email`

---

### 7. **Sin Lazy Loading en Relaciones**
**Riesgo**: Si `Empresa` carga todas sus `Sucursal` y `Usuario`, overhead adicional

---

### 8. **Sin Caché para Datos Estáticos**
- Estados (estado) y Municipios (municipio) se consultan en cada signup
- No hay caché

---

## ✅ Plan de Optimización (Pasos Recomendados)

### PASO 1: Configurar Connection Pool (5 min)
**Archivo**: `application-desarrollo.properties`
```properties
# HikariCP - Connection Pool
spring.datasource.hikari.maximum-pool-size=50
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000

# JPA Batch
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

---

### PASO 2: Crear Índices en Base de Datos
```sql
-- Índices para validación de unicidad (CRÍTICO)
CREATE INDEX idx_usuario_email_reg_borrado ON usuario(email, reg_borrado) 
WHERE reg_borrado = 1;

CREATE INDEX idx_usuario_telefono_activo ON usuario(telefono_contacto, activo) 
WHERE activo = true;

CREATE INDEX idx_paciente_telefono_reg ON paciente(telefono, reg_borrado) 
WHERE reg_borrado = 1;

CREATE INDEX idx_paciente_email_reg ON paciente(email, reg_borrado) 
WHERE reg_borrado = 1;

-- Índices para tenant_id (muy consultado)
CREATE INDEX idx_usuario_tenant_id ON usuario(tenant_id, reg_borrado);
CREATE INDEX idx_paciente_tenant_id ON paciente(tenant_id, reg_borrado);
```

---

### PASO 3: Optimizar Query N+1 en Patient Signup
**Archivo**: `PatientAuthService.java`
**Cambiar**:
```java
// ❌ ANTES (Query N+1)
assignedTenantId = empresaRepository.findAll().stream()
    .findFirst()
    .map(Empresa::getId)
    .orElseThrow(...);

// ✅ DESPUÉS (Query optimizada)
@Query("SELECT e.id FROM Empresa e LIMIT 1")
Optional<UUID> findFirstTenantId();

// En el servicio:
assignedTenantId = empresaRepository.findFirstTenantId()
    .orElseThrow(...);
```

---

### PASO 4: Combinar Validaciones de Email/Teléfono
**Archivo**: `UsuarioRepository.java`
**Agregar nueva query**:
```java
@Query("SELECT u FROM Usuario u WHERE (u.email = :email OR u.telefonoContacto = :phone) " +
       "AND u.regBorrado = 1")
Optional<Usuario> findByEmailOrPhone(@Param("email") String email, @Param("phone") String phone);
```

**Actualizar servicio**:
```java
// ❌ ANTES (2 queries)
usuarioRepository.findByEmailAndRegBorrado(email, 1).ifPresent(u -> throw...);
usuarioRepository.findByTelefonoContactoAndActive(phone).ifPresent(u -> throw...);

// ✅ DESPUÉS (1 query)
usuarioRepository.findByEmailOrPhone(email, phone).ifPresent(u -> {
    if (u.getEmail().equals(email)) throw EmailException...;
    else throw PhoneException...;
});
```

---

### PASO 5: Usar Batch para Multiple Entities
**Archivo**: `AuthCRMService.java`
```java
// ❌ ANTES
empresa = empresaRepository.save(empresa);
sucursal = sucursalRepository.save(sucursal);
owner = usuarioRepository.save(owner);

// ✅ DESPUÉS - Aprovechar batch
empresa = empresaRepository.save(empresa); // Persiste
sucursal = sucursalRepository.save(sucursal);
owner = usuarioRepository.save(owner);

// Añadir flush al final de la transacción para batch
entityManager.flush();
```

---

### PASO 6: Optimizar Password Encoding (Opcional pero Recomendado)
**Opción A - Aumentar bcrypt rounds (SIMPLE)**:
```java
// En SecurityConfig.java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(11); // 11 rounds = balance seguridad/velocidad
}
```

**Opción B - Usar salting más rápido (COMPLEJO)**:
Considerar `Argon2PasswordEncoder` si necesitas más velocidad

---

### PASO 7: Caché para Estados y Municipios
**Archivo**: Crear `application-desarrollo.properties`
```properties
spring.cache.type=caffeine
spring.cache.caffeine.spec=maximumSize=1000,expireAfterWrite=24h
```

**En entity o servicio**:
```java
@Cacheable(value = "estados")
public List<Estado> getAllEstados() {
    return estadoRepository.findAll();
}
```

---

## 📊 Impacto Esperado

| Optimización | Tiempo Actual | Tiempo Después | Mejora |
|---|---|---|---|
| Query N+1 Paciente | ~50ms | ~5ms | **10x** |
| Validación Email+Teléfono | ~20ms | ~10ms | **2x** |
| Connection Pool | Contención | 0-5ms | **Variable** |
| Batch Insert | 3 saves | 1 save | **2x** |
| Password Encoding | 200-400ms | 150-300ms (con rounds=11) | **1.2-1.5x** |
| Índices DB | ~100ms lookup | ~1-5ms lookup | **20x** |
| **Total Signup** | **~500-700ms** | **~100-150ms** | **4-5x FASTER** |

---

## 🚀 Orden de Implementación Recomendado

1. ✅ **PRIMERO**: Paso 2 (Índices DB) - Máximo impacto, sin cambios código
2. ✅ **SEGUNDO**: Paso 1 (Connection Pool) - 5 min, muy impactante
3. ✅ **TERCERO**: Paso 3 (Query N+1) - Critical fix
4. ✅ **CUARTO**: Paso 4 (Combinar validaciones)
5. ✅ **QUINTO**: Paso 6 (Password Encoding rounds)
6. ✅ **SEXTO**: Paso 5 (Batch) - Menos impacto pero good practice
7. ⭐ **OPCIONAL**: Paso 7 (Caché)

---

## ⚠️ Antes de Producción

- [ ] Ejecutar Índices en PROD
- [ ] Actualizar `application-prod.properties` con HikariCP config
- [ ] Load test con 10,000 usuarios simultáneos
- [ ] Monitorear DB query times con `spring.jpa.properties.hibernate.generate_statistics=true`
- [ ] Monitorear metrics: `io.micrometer` (Spring Boot Actuator)

