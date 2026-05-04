package com.meyisoft.dental.system.repository;

import com.meyisoft.dental.system.entity.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, UUID> {
    

    @Query("SELECT " +
           "COUNT(CASE WHEN p.createdAt BETWEEN :todayStart AND :todayEnd THEN 1 END) as pacientesHoy, " +
           "COUNT(CASE WHEN p.createdAt BETWEEN :yesterdayStart AND :yesterdayEnd THEN 1 END) as pacientesAyer " +
           "FROM Paciente p " +
           "WHERE p.tenantId = :tenantId AND p.regBorrado = 1")
    DashboardPacienteStats findDashboardPacienteStats(
            @Param("tenantId") UUID tenantId,
            @Param("todayStart") OffsetDateTime todayStart,
            @Param("todayEnd") OffsetDateTime todayEnd,
            @Param("yesterdayStart") OffsetDateTime yesterdayStart,
            @Param("yesterdayEnd") OffsetDateTime yesterdayEnd);

    List<Paciente> findByTenantIdAndRegBorrado(UUID tenantId, Integer regBorrado);
    
    Optional<Paciente> findByIdAndTenantIdAndRegBorrado(UUID id, UUID tenantId, Integer regBorrado);
    
    Optional<Paciente> findByTelefonoAndTenantIdAndRegBorrado(String telefono, UUID tenantId, Integer regBorrado);

    Optional<Paciente> findByEmailAndTenantIdAndRegBorrado(String email, UUID tenantId, Integer regBorrado);

    /** Búsqueda global (Cross-tenant) para Login */
    Optional<Paciente> findFirstByTelefonoAndRegBorrado(String telefono, Integer regBorrado);

    /** Búsqueda global (Cross-tenant) para Portal */
    List<Paciente> findAllByEmailAndRegBorrado(String email, Integer regBorrado);
    
    List<Paciente> findAllByTelefonoAndRegBorrado(String telefono, Integer regBorrado);
    
    @Query(value = "SELECT p FROM Paciente p WHERE p.telefono = :telefono AND p.tenantId = :tenantId AND p.regBorrado = :regBorrado AND p.id != :excludeId")
    Optional<Paciente> findByTelefonoDuplicate(String telefono, UUID tenantId, Integer regBorrado, UUID excludeId);

    @Query(value = "SELECT p FROM Paciente p WHERE p.email = :email AND p.tenantId = :tenantId AND p.regBorrado = :regBorrado AND p.id != :excludeId")
    Optional<Paciente> findByEmailDuplicate(String email, UUID tenantId, Integer regBorrado, UUID excludeId);

    /**
     * Consulta optimizada que trae a los pacientes con su próxima cita (si existe)
     * ordenados por:
     * 1. Cercanía de la próxima cita (Ascendente).
     * 2. Fecha de creación (Descendente) para los que no tienen cita.
     */
    @Query(value = "SELECT p.*, " +
           "(SELECT MIN(c.fecha_hora) FROM citas c WHERE c.paciente_id = p.id AND c.fecha_hora >= NOW() AND c.reg_borrado = 1) as proxima_cita " +
           "FROM pacientes p " +
           "WHERE p.tenant_id = :tenantId AND p.reg_borrado = 1 " +
           "ORDER BY " +
           "CASE WHEN (SELECT MIN(c.fecha_hora) FROM citas c WHERE c.paciente_id = p.id AND c.fecha_hora >= NOW() AND c.reg_borrado = 1) IS NOT NULL THEN 0 ELSE 1 END, " +
           "(SELECT MIN(c.fecha_hora) FROM citas c WHERE c.paciente_id = p.id AND c.fecha_hora >= NOW() AND c.reg_borrado = 1) ASC, " +
           "p.created_at DESC", 
           nativeQuery = true)
    List<Paciente> findListadoPriorizado(@Param("tenantId") UUID tenantId);

    long countByTenantIdAndCreatedAtBetweenAndRegBorrado(UUID tenantId, java.time.OffsetDateTime start, java.time.OffsetDateTime end, Integer regBorrado);
}
