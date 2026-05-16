package com.meyisoft.dental.system.models.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request exclusivo para autenticación de pacientes.
 * Login híbrido: permite correo o teléfono en un único campo.
 */
@Data
public class PatientLoginRequest {

    /**
     * Campo recomendado: acepta correo o teléfono.
     */
    @Size(max = 100, message = "El identificador no puede exceder los 100 caracteres")
    private String user;

    /**
     * Campo legado (compatibilidad): si llega, se usará como identificador.
     */
    @Size(max = 15, message = "El teléfono no puede exceder los 15 caracteres")
    private String telefono;

    @NotBlank(message = "El NIP es obligatorio")
    @Pattern(regexp = "\\d{6}", message = "El NIP debe ser de exactamente 6 dígitos")
    private String nip;
}
