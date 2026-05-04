package com.meyisoft.dental.system.models.dto;

import com.meyisoft.dental.system.enums.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineEntryDTO {
    private UUID citaId;
    private String folio;
    private OffsetDateTime fecha;
    private String servicioNombre;
    private String doctorNombre;
    private String doctorGenero;
    private AppointmentStatus estado;
    
    // Información Clínica
    private String diagnostico;
    private String procedimiento;
    private String recomendaciones;
    private List<MedicamentoDTO> medicamentos;
    private Integer duracionMinutos;
    
    // Información Financiera
    private BigDecimal montoTotal;
    private BigDecimal montoPagado;
    private BigDecimal saldoPendiente;
}
