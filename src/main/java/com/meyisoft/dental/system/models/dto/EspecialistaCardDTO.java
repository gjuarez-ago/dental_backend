package com.meyisoft.dental.system.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EspecialistaCardDTO {
    private String tenantId;
    private String nombreComercial;
    private String nombreDoctor;
    private String giro;
    private String giroNombre;
    private String estadoNombre;
    private String municipioNombre;
    private String fotografiaUrl;
    private double calificacion;
    private int totalResenas;
    private BigDecimal precioDesde;
    private String modalidad;
    private String proximoSlot;
    private String biografia;
}
