package com.meyisoft.dental.system.controller;

import com.meyisoft.dental.system.models.response.ApiResponse;
import com.meyisoft.dental.system.service.EmailService;
import com.meyisoft.dental.system.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controlador de pruebas para verificar el envío de notificaciones.
 * Ruta pública: /api/v1/public/test
 */
@RestController
@RequestMapping("/api/v1/public/test")
@RequiredArgsConstructor
public class TestController {

    // Inyectamos EmailService directamente para pruebas rápidas
    private final EmailService emailService;

    /**
     * Endpoint de prueba para enviar un correo HTML con la plantilla de confirmación de cita.
     * Ejemplo: GET /api/v1/public/test/notification?email=test@gmail.com
     */
    @GetMapping("/notification")
    public ApiResponse<String> testNotification(@RequestParam String email) {

        // Variables de prueba para la plantilla de confirmación de cita
        Map<String, String> variables = new java.util.HashMap<>();
        variables.put("NOMBRE_CLINICA", "Clínica Dental");
        variables.put("NOMBRE_PACIENTE", "Paciente de Prueba");
        variables.put("FOLIO_CITA", "CIT-20260425-0001");
        variables.put("FECHA_CITA", "28 de Abril, 2026");
        variables.put("HORA_CITA", "10:30 AM");
        variables.put("SERVICIO", "Limpieza Dental");
        variables.put("NOMBRE_DOCTOR", "Dr. García");
        variables.put("NOMBRE_SUCURSAL", "Sucursal Centro");
        variables.put("HORAS_CANCELACION", "24");
        variables.put("TELEFONO_CLINICA", "(555) 123-4567");
        variables.put("LOGO_URL", "https://pub-8c6866b9de504c61a0aa8938f5cdc44c.r2.dev/empresas/logo_blue-removebg-preview.png");
        variables.put("SITIO_WEB", "https://sarai-rios.meyisoft.com/#/");

        // Enviar el correo HTML usando la plantilla de confirmación
        emailService.sendHtmlEmail(
                email,
                NotificationType.CONFIRMACION_CITA.getDefaultSubject(),
                NotificationType.CONFIRMACION_CITA.getTemplateName(),
                variables
        );

        return ApiResponse.success("Correo de prueba enviado a: " + email);
    }

    /**
     * Endpoint de prueba para enviar un correo HTML al dueño con la notificación de cita pendiente.
     * Ejemplo: GET /api/v1/public/test/notification-owner?email=owner@gmail.com
     */
    @GetMapping("/notification-owner")
    public ApiResponse<String> testNotificationOwner(@RequestParam String email) {

        Map<String, String> variables = new java.util.HashMap<>();
        variables.put("NOMBRE_CLINICA", "Dental Studio");
        variables.put("NOMBRE_OWNER", "Dr. Administrador");
        variables.put("NOMBRE_PACIENTE", "Paciente de Prueba");
        variables.put("FECHA_CITA", "28 de Abril, 2026");
        variables.put("HORA_CITA", "10:30 AM");
        variables.put("SERVICIO", "Limpieza Dental");
        variables.put("NOMBRE_SUCURSAL", "Sucursal Centro");
        variables.put("ORIGEN", "App Móvil");
        variables.put("TELEFONO_CLINICA", "(555) 123-4567");
        variables.put("LOGO_URL", "https://pub-8c6866b9de504c61a0aa8938f5cdc44c.r2.dev/empresas/logo_blue-removebg-preview.png");
        variables.put("SITIO_WEB", "https://sarai-rios.meyisoft.com/#/");

        emailService.sendHtmlEmail(
                email,
                NotificationType.NUEVA_CITA_PENDIENTE.getDefaultSubject(),
                NotificationType.NUEVA_CITA_PENDIENTE.getTemplateName(),
                variables
        );

        return ApiResponse.success("Correo de notificación al dueño enviado a: " + email);
    }
}
