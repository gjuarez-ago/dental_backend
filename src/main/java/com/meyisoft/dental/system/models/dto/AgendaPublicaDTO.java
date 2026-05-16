package com.meyisoft.dental.system.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgendaPublicaDTO {
    private String tenantId;
    private String nombreComercial;
    private String nombreDoctor;
    private String giroNombre;
    private String fotografiaUrl;
    private String biografia;
    private String modalidad;
    private double calificacion;
    private int totalResenas;
    private int diasAnticipacionReserva;
    private List<ServicioPublicoDTO> servicios;
    private List<DiaAgendaDTO> diasDisponibles;
}
