package com.meyisoft.dental.system.service;

import com.meyisoft.dental.system.entity.Paciente;
import com.meyisoft.dental.system.entity.Usuario;
import com.meyisoft.dental.system.enums.NotificationType;
import com.meyisoft.dental.system.repository.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Servicio central de notificaciones del sistema dental.
 * <p>
 * Decide automáticamente el canal de envío (Email o WhatsApp) según las
 * reglas de negocio:
 * <ul>
 * <li>Paciente con emailVerificado=true → Email</li>
 * <li>Paciente con emailVerificado=false → WhatsApp</li>
 * <li>Doctor y Owner → siempre Email (tienen correo registrado)</li>
 * </ul>
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    // Servicio de envío de correos HTML
    private final EmailService emailService;

    // Servicio de envío de mensajes por WhatsApp
    private final WhatsAppService whatsAppService;

    // Repositorio para obtener datos de la empresa (logo, nombre, etc)
    private final EmpresaRepository empresaRepository;

    // ═══════════════════════════════════════════════════════════
    // NOTIFICACIONES AL PACIENTE
    // Regla: solo enviar email si emailVerificado == true
    // Si no, enviar por WhatsApp usando su teléfono
    // ═══════════════════════════════════════════════════════════

    /**
     * Envía una notificación al paciente eligiendo el canal correcto.
     * Si el paciente tiene su email verificado, se envía por correo HTML.
     * Si no, se envía un mensaje de texto plano por WhatsApp.
     *
     * @param paciente        entidad del paciente destinatario
     * @param type            tipo de notificación (determina plantilla y asunto)
     * @param templateVars    variables para la plantilla HTML del correo
     * @param whatsappMessage mensaje de texto plano para WhatsApp (alternativa)
     */
    public void notifyPaciente(Paciente paciente, NotificationType type,
            Map<String, String> templateVars, String whatsappMessage) {

        Map<String, String> finalVars = enrichWithEmpresaData(paciente.getTenantId(), templateVars);

        // Verificar si el paciente tiene su email verificado
        if (Boolean.TRUE.equals(paciente.getEmailVerificado())
                && paciente.getEmail() != null
                && !paciente.getEmail().isBlank()) {

            // Canal: EMAIL → enviar con plantilla HTML
            log.info("📧 Enviando {} por EMAIL a paciente: {}", type.name(), paciente.getNombreCompleto());
            emailService.sendHtmlEmail(
                    paciente.getEmail(),
                    type.getDefaultSubject(),
                    type.getTemplateName(),
                    finalVars);

        } else if (paciente.getTelefono() != null && !paciente.getTelefono().isBlank()) {

            // Canal: WHATSAPP → enviar mensaje de texto plano
            log.info("💬 Enviando {} por WHATSAPP a paciente: {}", type.name(), paciente.getNombreCompleto());
            whatsAppService.sendTextMessage(paciente.getTelefono(), whatsappMessage);

        } else {
            // El paciente no tiene ni email verificado ni teléfono registrado
            log.warn("⚠️ No se pudo notificar al paciente '{}': sin email verificado ni teléfono",
                    paciente.getNombreCompleto());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // NOTIFICACIONES AL DOCTOR
    // Siempre por Email (el doctor es un Usuario del sistema)
    // ═══════════════════════════════════════════════════════════

    /**
     * Envía una notificación por email al doctor.
     * Se utiliza para: cita asignada y agenda diaria.
     *
     * @param doctor       entidad del doctor (Usuario con esPersonalClinico=true)
     * @param type         tipo de notificación
     * @param templateVars variables para reemplazar en la plantilla HTML
     */
    public void notifyDoctor(Usuario doctor, NotificationType type, Map<String, String> templateVars) {

        // Validar que el doctor tenga email registrado
        if (doctor.getEmail() == null || doctor.getEmail().isBlank()) {
            log.warn("⚠️ Doctor '{}' no tiene email registrado. No se envió {}", doctor.getNombreCompleto(),
                    type.name());
            return;
        }

        log.info("📧 Enviando {} por EMAIL al Dr(a). {}", type.name(), doctor.getNombreCompleto());

        Map<String, String> finalVars = enrichWithEmpresaData(doctor.getTenantId(), templateVars);

        emailService.sendHtmlEmail(
                doctor.getEmail(),
                type.getDefaultSubject(),
                type.getTemplateName(),
                finalVars);
    }

    // ═══════════════════════════════════════════════════════════
    // NOTIFICACIONES AL DUEÑO (OWNER)
    // Siempre por Email
    // ═══════════════════════════════════════════════════════════

    /**
     * Envía una notificación por email al dueño de la clínica.
     * Se utiliza para: resumen diario de ingresos.
     *
     * @param owner        entidad del dueño (Usuario con rol=OWNER)
     * @param type         tipo de notificación
     * @param templateVars variables para reemplazar en la plantilla HTML
     */
    public void notifyOwner(Usuario owner, NotificationType type, Map<String, String> templateVars) {

        // Validar que el owner tenga email registrado
        if (owner.getEmail() == null || owner.getEmail().isBlank()) {
            log.warn("⚠️ Owner '{}' no tiene email registrado. No se envió {}", owner.getNombreCompleto(), type.name());
            return;
        }

        log.info("📧 Enviando {} por EMAIL al Owner: {}", type.name(), owner.getNombreCompleto());

        Map<String, String> finalVars = enrichWithEmpresaData(owner.getTenantId(), templateVars);

        emailService.sendHtmlEmail(
                owner.getEmail(),
                type.getDefaultSubject(),
                type.getTemplateName(),
                finalVars);
    }

    /**
     * Enriquecer las variables de la plantilla con los datos globales de la
     * empresa.
     */
    private Map<String, String> enrichWithEmpresaData(java.util.UUID tenantId, Map<String, String> originalVars) {
        Map<String, String> finalVars = new HashMap<>(originalVars);

        String defaultLogo = "https://pub-8c6866b9de504c61a0aa8938f5cdc44c.r2.dev/empresas/logo_blue-removebg-preview.png";
        String defaultWeb = "https://meyisoft.com/#/";

        if (tenantId != null) {
            empresaRepository.findById(tenantId).ifPresent(empresa -> {
                finalVars.put("NOMBRE_CLINICA",
                        empresa.getNombreComercial() != null ? empresa.getNombreComercial() : "Clínica Dental");
                finalVars.put("LOGO_URL",
                        (empresa.getLogoUrl() != null && !empresa.getLogoUrl().trim().isEmpty()) ? empresa.getLogoUrl()
                                : defaultLogo);
                finalVars.put("SITIO_WEB",
                        (empresa.getSitioWeb() != null && !empresa.getSitioWeb().trim().isEmpty())
                                ? empresa.getSitioWeb()
                                : defaultWeb);
                finalVars.put("TELEFONO_CLINICA",
                        (empresa.getTelefonoWhatsApp() != null && !empresa.getTelefonoWhatsApp().trim().isEmpty())
                                ? empresa.getTelefonoWhatsApp()
                                : "");
            });
        } else {
            finalVars.putIfAbsent("NOMBRE_CLINICA", "Clínica Dental");
            finalVars.putIfAbsent("LOGO_URL", defaultLogo);
            finalVars.putIfAbsent("SITIO_WEB", defaultWeb);
            finalVars.putIfAbsent("TELEFONO_CLINICA", "");
        }

        return finalVars;
    }
}
