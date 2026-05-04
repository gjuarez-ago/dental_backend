package com.meyisoft.dental.system.repository;

import com.meyisoft.dental.system.enums.AppointmentStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Proyección de interfaz para optimizar la consulta de la Agenda (Citas).
 * Proporciona un mapeo eficiente y tipado para los listados.
 */
public interface CitaSummaryProjection {
    UUID getId();
    UUID getPacienteId();
    String getPacienteNombre();
    String getPacienteTelefono();
    UUID getDoctorId();
    String getDoctorNombre();
    UUID getSucursalId();
    UUID getServicioId();
    String getServicioNombre();
    OffsetDateTime getFechaHora();
    Integer getDuracionMinutos();
    AppointmentStatus getEstado();
    String getMotivoConsulta();
    String getNotasRecepcion();
    String getSource();
    String getFolio();
    BigDecimal getMontoTotal();
    BigDecimal getMontoPagado();
    BigDecimal getPrecioServicio();
    String getMotivoRechazo();
    Boolean getProcedimientoQuirurgico();
    OffsetDateTime getCreatedAt();
}
