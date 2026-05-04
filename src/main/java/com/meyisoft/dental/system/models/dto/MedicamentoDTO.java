package com.meyisoft.dental.system.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicamentoDTO {
    private String nombre;       // Ej: Amoxicilina 500mg
    private String dosis;        // Ej: 1 tableta
    private String frecuencia;   // Ej: Cada 8 horas
    private String duracion;     // Ej: Por 7 días
    private String instrucciones; // Ej: Tomar después de los alimentos
}
