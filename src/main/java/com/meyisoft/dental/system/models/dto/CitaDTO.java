package com.meyisoft.dental.system.models.dto;

import com.meyisoft.dental.system.enums.AppointmentStatus;
import com.meyisoft.dental.system.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaDTO {
    private UUID id;
    private UUID pacienteId;
    private String pacienteNombre; // Auxiliar para el frontend
    private String pacienteTelefono; // Para perfiles incompletos desde App
    private UUID doctorId;
    private String doctorNombre; // Auxiliar para el frontend
    private UUID sucursalId;
    private UUID servicioId;
    private String servicioNombre; // Auxiliar para el frontend
    private OffsetDateTime fechaHora;
    private Integer duracionMinutos;
    private AppointmentStatus estado;
    private String motivoConsulta;
    private String notasRecepcion;
    private String source; // APP, CRM
    private String folio;

    // Información financiera básica
    private java.math.BigDecimal montoTotal;
    private java.math.BigDecimal montoPagado;
    private java.math.BigDecimal precioServicio;
    private TicketStatus ticketStatus;
    private String motivoRechazo;
    private String comprobanteUrl;
    private String referenciaPago;
    private Boolean procedimientoQuirurgico;
    private OffsetDateTime createdAt;
}
