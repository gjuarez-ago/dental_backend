package com.meyisoft.dental.system.repository;

import com.meyisoft.dental.system.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    @Query("SELECT u FROM Usuario u WHERE u.telefonoContacto = :phone AND u.regBorrado = 1")
    Optional<Usuario> findByTelefonoContactoAndActive(@Param("phone") String phone);

    @Query("SELECT u FROM Usuario u WHERE u.tenantId = :tenantId AND u.regBorrado = 1")
    Iterable<Usuario> findAllByTenantId(@Param("tenantId") UUID tenantId);

    java.util.List<Usuario> findByTenantIdAndRolAndRegBorrado(UUID tenantId, com.meyisoft.dental.system.enums.UserRole rol, Integer regBorrado);

    long countByTenantIdAndRolAndRegBorrado(UUID tenantId, com.meyisoft.dental.system.enums.UserRole rol, Integer regBorrado);

    java.util.List<Usuario> findByTenantIdAndSucursalIdPrincipalAndRegBorrado(UUID tenantId, UUID sucursalId, Integer regBorrado);

    java.util.List<Usuario> findByTenantIdAndRegBorrado(UUID tenantId, Integer regBorrado);

    java.util.Optional<Usuario> findByEmailAndRegBorrado(String email, Integer regBorrado);

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.tenantId = :tenantId AND u.esPersonalClinico = true AND u.regBorrado = 1")
    long countClinicalStaffByTenant(@Param("tenantId") UUID tenantId);

    java.util.List<Usuario> findByRolAndActivoAndRegBorrado(com.meyisoft.dental.system.enums.UserRole rol, Boolean activo, Integer regBorrado);

    // OPTIMIZACIÓN: Combinar búsqueda de email y teléfono en una sola query
    // Reducir de 2 queries a 1, evitando validaciones duplicadas
    @Query("SELECT u FROM Usuario u WHERE (LOWER(u.email) = LOWER(:email) OR u.telefonoContacto = :phone) AND u.regBorrado = 1")
    Optional<Usuario> findByEmailOrPhoneAndRegBorrado(@Param("email") String email, @Param("phone") String phone);
}
