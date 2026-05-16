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
        @UniqueConstraint(name = "uq_usuarios_phone", columnNames = { "telefono_contacto" }),
        @UniqueConstraint(name = "uq_usuarios_email", columnNames = { "email" })
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

    @Column(name = "telefono_contacto", nullable = false, length = 15)
    private String telefonoContacto;

    @Column(name = "email", length = 100)
    private String email;

    @JsonIgnore
    @Column(name = "nip_hash", nullable = false)
    private String nipHash; // NIP encriptado por Spring Security

    @JsonIgnore
    @Column(name = "requiere_cambio_nip")
    @Builder.Default
    private Boolean requiereCambioNip = true;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(name = "cedula_profesional", length = 50)
    private String cedulaProfesional;

    @Column(name = "biografia", columnDefinition = "text")
    private String biografia;

    @Column(name = "fotografia_url", length = 500)
    private String fotografiaUrl;

    @Column(name = "es_personal_clinico")
    @Builder.Default
    private Boolean esPersonalClinico = false;

    @Column(name = "activo")
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "genero", length = 20)
    private String genero; // MASCULINO, FEMENINO, OTRO

    @Column(name = "fecha_nacimiento")
    private java.time.LocalDate fechaNacimiento;

    @Column(name = "especialidades", columnDefinition = "text[]")
    private String[] especialidades;
}
