package com.meyisoft.dental.system.models.dto;

import com.meyisoft.dental.system.enums.AppointmentStatus;
import com.meyisoft.dental.system.enums.TicketStatus;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
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
    @Size(max = 150, message = "El nombre del paciente no puede exceder los 150 caracteres")
    private String pacienteNombre; // Auxiliar para el frontend

    @Size(max = 15, message = "El teléfono del paciente no puede exceder los 15 caracteres")
    private String pacienteTelefono; // Para perfiles incompletos desde App
    private UUID doctorId;
    private String doctorNombre; // Auxiliar para el frontend
    private UUID sucursalId;
    private UUID servicioId;
    private String servicioNombre; // Auxiliar para el frontend
    private OffsetDateTime fechaHora;
    private Integer duracionMinutos;
    private AppointmentStatus estado;
    @Size(max = 500, message = "El motivo de consulta no puede exceder los 500 caracteres")
    private String motivoConsulta;

    @Size(max = 1000, message = "Las notas de recepción no pueden exceder los 1000 caracteres")
    private String notasRecepcion;

    @Size(max = 20)
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
