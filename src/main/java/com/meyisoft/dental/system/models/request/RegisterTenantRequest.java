package com.meyisoft.dental.system.models.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterTenantRequest {

    @NotBlank(message = "El nombre comercial es obligatorio")
    @Size(max = 150, message = "El nombre comercial no puede exceder 150 caracteres")
    private String tenantName;

    /** CONSULTORIO | EMPRESA | DOCTOR_INDEPENDIENTE */
    @NotBlank(message = "El tipo de cuenta es obligatorio")
    @Pattern(regexp = "CONSULTORIO|EMPRESA|DOCTOR_INDEPENDIENTE",
             message = "El tipo debe ser CONSULTORIO, EMPRESA o DOCTOR_INDEPENDIENTE")
    private String tenantType;

    /** DENTAL | MEDICINA_GENERAL | PSICOLOGIA | PEDIATRIA | DERMATOLOGIA | NUTRICION | GENERAL ... */
    @NotBlank(message = "El giro es obligatorio")
    @Size(max = 50)
    private String giro;

    @NotBlank(message = "El estado es obligatorio")
    private String estadoId;

    @NotBlank(message = "El municipio es obligatorio")
    private String municipioId;

    @NotBlank(message = "El correo del administrador es obligatorio")
    @Email(message = "El correo debe ser válido")
    @Size(max = 100)
    private String adminEmail;

    @NotBlank(message = "El teléfono del administrador es obligatorio")
    @Size(max = 15)
    private String adminPhone;

    @NotBlank(message = "El nombre del administrador es obligatorio")
    @Size(max = 150)
    private String adminFullName;

    @NotBlank(message = "El NIP es obligatorio")
    @Pattern(regexp = "\\d{6}", message = "El NIP debe ser de exactamente 6 dígitos")
    private String adminNip;

    /** Sucursal única que se crea al registrar (limitada a 1 en el flujo público). */
    @NotBlank(message = "La dirección de la sucursal es obligatoria")
    @Size(max = 500)
    private String sucursalDireccion;

    @Size(max = 20)
    private String sucursalTelefono;
}
