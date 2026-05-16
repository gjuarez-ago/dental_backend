package com.meyisoft.dental.system.models.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class BookingPublicMarketplaceRequest {

    @NotNull
    private UUID tenantId;

    @NotNull
    private UUID servicioId;

    @NotBlank
    private String fecha;        // ISO date: yyyy-MM-dd

    @NotBlank
    private String horaInicio;   // HH:mm

    @NotBlank
    private String nombrePaciente;

    @NotBlank
    private String telefonoPaciente;

    private String emailPaciente;

    private String notas;
}
