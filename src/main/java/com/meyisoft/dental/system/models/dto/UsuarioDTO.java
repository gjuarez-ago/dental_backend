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
    private Boolean activo;
    private Boolean esPersonalClinico;
    private String[] especialidades;
}
