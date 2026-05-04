package com.meyisoft.dental.system.models.dto;

import com.meyisoft.dental.system.enums.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaPatientDTO {
    private UUID id;
    private String folio;
    private String clinicaNombre;
    private String sucursalNombre;
    private String sucursalTelefono;
    private String servicioNombre;
    private String doctorNombre;
    private String doctorGenero;
    private OffsetDateTime fechaHora;
    private AppointmentStatus estado;
    
    // Información financiera
    private BigDecimal montoBase; // Precio base según el servicio
    private BigDecimal montoPagado;
    private BigDecimal saldoPendiente;
    
    // Acciones permitidas
    private boolean permiteCancelar;
    private String mensajeCancelacion; // Razón si no se puede cancelar
}
