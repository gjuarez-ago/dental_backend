package com.meyisoft.dental.system.models.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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
public class ServicioDentalDTO {
    private UUID id;
    @NotBlank(message = "El nombre del servicio es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres")
    private String nombre;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 500, message = "La descripción no puede exceder los 500 caracteres")
    private String descripcion;
    private BigDecimal precioBase;
    private Integer duracionMinutos;
    private String colorEtiqueta;
    private String imagenUrl;
    private Boolean requiereValoracion;
    private String giro;
    private String especialidadRequerida;
    private Boolean procedimientoQuirurgico;
}
