package com.meyisoft.dental.system.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@Entity
@Table(name = "empresas")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Empresa extends BaseEntity {

    @Column(name = "nombre_comercial", nullable = false)
    private String nombreComercial;

    @Column(name = "plan_suscripcion")
    private String planSuscripcion; // SOLO, CONSULTORIO, RED

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "sitio_web")
    private String sitioWeb;

    @Column(name = "pais")
    private String pais; // México, Colombia, Perú, etc.

    @Column(name = "zona_horaria")
    private String zonaHoraria; // America/Mexico_City, America/Bogota, etc.

    @Column(name = "moneda")
    private String moneda; // MXN, USD, COP, etc.

    @Column(name = "prefijo_telefono")
    private String prefijoTelefono; // +52, +57, +51, etc.

    @Column(name = "telefono_whatsapp")
    private String telefonoWhatsApp;

    @Column(name = "idioma", length = 10)
    private String idioma; // es, en, pt, etc.

    @Column(name = "url_privacidad", length = 255)
    private String urlPrivacidad;

    @Column(name = "url_terminos", length = 255)
    private String urlTerminos;

    @Column(name = "rfc_emisor", length = 13)
    private String rfcEmisor;

    @Column(name = "regimen_fiscal_emisor")
    private String regimenFiscalEmisor;

    // si la empresa puede generar citas sin necesidad de aceptar primero la
    // solicitud de comprobante.
    @Column(name = "comprobante_spei")
    @Builder.Default
    private Boolean comprobanteSpei = true;

    @Column(name = "activo")
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "giro")
    @Builder.Default
    private String giro = "GENERAL"; // DENTAL, PSICOLOGIA, GENERAL, etc.

    @Column(name = "horas_anticipacion_cancelacion")
    @Builder.Default
    private Integer horasAnticipacionCancelacion = 24;

    @Column(name = "dias_anticipacion_reserva")
    @Builder.Default
    private Integer diasAnticipacionReserva = 1;
}
