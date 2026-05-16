package com.meyisoft.dental.system.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicioPublicoDTO {
    private UUID id;
    private String nombre;
    private String descripcion;
    private BigDecimal precioBase;
    private int duracionMinutos;
}
