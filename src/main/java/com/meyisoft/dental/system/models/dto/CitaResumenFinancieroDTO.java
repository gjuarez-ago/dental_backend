package com.meyisoft.dental.system.models.dto;

import com.meyisoft.dental.system.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaResumenFinancieroDTO {

    private UUID citaId;
    private UUID doctorId;
    private String pacienteNombre;
    private String servicioNombre;
    private BigDecimal precioBase;
    private BigDecimal totalPagado;
    private BigDecimal saldoPendiente;
    private boolean costoDefinido;
    private TicketStatus estadoTicket;
    private List<PagoDTO> historialPagos;

}
