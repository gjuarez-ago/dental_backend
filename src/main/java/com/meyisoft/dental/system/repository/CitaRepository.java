package com.meyisoft.dental.system.repository;

import com.meyisoft.dental.system.entity.Cita;
import com.meyisoft.dental.system.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CitaRepository extends JpaRepository<Cita, UUID> {
        List<Cita> findByTenantIdAndRegBorrado(UUID tenantId, Integer regBorrado);

        List<Cita> findByPacienteIdAndRegBorrado(UUID pacienteId, Integer regBorrado);

        /**
         * Verifica que el paciente tenga al menos una cita creada recientemente.
         * Se usa para validar que la solicitud de setup-access proviene de alguien
         * que acaba de terminar un flujo de reserva legítimo.
         */
        @Query("SELECT COUNT(c) > 0 FROM Cita c JOIN Paciente p ON c.pacienteId = p.id " +
               "WHERE p.telefono = :telefono AND c.createdAt >= :desde AND c.regBorrado = 1")
        boolean existeCitaRecientePorTelefono(
                @Param("telefono") String telefono,
                @Param("desde") OffsetDateTime desde);

        List<Cita> findByTenantIdAndSucursalIdAndFechaHoraBetweenAndRegBorrado(
                        UUID tenantId, UUID sucursalId, OffsetDateTime start, OffsetDateTime end, Integer regBorrado);

        @Query("SELECT c FROM Cita c WHERE c.tenantId = :tenantId AND c.sucursalId = :sucursalId " +
                        "AND c.fechaHora BETWEEN :start AND :end AND c.regBorrado = :regBorrado " +
                        "AND (:doctorId IS NULL OR c.doctorId = :doctorId OR c.estado = 'POR_CONFIRMAR')")
        List<Cita> findByRangeWithDoctorFilter(
                        @Param("tenantId") UUID tenantId,
                        @Param("sucursalId") UUID sucursalId,
                        @Param("start") OffsetDateTime start,
                        @Param("end") OffsetDateTime end,
                        @Param("regBorrado") Integer regBorrado,
                        @Param("doctorId") UUID doctorId);

        @Query("SELECT COUNT(c) FROM Cita c WHERE c.tenantId = :tenantId AND c.sucursalId = :sucursalId " +
                        "AND c.fechaHora BETWEEN :start AND :end AND c.regBorrado = 1 " +
                        "AND c.estado NOT IN (com.meyisoft.dental.system.enums.AppointmentStatus.CANCELADA, com.meyisoft.dental.system.enums.AppointmentStatus.AUSENTE, com.meyisoft.dental.system.enums.AppointmentStatus.RECHAZADA) "
                        +
                        "AND (:doctorId IS NULL OR c.doctorId = :doctorId)")
        long countApptsByRange(
                        @Param("tenantId") UUID tenantId,
                        @Param("sucursalId") UUID sucursalId,
                        @Param("start") OffsetDateTime start,
                        @Param("end") OffsetDateTime end,
                        @Param("doctorId") UUID doctorId);

        List<Cita> findByTenantIdAndEstadoAndRegBorrado(UUID tenantId, AppointmentStatus estado, Integer regBorrado);

        List<Cita> findByTenantIdAndSucursalIdAndEstadoAndRegBorrado(UUID tenantId, UUID sucursalId,
                        AppointmentStatus estado, Integer regBorrado);

        long countByTenantIdAndFechaHoraBetweenAndRegBorrado(UUID tenantId, OffsetDateTime start, OffsetDateTime end,
                        Integer regBorrado);

        // --- MÉTODOS DE TRASLAPE (LÓGICA UNIFICADA) ---
        @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM citas c " +
                        "WHERE c.tenant_id = :tenantId AND c.doctor_id = :doctorId " +
                        "AND c.reg_borrado = 1 AND c.estado NOT IN ('CANCELADA', 'AUSENTE', 'RECHAZADA') " +
                        "AND c.fecha_hora < :end AND (c.fecha_hora + (COALESCE(c.duracion_minutos, 30) * interval '1 minute')) > :start")
        long countOverlapping(UUID tenantId, UUID doctorId, OffsetDateTime start, OffsetDateTime end);

        @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM citas c " +
                        "WHERE c.tenant_id = :tenantId AND c.paciente_id = :pacienteId " +
                        "AND c.reg_borrado = 1 AND c.estado NOT IN ('CANCELADA', 'AUSENTE', 'RECHAZADA') " +
                        "AND c.fecha_hora < :end AND (c.fecha_hora + (COALESCE(c.duracion_minutos, 30) * interval '1 minute')) > :start")
        long countOverlappingPaciente(UUID tenantId, UUID pacienteId, OffsetDateTime start, OffsetDateTime end);

        @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM citas c " +
                        "WHERE c.tenant_id = :tenantId AND c.sucursal_id = :sucursalId " +
                        "AND c.reg_borrado = 1 AND c.estado NOT IN ('CANCELADA', 'AUSENTE', 'RECHAZADA') " +
                        "AND c.fecha_hora < :end AND (c.fecha_hora + (COALESCE(c.duracion_minutos, 30) * interval '1 minute')) > :start")
        long countOverlappingSucursal(UUID tenantId, UUID sucursalId, OffsetDateTime start, OffsetDateTime end);

        @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM citas c " +
                        "WHERE c.tenant_id = :tenantId AND c.doctor_id = :doctorId AND c.id != :excludeId " +
                        "AND c.reg_borrado = 1 AND c.estado NOT IN ('CANCELADA', 'AUSENTE', 'RECHAZADA') " +
                        "AND c.fecha_hora < :end AND (c.fecha_hora + (COALESCE(c.duracion_minutos, 30) * interval '1 minute')) > :start")
        long countOverlappingExcludingId(UUID tenantId, UUID doctorId, OffsetDateTime start, OffsetDateTime end,
                        UUID excludeId);

        @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM citas c " +
                        "WHERE c.tenant_id = :tenantId AND c.paciente_id = :pacienteId AND c.id != :excludeId " +
                        "AND c.reg_borrado = 1 AND c.estado NOT IN ('CANCELADA', 'AUSENTE', 'RECHAZADA') " +
                        "AND c.fecha_hora < :end AND (c.fecha_hora + (COALESCE(c.duracion_minutos, 30) * interval '1 minute')) > :start")
        long countOverlappingPacienteExcludingId(UUID tenantId, UUID pacienteId, OffsetDateTime start,
                        OffsetDateTime end, UUID excludeId);

        @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM citas c " +
                        "WHERE c.tenant_id = :tenantId AND c.sucursal_id = :sucursalId AND c.id != :excludeId " +
                        "AND c.reg_borrado = 1 AND c.estado NOT IN ('CANCELADA', 'AUSENTE', 'RECHAZADA') " +
                        "AND c.fecha_hora < :end AND (c.fecha_hora + (COALESCE(c.duracion_minutos, 30) * interval '1 minute')) > :start")
        long countOverlappingSucursalExcludingId(UUID tenantId, UUID sucursalId, OffsetDateTime start,
                        OffsetDateTime end, UUID excludeId);

        @Query(nativeQuery = true, value = "SELECT COUNT(*) > 0 FROM citas c " +
                        "WHERE c.tenant_id = :tenantId AND c.doctor_id = :doctorId AND c.id != :excludeId " +
                        "AND c.reg_borrado = 1 AND c.estado NOT IN ('CANCELADA', 'AUSENTE', 'RECHAZADA') " +
                        "AND c.fecha_hora < :end AND (c.fecha_hora + (COALESCE(c.duracion_minutos, 30) * interval '1 minute')) > :start")
        boolean existsOverlappingExcludingId(UUID tenantId, UUID doctorId, OffsetDateTime start, OffsetDateTime end,
                        UUID excludeId);

        @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM citas c " +
                        "WHERE c.tenant_id = :tenantId AND c.sucursal_id = :sucursalId " +
                        "AND c.reg_borrado = 1 AND c.estado NOT IN ('CANCELADA', 'AUSENTE', 'RECHAZADA') " +
                        "AND c.fecha_hora < :end AND (c.fecha_hora + (COALESCE(c.duracion_minutos, 30) * interval '1 minute')) > :start")
        long countCitasEnRango(UUID tenantId, UUID sucursalId, OffsetDateTime start, OffsetDateTime end);

        @Query("SELECT c FROM Cita c WHERE c.tenantId = :tenantId AND c.sucursalId = :sucursalId " +
                        "AND c.regBorrado = 1 AND c.estado NOT IN (com.meyisoft.dental.system.enums.AppointmentStatus.CANCELADA, com.meyisoft.dental.system.enums.AppointmentStatus.AUSENTE, com.meyisoft.dental.system.enums.AppointmentStatus.RECHAZADA) "
                        +
                        "AND c.fechaHora >= :start AND c.fechaHora < :end")
        List<Cita> findAllActiveByTenantAndSucursalInRange(UUID tenantId, UUID sucursalId, OffsetDateTime start,
                        OffsetDateTime end);

        @Query("SELECT COUNT(c) FROM Cita c WHERE c.tenantId = :tenantId AND c.pacienteId = :pacienteId " +
                        "AND c.regBorrado = 1 AND c.estado NOT IN (com.meyisoft.dental.system.enums.AppointmentStatus.CANCELADA, com.meyisoft.dental.system.enums.AppointmentStatus.AUSENTE, com.meyisoft.dental.system.enums.AppointmentStatus.RECHAZADA) "
                        +
                        "AND c.fechaHora >= :start AND c.fechaHora < :end")
        long countActiveByPacienteAndDate(
                        @Param("tenantId") UUID tenantId,
                        @Param("pacienteId") UUID pacienteId,
                        @Param("start") OffsetDateTime start,
                        @Param("end") OffsetDateTime end);

        @Query("SELECT " +
                        "COUNT(CASE WHEN c.fechaHora BETWEEN :todayStart AND :todayEnd THEN 1 END) as citasHoy, " +
                        "COUNT(CASE WHEN c.fechaHora BETWEEN :yesterdayStart AND :yesterdayEnd THEN 1 END) as citasAyer " +
                        "FROM Cita c " +
                        "WHERE c.tenantId = :tenantId AND c.regBorrado = 1 " +
                        "AND c.estado NOT IN (com.meyisoft.dental.system.enums.AppointmentStatus.CANCELADA, com.meyisoft.dental.system.enums.AppointmentStatus.AUSENTE, com.meyisoft.dental.system.enums.AppointmentStatus.RECHAZADA) "
                        +
                        "AND (:sucursalId IS NULL OR c.sucursalId = :sucursalId) " +
                        "AND (:doctorId IS NULL OR c.doctorId = :doctorId)")
        DashboardCitaStats findDashboardCitaStats(
                        @Param("tenantId") UUID tenantId,
                        @Param("sucursalId") UUID sucursalId,
                        @Param("todayStart") OffsetDateTime todayStart,
                        @Param("todayEnd") OffsetDateTime todayEnd,
                        @Param("yesterdayStart") OffsetDateTime yesterdayStart,
                        @Param("yesterdayEnd") OffsetDateTime yesterdayEnd,
                        @Param("doctorId") UUID doctorId);

        @Query("SELECT c.id as id, c.pacienteId as pacienteId, p.nombreCompleto as pacienteNombre, p.telefono as pacienteTelefono, "
                        +
                        "c.doctorId as doctorId, d.nombreCompleto as doctorNombre, c.sucursalId as sucursalId, " +
                        "c.servicioId as servicioId, s.nombre as servicioNombre, c.fechaHora as fechaHora, " +
                        "c.duracionMinutos as duracionMinutos, c.estado as estado, c.motivoConsulta as motivoConsulta, "
                        +
                        "c.notasRecepcion as notasRecepcion, c.source as source, c.folio as folio, " +
                        "c.montoTotal as montoTotal, c.montoPagado as montoPagado, c.precioServicio as precioServicio, "
                        +
                        "c.motivoRechazo as motivoRechazo, c.createdAt as createdAt, s.procedimientoQuirurgico as procedimientoQuirurgico " +
                        "FROM Cita c " +
                        "LEFT JOIN Paciente p ON c.pacienteId = p.id " +
                        "LEFT JOIN Usuario d ON c.doctorId = d.id " +
                        "LEFT JOIN ServicioDental s ON c.servicioId = s.id " +
                        "WHERE c.tenantId = :tenantId AND c.sucursalId = :sucursalId " +
                        "AND c.fechaHora BETWEEN :start AND :end AND c.regBorrado = :regBorrado " +
                        "AND (:doctorId IS NULL OR c.doctorId = :doctorId OR c.estado = com.meyisoft.dental.system.enums.AppointmentStatus.POR_CONFIRMAR)")
        List<CitaSummaryProjection> findByRangeOptimized(
                        @Param("tenantId") UUID tenantId,
                        @Param("sucursalId") UUID sucursalId,
                        @Param("start") OffsetDateTime start,
                        @Param("end") OffsetDateTime end,
                        @Param("regBorrado") Integer regBorrado,
                        @Param("doctorId") UUID doctorId);

        @Query("SELECT c.id as id, c.pacienteId as pacienteId, p.nombreCompleto as pacienteNombre, p.telefono as pacienteTelefono, "
                        +
                        "c.doctorId as doctorId, d.nombreCompleto as doctorNombre, c.sucursalId as sucursalId, " +
                        "c.servicioId as servicioId, s.nombre as servicioNombre, c.fechaHora as fechaHora, " +
                        "c.duracionMinutos as duracionMinutos, c.estado as estado, c.motivoConsulta as motivoConsulta, "
                        +
                        "c.notasRecepcion as notasRecepcion, c.source as source, c.folio as folio, " +
                        "c.montoTotal as montoTotal, c.montoPagado as montoPagado, c.precioServicio as precioServicio, "
                        +
                        "c.motivoRechazo as motivoRechazo, c.createdAt as createdAt, s.procedimientoQuirurgico as procedimientoQuirurgico " +
                        "FROM Cita c " +
                        "LEFT JOIN Paciente p ON c.pacienteId = p.id " +
                        "LEFT JOIN Usuario d ON c.doctorId = d.id " +
                        "LEFT JOIN ServicioDental s ON c.servicioId = s.id " +
                        "WHERE c.tenantId = :tenantId AND c.estado = :estado AND c.regBorrado = 1")
        List<CitaSummaryProjection> findByTenantAndEstadoOptimized(
                        @Param("tenantId") UUID tenantId,
                        @Param("estado") AppointmentStatus estado);

        @Query("SELECT c.id as id, c.pacienteId as pacienteId, p.nombreCompleto as pacienteNombre, p.telefono as pacienteTelefono, "
                        +
                        "c.doctorId as doctorId, d.nombreCompleto as doctorNombre, c.sucursalId as sucursalId, " +
                        "c.servicioId as servicioId, s.nombre as servicioNombre, c.fechaHora as fechaHora, " +
                        "c.duracionMinutos as duracionMinutos, c.estado as estado, c.motivoConsulta as motivoConsulta, "
                        +
                        "c.notasRecepcion as notasRecepcion, c.source as source, c.folio as folio, " +
                        "c.montoTotal as montoTotal, c.montoPagado as montoPagado, c.precioServicio as precioServicio, "
                        +
                        "c.motivoRechazo as motivoRechazo, c.createdAt as createdAt, s.procedimientoQuirurgico as procedimientoQuirurgico " +
                        "FROM Cita c " +
                        "LEFT JOIN Paciente p ON c.pacienteId = p.id " +
                        "LEFT JOIN Usuario d ON c.doctorId = d.id " +
                        "LEFT JOIN ServicioDental s ON c.servicioId = s.id " +
                        "WHERE c.tenantId = :tenantId AND c.sucursalId = :sucursalId AND c.estado = :estado AND c.regBorrado = 1")
        List<CitaSummaryProjection> findByTenantAndSucursalAndEstadoOptimized(
                        @Param("tenantId") UUID tenantId,
                        @Param("sucursalId") UUID sucursalId,
                        @Param("estado") AppointmentStatus estado);

        @Query("SELECT c.id as id, c.doctorId as doctorId, c.montoTotal as montoTotal, c.montoPagado as montoPagado, " +
                        "p.nombreCompleto as pacienteNombre, s.nombre as servicioNombre, s.precioBase as precioBase " +
                        "FROM Cita c " +
                        "LEFT JOIN Paciente p ON c.pacienteId = p.id " +
                        "LEFT JOIN ServicioDental s ON c.servicioId = s.id " +
                        "WHERE c.id = :citaId AND c.tenantId = :tenantId AND c.regBorrado = 1")
        Optional<java.util.Map<String, Object>> findFinancialSummary(
                        @Param("citaId") UUID citaId,
                        @Param("tenantId") UUID tenantId);

        @Query("SELECT c.id as citaId, c.fechaHora as fechaHora, c.folio as folio, " +
                        "p.id as pacienteId, p.nombreCompleto as pacienteNombre, p.telefono as pacienteTelefono, p.email as pacienteEmail, " +
                        "d.id as doctorId, d.nombreCompleto as doctorNombre, d.email as doctorEmail, d.sucursalIdPrincipal as doctorSucursalPrincipalId, " +
                        "s.nombre as servicioNombre, " +
                        "suc.nombreSucursal as sucursalNombre, suc.telefono as sucursalTelefono " +
                        "FROM Cita c " +
                        "JOIN Paciente p ON c.pacienteId = p.id " +
                        "JOIN Usuario d ON c.doctorId = d.id " +
                        "LEFT JOIN ServicioDental s ON c.servicioId = s.id " +
                        "JOIN Sucursal suc ON c.sucursalId = suc.id " +
                        "WHERE c.fechaHora BETWEEN :start AND :end " +
                        "AND c.regBorrado = 1 " +
                        "AND c.estado IN :estados")
        List<CitaCronProjection> findForCronByRange(
                        @Param("start") OffsetDateTime start,
                        @Param("end") OffsetDateTime end,
                        @Param("estados") java.util.Collection<AppointmentStatus> estados);
}
