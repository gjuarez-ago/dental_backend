package com.meyisoft.dental.system.models.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;
import lombok.Data;

@Data
public class OnboardingRequest {
    
    @NotNull(message = "La configuración de la empresa es obligatoria")
    private EmpresaConfigRequest empresa;

    @NotNull(message = "La configuración del perfil es obligatoria")
    private PerfilRequest perfil;
    
    @NotNull(message = "Los horarios son obligatorios")
    private Map<String, DiaHorarioRequest> horarios;
    
    @NotNull(message = "El servicio inicial es obligatorio")
    private ServicioRequest servicio;
    
    @NotNull(message = "Debe confirmar que posee cédula")
    private Boolean cedulaConfirmada;

    private String cedulaProfesional;

    @Data
    public static class EmpresaConfigRequest {
        private String nombreConsultorio;
        private String telefono;
        private String zonaHoraria;
        private Boolean habilitarComprobantes;
        private String banco;
        private String cuentaBancaria;
        private String clabeInterbancaria;
        private String estadoId;
        private String municipioId;
    }

    @Data
    public static class PerfilRequest {
        private String biografia;
        private String fotografiaUrl;
        private String genero;
    }

    @Data
    public static class DiaHorarioRequest {
        private Boolean active;
        private String start;
        private String end;
    }

    @Data
    public static class ServicioRequest {
        private String nombre;
        private Integer duracion;
        private BigDecimal precio;
    }
}
