package com.meyisoft.dental.system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "servicios_dentales")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ServicioDental extends BaseEntity {

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "precio_base", nullable = false)
    private BigDecimal precioBase;

    @Column(name = "duracion_minutos")
    private Integer duracionMinutos;

    @Column(name = "color_etiqueta")
    private String colorEtiqueta;

    @Column(name = "imagen_url", columnDefinition = "TEXT")
    private String imagenUrl;

    @Column(name = "requiere_valoracion")
    private Boolean requiereValoracion; // true = valoración, false = cita directa

    @Column(name = "giro")
    @Builder.Default
    private String giro = "DENTAL"; // Clasificación general (Dental, Psicología, etc.)

    @Column(name = "especialidad_requerida")
    private String especialidadRequerida; // El "match" para filtrar doctores (ej: Ortodoncia)

    @Column(name = "procedimiento_quirurgico")
    @Builder.Default
    private Boolean procedimientoQuirurgico = false; // true = requiere consentimiento informado para cirugía
}
