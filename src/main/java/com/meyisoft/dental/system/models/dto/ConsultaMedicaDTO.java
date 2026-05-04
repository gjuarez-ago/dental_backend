package com.meyisoft.dental.system.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaMedicaDTO {
    private UUID id;
    private UUID citaId;
    private UUID pacienteId;
    private UUID doctorId;
    private String doctorNombre;
    private UUID cie10Id;
    private String cie10Nombre; // Para mostrar en el listado sin hacer otro fetch
    private String diagnostico;
    private String presionArterial;
    private Integer frecuenciaCardiaca;
    private Integer frecuenciaRespiratoria;
    private java.math.BigDecimal temperatura;
    private java.math.BigDecimal peso;
    private java.math.BigDecimal talla;
    private java.math.BigDecimal imc;
    private String procedimientoRealizado;
    private java.util.List<MedicamentoDTO> prescripcionMedica;
    private String indicaciones;
    private String observacionesInternas;
    private String complicaciones;
    private Boolean recetaGenerada;
    private OffsetDateTime atencionInicio;
    private OffsetDateTime atencionFin;
    private OffsetDateTime createdAt;

    // Constructor para proyección JPQL optimizada (Evita N+1)
    public ConsultaMedicaDTO(UUID id, UUID citaId, UUID pacienteId, UUID doctorId, String doctorNombre, 
                            UUID cie10Id, String cie10Nombre, String diagnostico, String presionArterial, 
                            Integer frecuenciaCardiaca, Integer frecuenciaRespiratoria, java.math.BigDecimal temperatura, 
                            java.math.BigDecimal peso, java.math.BigDecimal talla, java.math.BigDecimal imc, 
                            String procedimientoRealizado, String indicaciones, String observacionesInternas, 
                            Boolean recetaGenerada, OffsetDateTime atencionInicio, OffsetDateTime atencionFin, 
                            OffsetDateTime createdAt) {
        this.id = id;
        this.citaId = citaId;
        this.pacienteId = pacienteId;
        this.doctorId = doctorId;
        this.doctorNombre = doctorNombre;
        this.cie10Id = cie10Id;
        this.cie10Nombre = cie10Nombre;
        this.diagnostico = diagnostico;
        this.presionArterial = presionArterial;
        this.frecuenciaCardiaca = frecuenciaCardiaca;
        this.frecuenciaRespiratoria = frecuenciaRespiratoria;
        this.temperatura = temperatura;
        this.peso = peso;
        this.talla = talla;
        this.imc = imc;
        this.procedimientoRealizado = procedimientoRealizado;
        this.indicaciones = indicaciones;
        this.observacionesInternas = observacionesInternas;
        this.recetaGenerada = recetaGenerada;
        this.atencionInicio = atencionInicio;
        this.atencionFin = atencionFin;
        this.createdAt = createdAt;
    }
}
