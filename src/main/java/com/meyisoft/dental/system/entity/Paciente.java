package com.meyisoft.dental.system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "pacientes")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Paciente extends BaseEntity {

    @Column(name = "nombre_completo", nullable = false)
    private String nombreCompleto;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "email")
    private String email;

    @Column(name = "genero")
    private String genero; // MASCULINO, FEMENINO, OTRO

    @Column(name = "curp")
    private String curp;

    @Column(name = "direccion")
    private String direccion;

    @Column(name = "ocupacion")
    private String ocupacion;

    @Column(name = "alergias", columnDefinition = "TEXT")
    private String alergias;

    @Column(name = "enfermedades_cronicas", columnDefinition = "TEXT")
    private String enfermedadesCronicas;

    @Column(name = "medicamentos_actuales", columnDefinition = "TEXT")
    private String medicamentosActuales;

    @Column(name = "emergencia_nombre")
    private String emergenciaNombre;

    @Column(name = "emergencia_telefono")
    private String emergenciaTelefono;

    @Column(name = "tipo_sangre")
    private String tipoSangre;

    @Column(name = "notas_clinicas", columnDefinition = "TEXT")
    private String notasClinicas;

    @Column(name = "antecedentes_heredofamiliares", columnDefinition = "TEXT")
    private String antecedentesHeredofamiliares;

    @Column(name = "antecedentes_no_patologicos", columnDefinition = "TEXT")
    private String antecedentesNoPatologicos;

    @Column(name = "aceptacion_privacidad", nullable = false)
    @Builder.Default
    private Boolean aceptacionPrivacidad = false;

    @Column(name = "fecha_aceptacion_privacidad")
    private OffsetDateTime fechaAceptacionPrivacidad;

    @Column(name = "saldo_pendiente", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal saldoPendiente = BigDecimal.ZERO;

    @Column(name = "expediente_completo", nullable = false)
    @Builder.Default
    private Boolean expedienteCompleto = false;

    @Column(name = "pin_hash")
    private String pinHash;

    @Column(name = "pin_cambiado")
    @Builder.Default
    private Boolean pinCambiado = false;

    @Column(name = "email_verificado")
    @Builder.Default
    private Boolean emailVerificado = false;

    @Column(name = "rfc", length = 13)
    private String rfc;

    @Column(name = "razon_social_fiscal")
    private String razonSocialFiscal;

    @Column(name = "codigo_postal_fiscal", length = 5)
    private String codigoPostalFiscal;

    @Column(name = "regimen_fiscal")
    private String regimenFiscal;
}
