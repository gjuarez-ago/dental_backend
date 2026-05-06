package com.meyisoft.dental.system.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalConfigDTO {
    
    // De la tabla Usuario
    private String nombreCompleto;
    private String cedulaProfesional;
    
    // De la tabla Sucursal
    private Integer ventanaCancelacion;
    private Map<String, DayConfigDTO> horarios;
    private String banco;
    private String cuentaBancaria;
    private String clabeInterbancaria;
    private String direccionSucursal;
    
    // De la tabla Empresa (Global)
    private String telefonoWhatsApp;
    private Integer horasAnticipacionCancelacion;
    private Integer diasAnticipacionReserva;
    private String isotypeUrl;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayConfigDTO {
        private boolean active;
        private String startTime;
        private String endTime;
    }
}
