package com.meyisoft.dental.system.models.request;

import com.meyisoft.dental.system.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 150, message = "El nombre no puede exceder los 150 caracteres")
    private String nombreCompleto;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 15, message = "El teléfono no puede exceder los 15 caracteres")
    private String telefonoContacto;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Debe ser un email válido")
    @Size(max = 100, message = "El email no puede exceder los 100 caracteres")
    private String email;
    private String nip; // NIP de 6 dígitos
    private UserRole rol;
    @Size(max = 50, message = "La cédula no puede exceder los 50 caracteres")
    private String cedulaProfesional;
    private String fotografiaUrl;
    private Boolean esPersonalClinico;
    private UUID sucursalId;
    private String genero;
    private java.util.List<String> especialidades;
}
