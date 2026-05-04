package com.meyisoft.dental.system.models.response;

import com.meyisoft.dental.system.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {
    private UUID id;
    private String nombreCompleto;
    private String telefonoContacto;
    private String email;
    private UserRole rol;
    private String cedulaProfesional;
    private String fotografiaUrl;
    private Boolean esPersonalClinico;
    private UUID sucursalIdPrincipal;
    private String genero;
    private Boolean requiereCambioNip;
    private Boolean activo;
    private java.util.List<String> especialidades;
    private OffsetDateTime createdAt;
}
