package com.meyisoft.dental.system.repository;

import com.meyisoft.dental.system.entity.Bitacora;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BitacoraRepository extends JpaRepository<Bitacora, UUID> {
    
    Page<Bitacora> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    @Query("SELECT b.id as id, b.usuarioId as usuarioId, u.nombreCompleto as usuarioNombre, " +
           "b.modulo as modulo, b.accion as accion, b.descripcion as descripcion, " +
           "b.entidadRelacionadaId as entidadRelacionadaId, b.createdAt as createdAt " +
           "FROM Bitacora b " +
           "LEFT JOIN Usuario u ON b.usuarioId = u.id " +
           "WHERE b.tenantId = :tenantId " +
           "ORDER BY b.createdAt DESC")
    Page<BitacoraProjection> findProjectedByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);
}
