package com.meyisoft.dental.system.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@Table(name = "catalogo_municipios")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogoMunicipio {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "estado_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID estadoId;

    @Column(name = "reg_borrado", nullable = false)
    @Builder.Default
    private Integer regBorrado = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now(java.time.ZoneId.of("America/Mexico_City").getRules().getOffset(java.time.Instant.now()));

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now(java.time.ZoneId.of("America/Mexico_City").getRules().getOffset(java.time.Instant.now()));
}
