package com.meyisoft.dental.system.entity;

import com.meyisoft.dental.system.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "usuarios", uniqueConstraints = {
        @UniqueConstraint(name = "uq_usuarios_phone", columnNames = { "telefono_contacto" })
})
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Usuario extends BaseEntity {

    @Column(name = "sucursal_id_principal")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID sucursalIdPrincipal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole rol; // OWNER, AGENT (Según AGENTS.md)

    @Column(name = "telefono_contacto", nullable = false)
    private String telefonoContacto;

    @Column(name = "email")
    private String email;

    @JsonIgnore
    @Column(name = "nip_hash", nullable = false)
    private String nipHash; // NIP encriptado por Spring Security

    @JsonIgnore
    @Column(name = "requiere_cambio_nip")
    @Builder.Default
    private Boolean requiereCambioNip = true;

    @Column(name = "nombre_completo", nullable = false)
    private String nombreCompleto;

    @Column(name = "cedula_profesional")
    private String cedulaProfesional;

    @Column(name = "fotografia_url")
    private String fotografiaUrl;

    @Column(name = "es_personal_clinico")
    @Builder.Default
    private Boolean esPersonalClinico = false;

    @Column(name = "activo")
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "genero")
    private String genero; // MASCULINO, FEMENINO, OTRO

    @Column(name = "especialidades", columnDefinition = "text[]")
    private String[] especialidades;
}
