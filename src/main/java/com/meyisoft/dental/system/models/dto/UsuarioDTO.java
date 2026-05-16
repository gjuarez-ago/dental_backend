package com.meyisoft.dental.system.models.dto;

import com.meyisoft.dental.system.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDTO {
    private UUID id;
    private String nombreCompleto;
    private String email;
    private String telefonoContacto;
    private UserRole rol;
    private UUID tenantId;
    private UUID sucursalIdPrincipal;
    private String tenantType;
    private Boolean activo;
    private Boolean esPersonalClinico;
    private Boolean onboardingCompletado;
    private String fotografiaUrl;
    private String biografia;
    private java.time.LocalDate fechaNacimiento;
    private String genero;
    private String[] especialidades;
    private String nombreComercial;
    private String sucursalTelefono;
    private UUID estadoId;
    private UUID municipioId;
}
