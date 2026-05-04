package com.meyisoft.dental.system.models.request;

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
public class UsuarioRequest {
    private String nombreCompleto;
    private String telefonoContacto;
    private String email;
    private String nip; // NIP de 6 dígitos
    private UserRole rol;
    private String cedulaProfesional;
    private String fotografiaUrl;
    private Boolean esPersonalClinico;
    private UUID sucursalId;
    private String genero;
    private java.util.List<String> especialidades;
}
