package com.meyisoft.dental.system.models.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PatientSetupAccessRequest {

    @NotBlank(message = "El teléfono es requerido")
    private String telefono;

    @NotBlank(message = "El correo es requerido")
    @Email(message = "Debe ser un correo válido")
    private String email;

}
