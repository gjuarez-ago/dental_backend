package com.meyisoft.dental.system.entity;

import com.meyisoft.dental.system.enums.PagoStatus;
import com.meyisoft.dental.system.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Entity
@Table(name = "pagos")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Pago extends BaseEntity {

    @Column(name = "cita_id")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID citaId;

    @Column(name = "doctor_id")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID doctorId;

    @Column(name = "paciente_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID pacienteId;

    @Column(name = "monto", nullable = false)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false)
    private PaymentMethod metodoPago;

    @Column(name = "folio_pago")
    private String folioPago;

    @Column(name = "comprobante_url")
    private String comprobanteUrl;

    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PagoStatus status;

    @Column(name = "motivo_rechazo", columnDefinition = "TEXT")
    private String motivoRechazo;
}
