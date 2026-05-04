package com.meyisoft.dental.system.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Servicio centralizado de envío de correos electrónicos con soporte HTML.
 * <p>
 * Lee las plantillas HTML desde /resources/templates/email/ y reemplaza
 * las variables dinámicas con la sintaxis {{VARIABLE}}.
 * Todos los envíos son asíncronos para no bloquear el hilo principal.
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    // Inyección del bean de Spring Mail para enviar correos
    private final JavaMailSender mailSender;

    // Dirección de remitente configurada desde application.properties
    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Envía un correo HTML de forma asíncrona usando una plantilla predefinida.
     *
     * @param to           dirección de correo del destinatario
     * @param subject      asunto del correo
     * @param templateName nombre del archivo HTML dentro de templates/email/ (sin
     *                     extensión)
     * @param variables    mapa clave-valor donde la clave es el nombre de la
     *                     variable
     *                     (sin los {{}}) y el valor es el texto de reemplazo.
     *                     Ejemplo: Map.of("NOMBRE_PACIENTE", "Juan Pérez")
     */
    @Async
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, String> variables) {
        try {
            // Cargar la plantilla HTML desde el classpath
            String htmlContent = loadTemplate(templateName);

            // Reemplazar cada variable {{CLAVE}} por su valor correspondiente
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                htmlContent = htmlContent.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }

            // Construir el mensaje MIME con soporte HTML
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom(fromEmail);
            // El segundo parámetro 'true' indica que el contenido es HTML
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("✉️ Email HTML enviado exitosamente a: {} | Plantilla: {}", to, templateName);

        } catch (MessagingException e) {
            // Error al construir o enviar el mensaje MIME
            log.error("❌ Error construyendo email para {}: {}", to, e.getMessage());
        } catch (IOException e) {
            // Error al leer el archivo de plantilla desde el classpath
            log.error("❌ Error leyendo plantilla '{}': {}", templateName, e.getMessage());
        }
    }

    /**
     * Envía un correo de texto plano (sin HTML) de forma asíncrona.
     * Se mantiene por retrocompatibilidad con código existente.
     *
     * @param to      dirección de correo del destinatario
     * @param subject asunto del correo
     * @param body    contenido de texto plano
     */
    @Async
    public void sendSimpleEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom(fromEmail);
            helper.setText(body, false);

            mailSender.send(message);
            log.info("✉️ Email texto plano enviado a: {}", to);
        } catch (MessagingException e) {
            log.error("❌ Error enviando email a {}: {}", to, e.getMessage());
        }
    }

    /**
     * Lee un archivo de plantilla HTML desde el classpath.
     * Busca en: src/main/resources/templates/email/{templateName}.html
     *
     * @param templateName nombre de la plantilla sin extensión
     * @return contenido HTML de la plantilla como String
     * @throws IOException si el archivo no existe o no se puede leer
     */
    private String loadTemplate(String templateName) throws IOException {
        // ClassPathResource busca dentro de src/main/resources automáticamente
        ClassPathResource resource = new ClassPathResource("templates/email/" + templateName + ".html");
        // Leemos todo el contenido del archivo como String UTF-8
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
