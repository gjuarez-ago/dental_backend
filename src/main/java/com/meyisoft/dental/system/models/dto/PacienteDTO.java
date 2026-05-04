package com.meyisoft.dental.system.models.dto;

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
    private String nombreCompleto;
    private LocalDate fechaNacimiento;
    private String telefono;
    private String email;
    private String genero;
    private String curp;
    private String direccion;
    private String ocupacion;
    private String alergias;
    private String enfermedadesCronicas;
    private String medicamentosActuales;
    private String emergenciaNombre;
    private String emergenciaTelefono;
    private String tipoSangre;
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
