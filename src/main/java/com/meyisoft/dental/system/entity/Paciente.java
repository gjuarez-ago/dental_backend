package com.meyisoft.dental.system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Entity
@Table(name = "pacientes")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Paciente extends BaseEntity {

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "telefono", length = 15)
    private String telefono;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "genero", length = 20)
    private String genero; // MASCULINO, FEMENINO, OTRO

    @Column(name = "curp", length = 18)
    private String curp;

    @Column(name = "direccion", length = 255)
    private String direccion;

    @Column(name = "estado_id")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID estadoId;

    @Column(name = "ocupacion", length = 100)
    private String ocupacion;

    @Column(name = "alergias", columnDefinition = "TEXT")
    private String alergias;

    @Column(name = "enfermedades_cronicas", columnDefinition = "TEXT")
    private String enfermedadesCronicas;

    @Column(name = "medicamentos_actuales", columnDefinition = "TEXT")
    private String medicamentosActuales;

    @Column(name = "emergencia_nombre", length = 150)
    private String emergenciaNombre;

    @Column(name = "emergencia_telefono", length = 15)
    private String emergenciaTelefono;

    @Column(name = "tipo_sangre", length = 10)
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

    @Column(name = "razon_social_fiscal", length = 255)
    private String razonSocialFiscal;

    @Column(name = "codigo_postal_fiscal", length = 5)
    private String codigoPostalFiscal;

    @Column(name = "regimen_fiscal", length = 100)
    private String regimenFiscal;
}
