package com.meyisoft.dental.system.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.UUID;

/**
 * Catálogo Internacional de Enfermedades (CIE-10).
 * Es un catálogo global compartido por todos los tenants.
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "catalogo_cie10", indexes = {
    @Index(name = "idx_cie10_codigo", columnList = "codigo", unique = true),
    @Index(name = "idx_cie10_nombre", columnList = "nombre")
})
public class CatalogoCie10 {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @Column(nullable = false, length = 10)
    private String codigo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String nombre;

    @Column(length = 100)
    private String categoria;

    @Builder.Default
    private Boolean activo = true;
}
