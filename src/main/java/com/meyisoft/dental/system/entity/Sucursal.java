package com.meyisoft.dental.system.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "sucursales")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Sucursal extends BaseEntity {

    @Column(name = "nombre_sucursal", nullable = false)
    private String nombreSucursal;

    @Column(name = "horarios_laborales", columnDefinition = "TEXT")
    private String horariosLaborales; // Almacena el JSON de la configuración de horarios

    @Column(name = "banco", length = 100)
    private String banco;

    @Column(name = "cuenta_bancaria", length = 30)
    private String cuentaBancaria;

    @Column(name = "clabe_interbancaria", length = 18)
    private String clabeInterbancaria;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "direccion", columnDefinition = "TEXT")
    private String direccion;

    @Column(name = "ventana_cancelacion")
    @Builder.Default
    private Integer ventanaCancelacion = 24; // Tiempo en horas para cambios de último momento

    @Column(name = "capacidad_atencion")
    @Builder.Default
    private Integer capacidadAtencion = 2; // Número de sillones, unidades dentales o cubículos simultáneos físicos
}
