package com.meyisoft.dental.system.enums;

/**
 * Define los tipos de notificación que el sistema puede enviar.
 * Cada tipo corresponde a una plantilla HTML en /templates/email/
 * y a un formato de mensaje para WhatsApp.
 */
public enum NotificationType {

    // ─── PACIENTE ───────────────────────────────────────────
    /** Se dispara cuando una cita cambia a estado CONFIRMADA */
    CONFIRMACION_CITA("confirmacion-cita", "Cita Confirmada"),

    /** Se dispara 24 hrs antes de la cita (cron job diario) */
    RECORDATORIO_CITA("recordatorio-cita", "Recordatorio de Cita"),

    /** Se dispara al registrar un pago exitoso en la consulta */
    COMPROBANTE_PAGO("comprobante-pago", "Comprobante de Pago"),

    // ─── DOCTOR ─────────────────────────────────────────────
    /** Se dispara cuando el dueño asigna una cita a un doctor */
    CITA_ASIGNADA_DOCTOR("cita-asignada-doctor", "Nueva Cita Asignada"),

    /** Se dispara como resumen matutino (cron a las 7:00 AM) */
    AGENDA_DIARIA_DOCTOR("agenda-diaria-doctor", "Tu Agenda para Hoy"),

    // ─── OWNER ──────────────────────────────────────────────
    /** Se dispara cuando un paciente solicita una cita (estado POR_CONFIRMAR) */
    NUEVA_CITA_PENDIENTE("nueva-cita-pendiente", "Nueva Solicitud de Cita"),

    /** Se dispara como resumen nocturno (cron a las 9:00 PM) */
    RESUMEN_DIARIO_OWNER("resumen-diario-owner", "Resumen Diario de Ingresos"),

    /** Se dispara cuando se crea un paciente desde el CRM con sus credenciales */
    BIENVENIDA_PACIENTE("bienvenida-paciente", "¡Bienvenido a tu Clínica Dental!"),

    /** Se dispara cuando se crea un integrante del staff desde el CRM */
    BIENVENIDA_STAFF("bienvenida-staff", "Bienvenido al Equipo - Tus Credenciales de Acceso");

    /** Nombre del archivo de plantilla HTML (sin extensión) */
    private final String templateName;

    /** Asunto por defecto para el correo electrónico */
    private final String defaultSubject;

    NotificationType(String templateName, String defaultSubject) {
        this.templateName = templateName;
        this.defaultSubject = defaultSubject;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getDefaultSubject() {
        return defaultSubject;
    }
}
