package com.meyisoft.dental.system.service;

import com.meyisoft.dental.system.entity.Cita;
import com.meyisoft.dental.system.entity.Paciente;
import com.meyisoft.dental.system.entity.Sucursal;
import com.meyisoft.dental.system.entity.Usuario;
import com.meyisoft.dental.system.enums.AppointmentStatus;
import com.meyisoft.dental.system.enums.NotificationType;
import com.meyisoft.dental.system.enums.UserRole;
import com.meyisoft.dental.system.models.dto.DashboardStatsDTO;
import com.meyisoft.dental.system.repository.CitaRepository;
import com.meyisoft.dental.system.repository.CitaCronProjection;
import com.meyisoft.dental.system.repository.PacienteRepository;
import com.meyisoft.dental.system.repository.ServicioDentalRepository;
import com.meyisoft.dental.system.repository.SucursalRepository;
import com.meyisoft.dental.system.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio encargado de ejecutar tareas programadas (Cron Jobs) para el envío de notificaciones.
 * - 07:00 AM: Agenda Diaria al Doctor
 * - 12:00 PM: Recordatorio de Cita al Paciente (para el día siguiente)
 * - 09:00 PM: Resumen Diario de Ingresos al Dueño
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CronNotificationService {

    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final SucursalRepository sucursalRepository;
    private final ServicioDentalRepository servicioRepository;
    private final NotificationService notificationService;
    private final CitaService citaService;

    // Offset de México por defecto para los cálculos de fechas
    private static final ZoneOffset MEXICO_OFFSET = ZoneOffset.ofHours(-6);

    /**
     * 1. AGENDA DIARIA AL DOCTOR
     * Se ejecuta todos los días a las 07:00 AM (Zona horaria de México: America/Mexico_City).
     * Envía a cada doctor un resumen de las citas que tiene programadas para el día de hoy.
     */
    @Scheduled(cron = "0 0 7 * * *", zone = "America/Mexico_City")
    @Transactional(readOnly = true)
    public void enviarAgendaDiariaDoctor() {
        log.info("⏰ Ejecutando CRON (07:00 AM): Agenda Diaria para Doctores");

        LocalDate hoy = LocalDate.now(MEXICO_OFFSET);
        OffsetDateTime inicioDia = hoy.atStartOfDay().atOffset(MEXICO_OFFSET);
        OffsetDateTime finDia = hoy.atTime(LocalTime.MAX).atOffset(MEXICO_OFFSET);

        // 1. Consulta Maestra: Obtener todas las citas de hoy en un solo JOIN masivo
        List<CitaCronProjection> citas = citaRepository.findForCronByRange(inicioDia, finDia, 
                List.of(AppointmentStatus.CONFIRMADA, AppointmentStatus.LLEGADA, AppointmentStatus.EN_CONSULTA));
        
        if (citas.isEmpty()) return;

        // 2. Agrupar en memoria por doctor
        Map<UUID, List<CitaCronProjection>> porDoctor = citas.stream()
                .collect(Collectors.groupingBy(CitaCronProjection::getDoctorId));

        // 3. Procesar cada doctor
        porDoctor.forEach((doctorId, citasDoctor) -> {
            CitaCronProjection primerRegistro = citasDoctor.get(0);
            
            StringBuilder listaCitasHtml = new StringBuilder();
            listaCitasHtml.append("<table role='presentation' width='100%' cellspacing='0' cellpadding='0' border='0' style='border:1px solid #e2e8f0;border-radius:8px;overflow:hidden;'>");
            listaCitasHtml.append("<tr>");
            listaCitasHtml.append("<td style='background-color:#f8fafc;padding:10px 16px;font-size:12px;font-weight:600;color:#64748b;text-transform:uppercase;border-bottom:1px solid #e2e8f0;'>Hora</td>");
            listaCitasHtml.append("<td style='background-color:#f8fafc;padding:10px 16px;font-size:12px;font-weight:600;color:#64748b;text-transform:uppercase;border-bottom:1px solid #e2e8f0;'>Paciente</td>");
            listaCitasHtml.append("<td style='background-color:#f8fafc;padding:10px 16px;font-size:12px;font-weight:600;color:#64748b;text-transform:uppercase;border-bottom:1px solid #e2e8f0;'>Servicio</td>");
            listaCitasHtml.append("</tr>");

            for (CitaCronProjection c : citasDoctor) {
                listaCitasHtml.append("<tr>");
                listaCitasHtml.append("<td style='padding:12px 16px;font-size:14px;color:#1e293b;border-bottom:1px solid #f1f5f9;font-weight:600;'>")
                        .append(c.getFechaHora().toLocalTime().toString()).append("</td>");
                listaCitasHtml.append("<td style='padding:12px 16px;font-size:14px;color:#475569;border-bottom:1px solid #f1f5f9;'>")
                        .append(c.getPacienteNombre()).append("</td>");
                listaCitasHtml.append("<td style='padding:12px 16px;font-size:13px;color:#64748b;border-bottom:1px solid #f1f5f9;'>")
                        .append(c.getServicioNombre() != null ? c.getServicioNombre() : "General").append("</td>");
                listaCitasHtml.append("</tr>");
            }
            listaCitasHtml.append("</table>");

            // Mock de objeto Usuario para la notificación (solo campos necesarios)
            Usuario mockDoctor = Usuario.builder()
                    .id(doctorId)
                    .nombreCompleto(primerRegistro.getDoctorNombre())
                    .email(primerRegistro.getDoctorEmail())
                    .tenantId(citasDoctor.get(0).getDoctorId()) // Asumiendo que el ID del tenant se puede inferir o no es crítico aquí si se usa el email
                    .build();

            Map<String, String> vars = Map.of(
                    "NOMBRE_CLINICA", "Clínica Dental",
                    "NOMBRE_DOCTOR", primerRegistro.getDoctorNombre(),
                    "FECHA_HOY", hoy.toString(),
                    "TOTAL_CITAS", String.valueOf(citasDoctor.size()),
                    "LISTA_CITAS", listaCitasHtml.toString(),
                    "TELEFONO_CLINICA", primerRegistro.getSucursalTelefono() != null ? primerRegistro.getSucursalTelefono() : ""
            );

            notificationService.notifyDoctor(mockDoctor, NotificationType.AGENDA_DIARIA_DOCTOR, vars);
        });
    }

    /**
     * 2. RECORDATORIO DE CITA AL PACIENTE
     * Se ejecuta todos los días a las 12:00 PM (Mediodía en México).
     * Busca las citas de MAÑANA y les envía el recordatorio.
     */
    @Scheduled(cron = "0 0 12 * * *", zone = "America/Mexico_City")
    @Transactional(readOnly = true)
    public void enviarRecordatoriosPaciente() {
        log.info("⏰ Ejecutando CRON (12:00 PM): Recordatorios de Cita para Mañana");

        LocalDate manana = LocalDate.now(MEXICO_OFFSET).plusDays(1);
        OffsetDateTime inicioDia = manana.atStartOfDay().atOffset(MEXICO_OFFSET);
        OffsetDateTime finDia = manana.atTime(LocalTime.MAX).atOffset(MEXICO_OFFSET);

        // 1. Consulta Maestra para mañana
        List<CitaCronProjection> citas = citaRepository.findForCronByRange(inicioDia, finDia, 
                List.of(AppointmentStatus.CONFIRMADA));

        for (CitaCronProjection c : citas) {
            // Mock de objeto Paciente (solo campos necesarios)
            Paciente mockPaciente = Paciente.builder()
                    .id(c.getPacienteId())
                    .nombreCompleto(c.getPacienteNombre())
                    .telefono(c.getPacienteTelefono())
                    .build();

            Map<String, String> vars = Map.of(
                    "NOMBRE_CLINICA", "Clínica Dental",
                    "NOMBRE_PACIENTE", c.getPacienteNombre(),
                    "FOLIO_CITA", c.getFolio(),
                    "FECHA_CITA", c.getFechaHora().toLocalDate().toString(),
                    "HORA_CITA", c.getFechaHora().toLocalTime().toString(),
                    "SERVICIO", c.getServicioNombre() != null ? c.getServicioNombre() : "General",
                    "NOMBRE_DOCTOR", c.getDoctorNombre(),
                    "NOMBRE_SUCURSAL", c.getSucursalNombre(),
                    "TELEFONO_CLINICA", c.getSucursalTelefono() != null ? c.getSucursalTelefono() : ""
            );

            String wppMessage = String.format("⏰ Hola %s, te recordamos tu cita de MAÑANA (%s) a las %s en %s con el Dr(a). %s. ¡Te esperamos!",
                    c.getPacienteNombre(), c.getFechaHora().toLocalDate().toString(),
                    c.getFechaHora().toLocalTime().toString(), c.getSucursalNombre(),
                    c.getDoctorNombre());

            notificationService.notifyPaciente(mockPaciente, NotificationType.RECORDATORIO_CITA, vars, wppMessage);
        }
    }

    /**
     * 3. RESUMEN DIARIO AL DUEÑO
     * Se ejecuta todos los días a las 09:00 PM (Noche en México).
     * Obtiene las estadísticas del día y se las envía al dueño de cada Tenant.
     */
    @Scheduled(cron = "0 0 21 * * *", zone = "America/Mexico_City")
    @Transactional(readOnly = true)
    public void enviarResumenDiarioOwner() {
        log.info("⏰ Ejecutando CRON (09:00 PM): Resumen Diario para Dueños");

        LocalDate hoy = LocalDate.now(MEXICO_OFFSET);

        // Obtener todos los dueños únicos activos (usando consulta filtrada)
        List<Usuario> owners = usuarioRepository.findByRolAndActivoAndRegBorrado(UserRole.OWNER, true, 1);

        for (Usuario owner : owners) {
            try {
                // Obtener las estadísticas para el tenant del dueño (general, sin filtro de sucursal/doctor)
                DashboardStatsDTO stats = citaService.getDashboardSummary(owner.getTenantId(), null, null);

                Sucursal sucursal = sucursalRepository.findById(owner.getSucursalIdPrincipal()).orElse(null);

                // Construir tabla de desglose si hubiera (para este ejemplo simulamos, ya que no lo expone DashboardStatsDTO directo)
                String desgloseHtml = "<tr><td colspan='2' style='padding:12px;text-align:center;font-size:13px;color:#64748b;'>Ver detalles en el sistema</td></tr>";

                Map<String, String> vars = Map.of(
                        "NOMBRE_CLINICA", "Clínica Dental",
                        "NOMBRE_OWNER", owner.getNombreCompleto(),
                        "FECHA_HOY", hoy.toString(),
                        "TOTAL_COBRADO", "$" + (stats.getIngresosHoy() != null ? stats.getIngresosHoy().toString() : "0.00"),
                        "CITAS_ATENDIDAS", String.valueOf(stats.getCitasHoyCount()),
                        "CITAS_CANCELADAS", "0", // Este dato requeriría otra query específica
                        "CITAS_PENDIENTES", String.valueOf(stats.getComprobantesPendientesCount()),
                        "DESGLOSE_PAGOS", desgloseHtml,
                        "SALDO_PENDIENTE_GLOBAL", "$" + (stats.getIngresosPorValidar() != null ? stats.getIngresosPorValidar().toString() : "0.00"),
                        "TELEFONO_CLINICA", sucursal != null ? sucursal.getTelefono() : ""
                );

                notificationService.notifyOwner(owner, NotificationType.RESUMEN_DIARIO_OWNER, vars);

            } catch (Exception e) {
                log.error("Error al enviar resumen diario al Owner {}: {}", owner.getId(), e.getMessage());
            }
        }
    }
}
