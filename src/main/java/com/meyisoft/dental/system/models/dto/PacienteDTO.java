package com.meyisoft.dental.system.models.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteDTO {

    private UUID id;
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede exceder los 150 caracteres")
    private String nombreCompleto;
    private LocalDate fechaNacimiento;
    private UUID estadoId;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 15, message = "El teléfono no puede exceder los 15 caracteres")
    private String telefono;

    @Email(message = "Email inválido")
    @Size(max = 100)
    private String email;

    @Size(max = 20)
    private String genero;

    @Size(max = 18)
    private String curp;

    @Size(max = 255)
    private String direccion;

    @Size(max = 100)
    private String ocupacion;
    private String alergias;
    private String enfermedadesCronicas;
    private String medicamentosActuales;
    @Size(max = 150)
    private String emergenciaNombre;

    @Size(max = 15)
    private String emergenciaTelefono;

    @Size(max = 10)
    private String tipoSangre;

    @Size(max = 2000)
    private String notasClinicas;
    private String antecedentesHeredofamiliares;
    private String antecedentesNoPatologicos;
    private Boolean aceptacionPrivacidad;
    private OffsetDateTime fechaAceptacionPrivacidad;


    // Auditoría
    private java.math.BigDecimal saldoPendiente;
    private Boolean expedienteCompleto;

    // Metadata para el listado inteligente
    private OffsetDateTime proximaCita;
    private OffsetDateTime createdAt;
}
