package com.meyisoft.dental.system.entity;

import com.meyisoft.dental.system.enums.AppointmentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(
        name = "citas",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_citas_tenant_folio", columnNames = {"tenant_id", "folio"})
        }
)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Cita extends BaseEntity {

    @Column(name = "paciente_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID pacienteId;

    @Column(name = "doctor_id")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID doctorId;

    @Column(name = "sucursal_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID sucursalId;

    @Column(name = "servicio_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID servicioId;

    @Column(name = "fecha_hora", nullable = false)
    private OffsetDateTime fechaHora;

    @Column(name = "duracion_minutos")
    private Integer duracionMinutos;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    @Builder.Default
    private AppointmentStatus estado = AppointmentStatus.POR_CONFIRMAR;

    @Column(name = "source", length = 20)
    private String source; // APP, CRM

    @Column(name = "folio", length = 20)
    private String folio;

    @Column(name = "motivo_consulta", columnDefinition = "TEXT")
    private String motivoConsulta;

    @Column(name = "notas_recepcion", columnDefinition = "TEXT")
    private String notasRecepcion;

    @Column(name = "monto_total", precision = 12, scale = 2)
    private java.math.BigDecimal montoTotal;

    @Column(name = "monto_pagado", precision = 12, scale = 2)
    @Builder.Default
    private java.math.BigDecimal montoPagado = java.math.BigDecimal.ZERO;

    @Column(name = "precio_servicio", precision = 12, scale = 2)
    private java.math.BigDecimal precioServicio;

    @Column(name = "motivo_rechazo", columnDefinition = "TEXT")
    private String motivoRechazo;

}
