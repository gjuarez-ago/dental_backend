package com.meyisoft.dental.system.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import com.meyisoft.dental.system.models.dto.MedicamentoDTO;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@Table(name = "consultas_medicas")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ConsultaMedica extends BaseEntity {

    @Column(name = "cita_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID citaId;

    @Column(name = "paciente_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID pacienteId;

    @Column(name = "doctor_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID doctorId;

    @Column(name = "diagnostico", columnDefinition = "TEXT")
    private String diagnostico;

    @Column(name = "cie10_id")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID cie10Id;

    @Column(name = "presion_arterial", length = 20)
    private String presionArterial;

    @Column(name = "frecuencia_cardiaca")
    private Integer frecuenciaCardiaca;

    @Column(name = "frecuencia_respiratoria")
    private Integer frecuenciaRespiratoria;

    @Column(name = "temperatura", precision = 4, scale = 1)
    private BigDecimal temperatura;

    @Column(name = "peso", precision = 5, scale = 2)
    private BigDecimal peso;

    @Column(name = "talla", precision = 5, scale = 2)
    private BigDecimal talla;

    @Column(name = "imc", precision = 4, scale = 2)
    private BigDecimal imc;

    @Column(name = "procedimiento_realizado", columnDefinition = "TEXT")
    private String procedimientoRealizado;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "prescripcion_medica", columnDefinition = "jsonb")
    private List<MedicamentoDTO> prescripcionMedica;

    @Column(name = "indicaciones_post_operatorias", columnDefinition = "TEXT")
    private String indicaciones;

    @Column(name = "observaciones_internas", columnDefinition = "TEXT")
    private String observacionesInternas;

    @Column(name = "complicaciones", columnDefinition = "TEXT")
    private String complicaciones;

    @Column(name = "atencion_inicio")
    private OffsetDateTime atencionInicio;

    @Column(name = "atencion_fin")
    private OffsetDateTime atencionFin;

    @Builder.Default
    @Column(name = "receta_generada")
    private Boolean recetaGenerada = false;
}
