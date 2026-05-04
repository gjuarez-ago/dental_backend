package com.meyisoft.dental.system.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meyisoft.dental.system.entity.Cita;
import com.meyisoft.dental.system.entity.Sucursal;
import com.meyisoft.dental.system.enums.AppointmentStatus;
import com.meyisoft.dental.system.exception.BusinessException;
import com.meyisoft.dental.system.config.AuditAction;
import com.meyisoft.dental.system.models.dto.CitaDTO;
import com.meyisoft.dental.system.repository.CitaRepository;
import com.meyisoft.dental.system.repository.CitaSummaryProjection;
import com.meyisoft.dental.system.repository.RegistroFolioRepository;
import com.meyisoft.dental.system.repository.SucursalRepository;
import com.meyisoft.dental.system.repository.UsuarioRepository;
import com.meyisoft.dental.system.repository.PacienteRepository;
import com.meyisoft.dental.system.repository.PagoRepository;
import com.meyisoft.dental.system.repository.ServicioDentalRepository;
import com.meyisoft.dental.system.repository.EmpresaRepository;
import com.meyisoft.dental.system.repository.DashboardPagoStats;
import com.meyisoft.dental.system.repository.DashboardCitaStats;
import com.meyisoft.dental.system.repository.DashboardPacienteStats;
import com.meyisoft.dental.system.enums.UserRole;
import com.meyisoft.dental.system.entity.Usuario;
import com.meyisoft.dental.system.entity.Empresa;
import com.meyisoft.dental.system.entity.Paciente;
import com.meyisoft.dental.system.entity.Pago;
import com.meyisoft.dental.system.entity.RegistroFolio;
import com.meyisoft.dental.system.enums.PaymentMethod;
import com.meyisoft.dental.system.enums.TicketStatus;
import com.meyisoft.dental.system.enums.PagoStatus;
import com.meyisoft.dental.system.models.dto.DashboardStatsDTO;
import com.meyisoft.dental.system.models.dto.IncomeDetailDTO;
import com.meyisoft.dental.system.models.dto.DisponibilidadDiaDTO;
import com.meyisoft.dental.system.models.dto.SlotDisponibilidadDTO;
import com.meyisoft.dental.system.entity.ServicioDental;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CitaService {

    private final CitaRepository repository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioRepository usuarioRepository;
    private final RegistroFolioRepository folioRepository;
    private final PacienteRepository pacienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final PagoRepository pagoRepository;
    private final ServicioDentalRepository servicioDentalRepository;
    private final EmpresaRepository empresaRepository;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @Data
    public static class DayConfig {
        @JsonProperty("active")
        private boolean active;

        @JsonProperty("enabled")
        public void setEnabled(boolean enabled) {
            this.active = enabled;
        }

        @JsonProperty("startTime")
        private String startTime;

        @JsonProperty("start")
        public void setStart(String start) {
            this.startTime = start;
        }

        @JsonProperty("endTime")
        private String endTime;

        @JsonProperty("end")
        public void setEnd(String end) {
            this.endTime = end;
        }
    }

    @Transactional(readOnly = true)
    public List<CitaDTO> listarPorRango(UUID tenantId, UUID sucursalId, OffsetDateTime start, OffsetDateTime end,
            UUID doctorId) {
        List<CitaSummaryProjection> results = repository.findByRangeOptimized(tenantId, sucursalId, start, end, 1, doctorId);
        if (results.isEmpty())
            return new ArrayList<>();

        // Optimización: Cargar todos los pagos de estas citas en una sola consulta
        Set<UUID> citaIds = results.stream().map(CitaSummaryProjection::getId).collect(Collectors.toSet());
        Map<UUID, List<Pago>> pagosMap = pagoRepository.findAllByCitaIdInAndRegBorrado(citaIds, 1).stream()
                .collect(Collectors.groupingBy(Pago::getCitaId));

        return results.stream()
                .map(r -> mapFromProjection(r, pagosMap))
                .sorted((c1, c2) -> {
                    int p1 = getStatusPriority(c1.getEstado());
                    int p2 = getStatusPriority(c2.getEstado());
                    if (p1 != p2)
                        return Integer.compare(p1, p2);
                    return c1.getFechaHora().compareTo(c2.getFechaHora());
                })
                .collect(Collectors.toList());
    }

    private CitaDTO mapFromProjection(CitaSummaryProjection p, Map<UUID, List<Pago>> pagosMap) {
        UUID id = p.getId();
        
        CitaDTO dto = CitaDTO.builder()
                .id(id)
                .pacienteId(p.getPacienteId())
                .pacienteNombre(p.getPacienteNombre())
                .pacienteTelefono(p.getPacienteTelefono())
                .doctorId(p.getDoctorId())
                .doctorNombre(p.getDoctorNombre())
                .sucursalId(p.getSucursalId())
                .servicioId(p.getServicioId())
                .servicioNombre(p.getServicioNombre())
                .fechaHora(p.getFechaHora())
                .duracionMinutos(p.getDuracionMinutos())
                .estado(p.getEstado())
                .motivoConsulta(p.getMotivoConsulta())
                .notasRecepcion(p.getNotasRecepcion())
                .source(p.getSource())
                .folio(p.getFolio())
                .montoTotal(p.getMontoTotal())
                .precioServicio(p.getPrecioServicio())
                .motivoRechazo(p.getMotivoRechazo())
                .procedimientoQuirurgico(p.getProcedimientoQuirurgico())
                .createdAt(p.getCreatedAt())
                .build();

        // Calcular montos reales desde los pagos cargados por lote
        List<Pago> pagos = pagosMap != null ? pagosMap.get(id) : null;
        BigDecimal totalPagadoReal = BigDecimal.ZERO;
        boolean tienePendienteRevision = false;
        String comprobanteUrl = null;

        if (pagos != null) {
            totalPagadoReal = pagos.stream()
                .filter(pago -> pago.getStatus() == PagoStatus.APROBADO || pago.getStatus() == PagoStatus.PENDIENTE_REVISION)
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            tienePendienteRevision = pagos.stream().anyMatch(pago -> pago.getStatus() == PagoStatus.PENDIENTE_REVISION);
            comprobanteUrl = pagos.stream().filter(pago -> pago.getComprobanteUrl() != null).map(Pago::getComprobanteUrl).findFirst().orElse(null);
        }

        dto.setMontoPagado(totalPagadoReal);
        dto.setComprobanteUrl(comprobanteUrl);

        // Calcular estado del ticket con lógica completa
        calculateTicketStatus(dto, tienePendienteRevision);
        
        return dto;
    }

    private void calculateTicketStatus(CitaDTO dto, boolean tienePendienteRevision) {
        if (dto.getMontoTotal() == null) {
            dto.setTicketStatus(TicketStatus.POR_DEFINIR);
            return;
        }

        if (tienePendienteRevision) {
            dto.setTicketStatus(TicketStatus.EN_REVISION);
            return;
        }

        BigDecimal total = dto.getMontoTotal();
        BigDecimal pagado = dto.getMontoPagado() != null ? dto.getMontoPagado() : BigDecimal.ZERO;

        if (total.compareTo(BigDecimal.ZERO) == 0) {
            dto.setTicketStatus(TicketStatus.CORTESIA);
        } else if (pagado.compareTo(BigDecimal.ZERO) == 0) {
            dto.setTicketStatus(TicketStatus.PENDIENTE);
        } else if (pagado.compareTo(total) >= 0) {
            dto.setTicketStatus(TicketStatus.LIQUIDADO);
        } else {
            dto.setTicketStatus(TicketStatus.ABONADO);
        }
    }

    private int getStatusPriority(AppointmentStatus status) {
        if (status == null) return 99;
        return switch (status) {
            case EN_CONSULTA -> 1;
            case LLEGADA -> 2;
            case CONFIRMADA -> 3;
            case POR_LIQUIDAR -> 4;
            case POR_CONFIRMAR -> 5;
            case FINALIZADA -> 6;
            case AUSENTE -> 7;
            case CANCELADA -> 8;
            default -> 99;
        };
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarDoctores(UUID tenantId, UUID servicioId) {
        // Obtener el plan de la empresa
        String plan = empresaRepository.findById(tenantId)
                .map(Empresa::getPlanSuscripcion)
                .orElse("SOLO");

        // Obtener la especialidad requerida si se envió servicioId
        String especialidadReq = null;
        if (servicioId != null) {
            especialidadReq = servicioDentalRepository.findById(servicioId)
                    .map(ServicioDental::getEspecialidadRequerida)
                    .orElse(null);
        }

        final String finalEspecialidadReq = especialidadReq;

        // Buscamos todos los usuarios activos que sean personal clínico (incluye OWNER
        // y DOCTOR)
        return usuarioRepository.findByTenantIdAndRegBorrado(tenantId, 1)
                .stream()
                .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .filter(u -> Boolean.TRUE.equals(u.getEsPersonalClinico()))
                .filter(d -> {
                    // En el plan SOLO, ignoramos el filtro técnico
                    if ("SOLO".equals(plan))
                        return true;

                    // Si no hay especialidad requerida, todos califican
                    if (finalEspecialidadReq == null || finalEspecialidadReq.isBlank())
                        return true;

                    // Si hay especialidad requerida, validamos que el doctor la tenga
                    if (d.getEspecialidades() == null || d.getEspecialidades().length == 0)
                        return false;
                    return java.util.Arrays.asList(d.getEspecialidades()).contains(finalEspecialidadReq);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    @AuditAction(modulo = "CITAS", accion = "AGENDAR", descripcion = "Agendamiento de nueva cita médica")
    public CitaDTO agendar(CitaDTO dto, UUID tenantId, String comprobanteUrl) {
        // 1. Obtener información del servicio
        ServicioDental servicio = null;
        if (dto.getServicioId() != null) {
            servicio = servicioDentalRepository.findByIdAndTenantIdAndRegBorrado(dto.getServicioId(), tenantId, 1)
                    .orElseThrow(
                            () -> new BusinessException("NOT_FOUND", "Servicio no encontrado", HttpStatus.NOT_FOUND));

            if (dto.getDuracionMinutos() == null) {
                dto.setDuracionMinutos(servicio.getDuracionMinutos());
            }
        }

        if (dto.getDuracionMinutos() == null)
            dto.setDuracionMinutos(30);

        // 2. Validar horario de sucursal
        validarHorarioSucursal(dto.getSucursalId(), dto.getFechaHora(), dto.getDuracionMinutos());

        // 3. Validar disponibilidad
        OffsetDateTime finCita = dto.getFechaHora().plusMinutes(dto.getDuracionMinutos());

        // Validar consultorio
        if (repository.countOverlappingSucursal(tenantId, dto.getSucursalId(), dto.getFechaHora(), finCita) > 0) {
            throw new BusinessException("CHOQUE_CITAS", "El consultorio ya está ocupado en este horario",
                    HttpStatus.CONFLICT);
        }

        // Validar doctor si está asignado
        if (dto.getDoctorId() != null) {
            Usuario doctor = usuarioRepository.findById(dto.getDoctorId())
                    .filter(u -> u.getRegBorrado() == 1)
                    .orElseThrow(() -> new BusinessException("DOCTOR_NO_ENCONTRADO", "El doctor seleccionado no existe",
                            HttpStatus.NOT_FOUND));

            if (Boolean.FALSE.equals(doctor.getActivo())) {
                throw new BusinessException("DOCTOR_INACTIVO",
                        "PERSONAL NO DISPONIBLE: No se puede asignar la cita a este doctor(a) porque su perfil está DESACTIVADO. Un usuario desactivado no puede recibir nuevos pacientes ni aparecer en la lista de atención. Para usar este perfil en la agenda, primero debe ser reactivado desde el panel de Usuarios.",
                        HttpStatus.BAD_REQUEST);
            }

            if (repository.countOverlapping(tenantId, dto.getDoctorId(), dto.getFechaHora(), finCita) > 0) {
                throw new BusinessException("CHOQUE_CITAS", "El doctor ya tiene una cita agendada en este horario",
                        HttpStatus.CONFLICT);
            }
        }

        // 4. Manejo de Paciente
        String source = (dto.getSource() != null) ? dto.getSource().toUpperCase() : "CRM";
        UUID pacienteId = dto.getPacienteId();

        if (pacienteId == null && (source.equals("APP") || source.equals("PUBLIC"))) {
            if (dto.getPacienteNombre() == null || dto.getPacienteNombre().trim().isEmpty()) {
                throw new BusinessException("MISSING_DATA", "El nombre del paciente es obligatorio",
                        HttpStatus.BAD_REQUEST);
            }

            validarTelefono(dto.getPacienteTelefono());

            Optional<Paciente> pacienteExistente = pacienteRepository.findByTelefonoAndTenantIdAndRegBorrado(
                    dto.getPacienteTelefono(), tenantId, 1);

            if (pacienteExistente.isPresent()) {
                pacienteId = pacienteExistente.get().getId();
            } else {
                Paciente nuevoPaciente = Paciente.builder()
                        .id(UUID.randomUUID())
                        .nombreCompleto(dto.getPacienteNombre())
                        .telefono(dto.getPacienteTelefono())
                        .expedienteCompleto(false)
                        .pinHash(passwordEncoder.encode("123456"))
                        .pinCambiado(false)
                        .build();
                nuevoPaciente.setTenantId(tenantId);
                nuevoPaciente.setRegBorrado(1);
                pacienteId = pacienteRepository.save(nuevoPaciente).getId();
            }
        } else if (pacienteId == null) {
            throw new BusinessException("MISSING_DATA", "El paciente es obligatorio", HttpStatus.BAD_REQUEST);
        }

        // 5. Traslape Paciente e Historial Diario
        // 5.1 Traslape horario (No chocar con otra cita)
        if (repository.countOverlappingPaciente(tenantId, pacienteId, dto.getFechaHora(), finCita) > 0) {
            throw new BusinessException("CHOQUE_CITAS_PACIENTE",
                    "El paciente ya tiene una cita agendada en este horario", HttpStatus.CONFLICT);
        }

        // 5.2 Límite de una cita por día (Regla de negocio: no saturar espacios)
        OffsetDateTime inicioDia = dto.getFechaHora().toLocalDate().atStartOfDay()
                .atOffset(dto.getFechaHora().getOffset());
        OffsetDateTime finDia = inicioDia.plusDays(1);

        if (repository.countActiveByPacienteAndDate(tenantId, pacienteId, inicioDia, finDia) > 0) {
            throw new BusinessException("LIMITE_CITA_DIARIA",
                    "Lo sentimos, solo se permite una cita por día. Si necesitas atención adicional urgente, por favor comunícate directamente con la clínica.",
                    HttpStatus.BAD_REQUEST);
        }


        AppointmentStatus estadoInicial = (source.equals("APP") || source.equals("PUBLIC"))
                ? AppointmentStatus.POR_CONFIRMAR
                : AppointmentStatus.CONFIRMADA;

        String folio = generarSiguienteFolio(tenantId, "CITA");

        BigDecimal costoFinal = dto.getMontoTotal() != null ? dto.getMontoTotal()
                : (servicio != null ? servicio.getPrecioBase() : BigDecimal.ZERO);

        // Validación de Transparencia: No permitir costo 0 si el servicio tiene valor
        if (servicio != null && servicio.getPrecioBase().compareTo(BigDecimal.ZERO) > 0 && costoFinal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_COST", 
                "El costo de la cita no puede ser $0.00. Para servicios de cortesía o garantía, mantén el precio real y regístralo como Cortesía en el momento del cobro para que aparezca en tus reportes de inversión.", 
                HttpStatus.BAD_REQUEST);
        }

        Cita entity = Cita.builder()
                .id(UUID.randomUUID())
                .pacienteId(pacienteId)
                .doctorId(dto.getDoctorId())
                .sucursalId(dto.getSucursalId())
                .servicioId(dto.getServicioId())
                .fechaHora(dto.getFechaHora())
                .duracionMinutos(dto.getDuracionMinutos())
                .estado(estadoInicial)
                .source(source)
                .folio(folio)
                .motivoConsulta(dto.getMotivoConsulta())
                .notasRecepcion(dto.getNotasRecepcion())
                .precioServicio(servicio != null ? servicio.getPrecioBase() : BigDecimal.ZERO)
                .montoTotal(costoFinal)
                .montoPagado(BigDecimal.ZERO)
                .build();

        entity.setTenantId(tenantId);
        entity.setRegBorrado(1);
        Cita saved = repository.save(entity);

        // Registrar anticipo si aplica
        if ((source.equals("APP") || source.equals("PUBLIC")) && dto.getMontoPagado() != null) {
            Pago anticipo = Pago.builder()
                    .id(UUID.randomUUID())
                    .citaId(saved.getId())
                    .pacienteId(pacienteId)
                    .monto(dto.getMontoPagado())
                    .metodoPago(PaymentMethod.TRANSFERENCIA)
                    .status(PagoStatus.PENDIENTE_REVISION)
                    .comprobanteUrl(comprobanteUrl)
                    .folioPago(dto.getReferenciaPago())
                    .notas("Anticipo desde " + source)
                    .build();
            anticipo.setTenantId(tenantId);
            anticipo.setRegBorrado(1);
            pagoRepository.save(anticipo);
        }

        // Notificar al dueño si se requiere confirmación
        if (estadoInicial == AppointmentStatus.POR_CONFIRMAR) {
            notificarNuevaCitaPendienteOwner(saved, dto);
        } else if (estadoInicial == AppointmentStatus.CONFIRMADA) {
            // Si la cita nace confirmada (agendada desde CRM), cargamos la deuda al paciente
            Paciente paciente = pacienteRepository.findById(pacienteId).orElseThrow();
            if (paciente.getSaldoPendiente() == null) paciente.setSaldoPendiente(BigDecimal.ZERO);
            paciente.setSaldoPendiente(paciente.getSaldoPendiente().add(costoFinal));
            pacienteRepository.save(paciente);
        }

        return mapToDTO(saved);
    }

    @Transactional
    @AuditAction(modulo = "CITAS", accion = "REPROGRAMAR", descripcion = "Cambio de fecha/hora de cita existente")
    public CitaDTO reprogramar(UUID id, OffsetDateTime nuevaFechaHora, Integer nuevaDuracion, BigDecimal montoTotal,
            UUID tenantId) {
        Cita entity = repository.findById(id)
                .filter(c -> c.getTenantId().equals(tenantId) && c.getRegBorrado() == 1)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Cita no encontrada", HttpStatus.NOT_FOUND));

        Integer duracion = (nuevaDuracion != null) ? nuevaDuracion : entity.getDuracionMinutos();
        OffsetDateTime finCita = nuevaFechaHora.plusMinutes(duracion);

        if (repository.countOverlappingSucursalExcludingId(tenantId, entity.getSucursalId(), nuevaFechaHora, finCita,
                id) > 0) {
            throw new BusinessException("CHOQUE_CITAS", "El consultorio ya está ocupado", HttpStatus.CONFLICT);
        }

        if (entity.getDoctorId() != null && repository.countOverlappingExcludingId(tenantId, entity.getDoctorId(),
                nuevaFechaHora, finCita, id) > 0) {
            throw new BusinessException("CHOQUE_CITAS", "El doctor ya está ocupado", HttpStatus.CONFLICT);
        }

        validarHorarioSucursal(entity.getSucursalId(), nuevaFechaHora, duracion);

        entity.setFechaHora(nuevaFechaHora);
        entity.setDuracionMinutos(duracion);

        if (montoTotal != null) {
            // Validación de Transparencia: No permitir costo 0 en edición si el servicio tiene valor
            BigDecimal precioBase = entity.getPrecioServicio() != null ? entity.getPrecioServicio() : BigDecimal.ZERO;
            if (precioBase.compareTo(BigDecimal.ZERO) > 0 && montoTotal.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("INVALID_COST", 
                    "No se permite cambiar el costo a $0.00. Mantén el precio real; si es una garantía o cortesía, regístralo como tal al momento del cobro.", 
                    HttpStatus.BAD_REQUEST);
            }
            entity.setMontoTotal(montoTotal);
        }

        entity.setEstado(AppointmentStatus.CONFIRMADA);

        return mapToDTO(repository.save(entity));
    }

    @Transactional
    @AuditAction(modulo = "CITAS", accion = "CONFIRMAR", descripcion = "Confirmación de cita por personal clínico")
    public CitaDTO confirmarCita(UUID id, UUID doctorId, BigDecimal montoTotal, UUID tenantId) {
        Cita entity = repository.findById(id)
                .filter(c -> c.getTenantId().equals(tenantId) && c.getRegBorrado() == 1)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Cita no encontrada", HttpStatus.NOT_FOUND));

        if (entity.getEstado() != AppointmentStatus.POR_CONFIRMAR) {
            throw new BusinessException("INVALID_STATE", "La cita ya no está pendiente de confirmar",
                    HttpStatus.BAD_REQUEST);
        }

        OffsetDateTime start = entity.getFechaHora();
        OffsetDateTime end = start.plusMinutes(entity.getDuracionMinutos());

        if (repository.countOverlappingExcludingId(tenantId, doctorId, start, end, id) > 0) {
            throw new BusinessException("CHOQUE_CITAS", "El doctor ya tiene otra cita en este horario",
                    HttpStatus.CONFLICT);
        }

        if (montoTotal != null)
            entity.setMontoTotal(montoTotal);
        entity.setDoctorId(doctorId);
        entity.setEstado(AppointmentStatus.CONFIRMADA);
        Cita saved = repository.save(entity);

        // Actualizar saldo del paciente: Cargar deuda y aplicar pagos
        Paciente paciente = pacienteRepository.findById(entity.getPacienteId()).orElseThrow();
        if (paciente.getSaldoPendiente() == null)
            paciente.setSaldoPendiente(BigDecimal.ZERO);

        // 1. Cargar el costo total de la cita a la deuda global
        paciente.setSaldoPendiente(paciente.getSaldoPendiente().add(entity.getMontoTotal()));

        // 2. Aprobar pagos pendientes asociados y restarlos de la deuda
        List<Pago> pagosCita = pagoRepository.findByCitaIdAndRegBorrado(id, 1);
        for (Pago p : pagosCita) {
            if (p.getStatus() == PagoStatus.PENDIENTE_REVISION) {
                p.setStatus(PagoStatus.APROBADO);
                paciente.setSaldoPendiente(paciente.getSaldoPendiente().subtract(p.getMonto()));
                pagoRepository.save(p);
            }
        }
        pacienteRepository.save(paciente);

        // Notificar al paciente la confirmación de su cita
        notificarConfirmacionCitaPaciente(saved);

        // Notificar al doctor que se le ha asignado una cita
        notificarCitaAsignadaDoctor(saved);

        return mapToDTO(saved);
    }

    private void notificarNuevaCitaPendienteOwner(Cita cita, CitaDTO dto) {
        try {
            List<Usuario> owners = usuarioRepository.findByTenantIdAndRolAndRegBorrado(cita.getTenantId(),
                    UserRole.OWNER, 1);
            if (owners.isEmpty())
                return;

            Empresa empresa = empresaRepository.findById(cita.getTenantId()).orElse(null);
            Usuario owner = owners.get(0);
            Sucursal sucursal = sucursalRepository.findById(cita.getSucursalId()).orElse(null);
            ServicioDental servicio = (cita.getServicioId() != null)
                    ? servicioDentalRepository.findById(cita.getServicioId()).orElse(null)
                    : null;

            Map<String, String> vars = Map.of(
                    "NOMBRE_CLINICA", empresa != null ? empresa.getNombreComercial() : "Clínica Dental",
                    "NOMBRE_OWNER", owner.getNombreCompleto(),
                    "NOMBRE_PACIENTE", dto.getPacienteNombre() != null ? dto.getPacienteNombre() : "Paciente",
                    "FECHA_CITA", cita.getFechaHora().toLocalDate().toString(),
                    "HORA_CITA", cita.getFechaHora().toLocalTime().toString(),
                    "SERVICIO", servicio != null ? servicio.getNombre() : "Consulta General",
                    "NOMBRE_SUCURSAL", sucursal != null ? sucursal.getNombreSucursal() : "Sucursal Principal",
                    "ORIGEN", cita.getSource(),
                    "TELEFONO_CLINICA", sucursal != null ? sucursal.getTelefono() : "");

            notificationService.notifyOwner(owner,
                    com.meyisoft.dental.system.enums.NotificationType.NUEVA_CITA_PENDIENTE, vars);
        } catch (Exception e) {
            log.error("Error al enviar notificación al Owner sobre cita pendiente: {}", e.getMessage());
        }
    }

    private void notificarConfirmacionCitaPaciente(Cita cita) {
        try {
            Paciente paciente = pacienteRepository.findById(cita.getPacienteId()).orElse(null);
            if (paciente == null)
                return;

            Empresa empresa = empresaRepository.findById(cita.getTenantId()).orElse(null);
            Sucursal sucursal = sucursalRepository.findById(cita.getSucursalId()).orElse(null);
            ServicioDental servicio = (cita.getServicioId() != null)
                    ? servicioDentalRepository.findById(cita.getServicioId()).orElse(null)
                    : null;
            Usuario doctor = (cita.getDoctorId() != null) ? usuarioRepository.findById(cita.getDoctorId()).orElse(null)
                    : null;

            Map<String, String> vars = Map.of(
                    "NOMBRE_CLINICA", empresa != null ? empresa.getNombreComercial() : "Clínica Dental",
                    "NOMBRE_PACIENTE", paciente.getNombreCompleto(),
                    "FOLIO_CITA", cita.getFolio(),
                    "FECHA_CITA", cita.getFechaHora().toLocalDate().toString(),
                    "HORA_CITA", cita.getFechaHora().toLocalTime().toString(),
                    "SERVICIO", servicio != null ? servicio.getNombre() : "Consulta General",
                    "NOMBRE_DOCTOR", doctor != null ? doctor.getNombreCompleto() : "Por asignar",
                    "NOMBRE_SUCURSAL", sucursal != null ? sucursal.getNombreSucursal() : "Sucursal Principal",
                    "TELEFONO_CLINICA", sucursal != null ? sucursal.getTelefono() : "");

            String wppMessage = String.format(
                    "🦷 Hola %s, tu cita (Folio: %s) ha sido CONFIRMADA para el %s a las %s en %s.",
                    paciente.getNombreCompleto(), cita.getFolio(), cita.getFechaHora().toLocalDate().toString(),
                    cita.getFechaHora().toLocalTime().toString(),
                    sucursal != null ? sucursal.getNombreSucursal() : "nuestra clínica");

            notificationService.notifyPaciente(paciente,
                    com.meyisoft.dental.system.enums.NotificationType.CONFIRMACION_CITA, vars, wppMessage);
        } catch (Exception e) {
            log.error("Error al enviar notificación de confirmación al paciente: {}", e.getMessage());
        }
    }

    private void notificarCitaAsignadaDoctor(Cita cita) {
        try {
            if (cita.getDoctorId() == null)
                return;
            Usuario doctor = usuarioRepository.findById(cita.getDoctorId()).orElse(null);
            Paciente paciente = pacienteRepository.findById(cita.getPacienteId()).orElse(null);
            if (doctor == null || paciente == null)
                return;

            Empresa empresa = empresaRepository.findById(cita.getTenantId()).orElse(null);
            Sucursal sucursal = sucursalRepository.findById(cita.getSucursalId()).orElse(null);
            ServicioDental servicio = (cita.getServicioId() != null)
                    ? servicioDentalRepository.findById(cita.getServicioId()).orElse(null)
                    : null;

            Map<String, String> vars = Map.of(
                    "NOMBRE_CLINICA", empresa != null ? empresa.getNombreComercial() : "Clínica Dental",
                    "NOMBRE_DOCTOR", doctor.getNombreCompleto(),
                    "NOMBRE_PACIENTE", paciente.getNombreCompleto(),
                    "FECHA_CITA", cita.getFechaHora().toLocalDate().toString(),
                    "HORA_CITA", cita.getFechaHora().toLocalTime().toString(),
                    "SERVICIO", servicio != null ? servicio.getNombre() : "Consulta General",
                    "MOTIVO_CONSULTA", cita.getMotivoConsulta() != null ? cita.getMotivoConsulta() : "No especificado",
                    "TELEFONO_CLINICA", sucursal != null ? sucursal.getTelefono() : "");

            notificationService.notifyDoctor(doctor,
                    com.meyisoft.dental.system.enums.NotificationType.CITA_ASIGNADA_DOCTOR, vars);
        } catch (Exception e) {
            log.error("Error al enviar notificación de cita asignada al doctor: {}", e.getMessage());
        }
    }

    @Transactional
    public CitaDTO rechazarCita(UUID id, String motivo, UUID tenantId) {
        Cita entity = repository.findById(id)
                .filter(c -> c.getTenantId().equals(tenantId) && c.getRegBorrado() == 1)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Cita no encontrada", HttpStatus.NOT_FOUND));

        entity.setEstado(AppointmentStatus.RECHAZADA);
        entity.setMotivoRechazo(motivo);
        Cita saved = repository.save(entity);

        // Al rechazar una solicitud pendiente, siempre invalidamos los pagos asociados
        // (si los hubiera)
        pagoRepository.findByCitaIdAndRegBorrado(id, 1).forEach(pago -> {
            if (pago.getStatus() == PagoStatus.PENDIENTE_REVISION)
                pago.setStatus(PagoStatus.RECHAZADO);
            else if (pago.getStatus() == PagoStatus.APROBADO)
                pago.setStatus(PagoStatus.CANCELADO);
            pagoRepository.save(pago);
        });

        return mapToDTO(saved);
    }

    @Transactional
    public CitaDTO cancelarCita(UUID id, String motivo, UUID tenantId, boolean reembolsar) {
        Cita entity = repository.findById(id)
                .filter(c -> c.getTenantId().equals(tenantId) && c.getRegBorrado() == 1)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Cita no encontrada", HttpStatus.NOT_FOUND));

        // 1. Si la cita estaba CONFIRMADA, retiramos el cargo de la deuda global del paciente
        if (entity.getEstado() == AppointmentStatus.CONFIRMADA) {
            Paciente paciente = pacienteRepository.findById(entity.getPacienteId()).orElseThrow();
            if (paciente.getSaldoPendiente() != null) {
                paciente.setSaldoPendiente(paciente.getSaldoPendiente().subtract(entity.getMontoTotal()));
                pacienteRepository.save(paciente);
            }
        }

        entity.setEstado(AppointmentStatus.CANCELADA);
        entity.setMotivoRechazo(motivo);
        Cita saved = repository.save(entity);

        // 2. Manejo de reembolsos y limpieza de pagos
        pagoRepository.findByCitaIdAndRegBorrado(id, 1).forEach(pago -> {
            if (pago.getStatus() == PagoStatus.PENDIENTE_REVISION) {
                pago.setStatus(PagoStatus.RECHAZADO);
            } else if (pago.getStatus() == PagoStatus.APROBADO) {
                pago.setStatus(PagoStatus.CANCELADO);
                // Si reembolsamos un pago que ya había sido aprobado, debemos "devolver" ese monto 
                // al saldo pendiente del paciente (o dejarlo como está si no se reembolsa realmente)
                if (reembolsar) {
                    Paciente p = pacienteRepository.findById(entity.getPacienteId()).orElseThrow();
                    if (p.getSaldoPendiente() != null) {
                        p.setSaldoPendiente(p.getSaldoPendiente().add(pago.getMonto()));
                        pacienteRepository.save(p);
                    }
                }
            }
            pagoRepository.save(pago);
        });

        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<CitaDTO> listarPorConfirmar(UUID tenantId, UUID sucursalId) {
        List<CitaSummaryProjection> results = (sucursalId != null)
                ? repository.findByTenantAndSucursalAndEstadoOptimized(tenantId, sucursalId,
                        AppointmentStatus.POR_CONFIRMAR)
                : repository.findByTenantAndEstadoOptimized(tenantId, AppointmentStatus.POR_CONFIRMAR);

        if (results.isEmpty())
            return new ArrayList<>();

        // Cargar pagos por lote
        Set<UUID> citaIds = results.stream().map(CitaSummaryProjection::getId).collect(Collectors.toSet());
        Map<UUID, List<Pago>> pagosMap = pagoRepository.findAllByCitaIdInAndRegBorrado(citaIds, 1).stream()
                .collect(Collectors.groupingBy(Pago::getCitaId));

        return results.stream().map(r -> mapFromProjection(r, pagosMap)).collect(Collectors.toList());
    }

    @Transactional
    public CitaDTO actualizarEstado(UUID id, AppointmentStatus nuevoEstado, BigDecimal montoTotal, UUID tenantId) {
        Cita entity = repository.findById(id)
                .filter(c -> c.getTenantId().equals(tenantId) && c.getRegBorrado() == 1)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Cita no encontrada", HttpStatus.NOT_FOUND));

        entity.setEstado(nuevoEstado);
        if (montoTotal != null)
            entity.setMontoTotal(montoTotal);
        return mapToDTO(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardSummary(UUID tenantId, UUID sucursalId, UUID doctorId) {
        OffsetDateTime now = OffsetDateTime.now(com.meyisoft.dental.system.utils.DateUtils.MEXICO_OFFSET);
        OffsetDateTime todayStart = now.toLocalDate().atStartOfDay()
                .atOffset(com.meyisoft.dental.system.utils.DateUtils.MEXICO_OFFSET);
        OffsetDateTime todayEnd = now.toLocalDate().atTime(23, 59, 59)
                .atOffset(com.meyisoft.dental.system.utils.DateUtils.MEXICO_OFFSET);
        OffsetDateTime yesterdayStart = todayStart.minusDays(1);
        OffsetDateTime yesterdayEnd = todayEnd.minusDays(1);

        // 1. Obtener todas las estadísticas de Pagos en un solo viaje
        DashboardPagoStats pagoStats = pagoRepository.findDashboardStats(tenantId, todayStart, todayEnd, yesterdayStart, yesterdayEnd, doctorId);
        BigDecimal ingresosHoy = pagoStats.getIngresosHoy() != null ? pagoStats.getIngresosHoy() : BigDecimal.ZERO;
        BigDecimal ingresosAyer = pagoStats.getIngresosAyer() != null ? pagoStats.getIngresosAyer() : BigDecimal.ZERO;

        // 2. Obtener todas las estadísticas de Citas en un solo viaje
        DashboardCitaStats citaStats = repository.findDashboardCitaStats(tenantId, sucursalId, todayStart, todayEnd, yesterdayStart, yesterdayEnd, doctorId);
        long citasHoy = citaStats.getCitasHoy() != null ? citaStats.getCitasHoy() : 0L;
        long citasAyer = citaStats.getCitasAyer() != null ? citaStats.getCitasAyer() : 0L;

        // 3. Obtener estadísticas de Pacientes
        DashboardPacienteStats pacienteStats = pacienteRepository.findDashboardPacienteStats(tenantId, todayStart, todayEnd, yesterdayStart, yesterdayEnd);
        long pacientesHoy = pacienteStats.getPacientesHoy() != null ? pacienteStats.getPacientesHoy() : 0L;
        long pacientesAyer = pacienteStats.getPacientesAyer() != null ? pacienteStats.getPacientesAyer() : 0L;

        // 4. Detalle de ingresos (Listado, requiere consulta aparte)
        List<IncomeDetailDTO> detalleHoy = pagoRepository.findIncomeDetail(
                tenantId, PagoStatus.APROBADO, todayStart, todayEnd, 1, doctorId)
                .stream()
                .map(pago -> IncomeDetailDTO.builder().monto(pago.getMonto()).fecha(pago.getCreatedAt()).build())
                .collect(Collectors.toList());

        return DashboardStatsDTO.builder()
                .ingresosPorValidar(pagoStats.getIngresosPendientes() != null ? pagoStats.getIngresosPendientes() : BigDecimal.ZERO)
                .comprobantesPendientesCount(pagoStats.getCountPendientes() != null ? pagoStats.getCountPendientes() : 0L)
                .ingresosHoy(ingresosHoy)
                .ingresosHoyTrend(calculateTrend(ingresosHoy, ingresosAyer))
                .citasHoyCount(citasHoy)
                .citasHoyTrend(citasHoy - citasAyer)
                .pacientesNuevosCount(pacientesHoy)
                .pacientesNuevosTrend(
                        calculateTrend(BigDecimal.valueOf(pacientesHoy), BigDecimal.valueOf(pacientesAyer)))
                .ingresosDetalleHoy(detalleHoy)
                .build();
    }

    private double calculateTrend(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0)
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        return ((current.doubleValue() - previous.doubleValue()) / previous.doubleValue()) * 100.0;
    }

    // --- MÉTODOS DE DISPONIBILIDAD (MANTENIDOS) ---

    @Transactional(readOnly = true)
    public List<DisponibilidadDiaDTO> obtenerDisponibilidadMes(UUID tenantId, UUID sucursalId, int mes, int anio,
            UUID servicioId) {
        YearMonth yearMonth = YearMonth.of(anio, mes);
        List<DisponibilidadDiaDTO> disponibilidad = new ArrayList<>();

        // Determinar duración a usar para los "dots" de disponibilidad
        int duracionEstimada = 30;
        if (servicioId != null) {
            duracionEstimada = servicioDentalRepository.findById(servicioId)
                    .map(s -> s.getDuracionMinutos() != null ? s.getDuracionMinutos() : 30)
                    .orElse(30);
        }

        List<Usuario> doctores = usuarioRepository
                .findByTenantIdAndSucursalIdPrincipalAndRegBorrado(tenantId, sucursalId, 1)
                .stream()
                .filter(Usuario::getActivo)
                .filter(u -> Boolean.TRUE.equals(u.getEsPersonalClinico())) // Cualquier rol que sea médico (incluye
                                                                            // OWNER)
                .collect(Collectors.toList());

        OffsetDateTime start = yearMonth.atDay(1).atStartOfDay()
                .atOffset(com.meyisoft.dental.system.utils.DateUtils.MEXICO_OFFSET);
        OffsetDateTime end = yearMonth.atEndOfMonth().atTime(23, 59, 59)
                .atOffset(com.meyisoft.dental.system.utils.DateUtils.MEXICO_OFFSET);
        List<Cita> citasMes = repository.findAllActiveByTenantAndSucursalInRange(tenantId, sucursalId, start, end);

        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate fecha = yearMonth.atDay(day);
            DayConfig config = obtenerConfiguracionDia(sucursalId, fecha);
            boolean esLaboral = config != null && config.isActive();
            boolean estaLlena = true;
            if (esLaboral && !doctores.isEmpty()) {
                List<Cita> citasDia = citasMes.stream().filter(c -> c.getFechaHora().toLocalDate().equals(fecha))
                        .collect(Collectors.toList());

                String plan = empresaRepository.findById(tenantId).map(Empresa::getPlanSuscripcion).orElse("SOLO");

                estaLlena = calcularSlotsParaFechaOptimizado(tenantId, sucursalId, fecha, duracionEstimada, doctores,
                        citasDia, plan).stream().noneMatch(SlotDisponibilidadDTO::isDisponible);
            }
            disponibilidad.add(new DisponibilidadDiaDTO(fecha, estaLlena, esLaboral));
        }
        return disponibilidad;
    }

    @Transactional(readOnly = true)
    public List<SlotDisponibilidadDTO> obtenerSlotsDisponibles(UUID tenantId, UUID sucursalId, LocalDate fecha,
            UUID servicioId) {
        ServicioDental servicio = servicioDentalRepository.findById(servicioId)
                .filter(s -> s.getTenantId().equals(tenantId) && s.getRegBorrado() == 1)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Servicio no encontrado", HttpStatus.NOT_FOUND));

        String especialidadReq = servicio.getEspecialidadRequerida();

        // Obtener el plan de la empresa para decidir si aplicar filtro estricto
        String plan = empresaRepository.findById(tenantId)
                .map(Empresa::getPlanSuscripcion)
                .orElse("SOLO");

        List<Usuario> doctores = usuarioRepository
                .findByTenantIdAndSucursalIdPrincipalAndRegBorrado(tenantId, sucursalId, 1)
                .stream()
                .filter(Usuario::getActivo)
                .filter(u -> Boolean.TRUE.equals(u.getEsPersonalClinico())) // Incluye al OWNER si atiende
                .filter(d -> {
                    // En el plan SOLO, ignoramos el filtro de especialidad técnica
                    if ("SOLO".equals(plan))
                        return true;

                    // Si el servicio no exige especialidad, cualquier doctor activo de la sucursal
                    // es válido
                    if (especialidadReq == null || especialidadReq.isBlank())
                        return true;
                    // Si exige especialidad, verificamos que el doctor la tenga en su arreglo
                    if (d.getEspecialidades() == null || d.getEspecialidades().length == 0)
                        return false;
                    return java.util.Arrays.asList(d.getEspecialidades()).contains(especialidadReq);
                })
                .collect(Collectors.toList());

        int duracion = servicio.getDuracionMinutos() != null ? servicio.getDuracionMinutos() : 30;
        OffsetDateTime s = fecha.atStartOfDay().atOffset(com.meyisoft.dental.system.utils.DateUtils.MEXICO_OFFSET);
        OffsetDateTime e = fecha.atTime(23, 59, 59).atOffset(com.meyisoft.dental.system.utils.DateUtils.MEXICO_OFFSET);
        List<Cita> citasDia = repository.findAllActiveByTenantAndSucursalInRange(tenantId, sucursalId, s, e);

        return calcularSlotsParaFechaOptimizado(tenantId, sucursalId, fecha, duracion, doctores, citasDia, plan)
                .stream()
                .filter(SlotDisponibilidadDTO::isDisponible)
                .collect(Collectors.toList());
    }

    private List<SlotDisponibilidadDTO> calcularSlotsParaFechaOptimizado(UUID tenantId, UUID sucursalId,
            LocalDate fecha, int duracion, List<Usuario> doctores, List<Cita> citasDia, String plan) {
        List<SlotDisponibilidadDTO> slots = new ArrayList<>();
        DayConfig config = obtenerConfiguracionDia(sucursalId, fecha);

        // Si la clínica está cerrada o no hay personal capacitado, no hay slots
        if (config == null || !config.isActive() || doctores.isEmpty())
            return slots;

        // Obtenemos la capacidad física de la sucursal (Número de Sillones)
        Sucursal sucursal = sucursalRepository.findById(sucursalId).orElse(null);
        int capacidadSillones = (sucursal != null && sucursal.getCapacidadAtencion() != null)
                ? sucursal.getCapacidadAtencion()
                : 1;

        // Si el plan es SOLO, forzamos la capacidad a 1 espacio físico
        if ("SOLO".equals(plan)) {
            capacidadSillones = 1;
        }

        LocalTime actual = LocalTime.parse(config.getStartTime());
        LocalTime cierre = LocalTime.parse(config.getEndTime());

        while (actual.plusMinutes(duracion).isBefore(cierre) || actual.plusMinutes(duracion).equals(cierre)) {
            LocalTime fin = actual.plusMinutes(duracion);
            OffsetDateTime s = actual.atDate(fecha).atOffset(com.meyisoft.dental.system.utils.DateUtils.MEXICO_OFFSET);
            OffsetDateTime e = fin.atDate(fecha).atOffset(com.meyisoft.dental.system.utils.DateUtils.MEXICO_OFFSET);

            // 1. Validar límite físico (Sillones ocupados en este momento)
            long citasSimultaneas = citasDia.stream()
                    .filter(c -> c.getEstado() != AppointmentStatus.CANCELADA 
                              && c.getEstado() != AppointmentStatus.RECHAZADA
                              && c.getEstado() != AppointmentStatus.AUSENTE)
                    .filter(c -> c.getFechaHora().isBefore(e)
                            && c.getFechaHora().plusMinutes(c.getDuracionMinutos()).isAfter(s))
                    .count();

            boolean haySillonLibre = citasSimultaneas < capacidadSillones;
            boolean alMenosUnDoctorLibre = false;

            // 2. Si hay espacio físico, buscamos si hay manos (doctores) libres
            if (haySillonLibre) {
                for (Usuario doctor : doctores) {
                    boolean doctorOcupado = citasDia.stream()
                            .filter(c -> c.getDoctorId() != null && c.getDoctorId().equals(doctor.getId()))
                            .filter(c -> c.getEstado() != AppointmentStatus.CANCELADA 
                                      && c.getEstado() != AppointmentStatus.RECHAZADA
                                      && c.getEstado() != AppointmentStatus.AUSENTE)
                            .anyMatch(c -> c.getFechaHora().isBefore(e)
                                    && c.getFechaHora().plusMinutes(c.getDuracionMinutos()).isAfter(s));

                    if (!doctorOcupado) {
                        alMenosUnDoctorLibre = true;
                        break; // Con un doctor libre y un sillón libre, el slot es válido
                    }
                }
            }

            boolean disp = haySillonLibre && alMenosUnDoctorLibre;

            // Regla: No se pueden agendar citas en el pasado
            if (fecha.equals(LocalDate.now(com.meyisoft.dental.system.utils.DateUtils.MEXICO_OFFSET))
                    && s.isBefore(OffsetDateTime.now(com.meyisoft.dental.system.utils.DateUtils.MEXICO_OFFSET))) {
                disp = false;
            }

            slots.add(new SlotDisponibilidadDTO(actual, fin, disp));
            actual = actual.plusMinutes(30);
        }
        return slots;
    }

    private DayConfig obtenerConfiguracionDia(UUID sucursalId, LocalDate fecha) {
        Sucursal sucursal = sucursalRepository.findById(sucursalId).orElse(null);
        if (sucursal == null || sucursal.getHorariosLaborales() == null)
            return null;
        try {
            Map<String, DayConfig> configs = objectMapper.readValue(sucursal.getHorariosLaborales(),
                    new TypeReference<Map<String, DayConfig>>() {
                    });
            String key = fecha.getDayOfWeek().name().toLowerCase();
            return configs.get(key);
        } catch (Exception e) {
            return null;
        }
    }

    private String generarSiguienteFolio(UUID tenantId, String tipo) {
        LocalDate hoy = LocalDate.now();
        // El repositorio usa @Lock(LockModeType.PESSIMISTIC_WRITE)
        RegistroFolio reg = folioRepository.findByTenantIdAndTipoAndFechaAndRegBorrado(tenantId, tipo, hoy, 1)
                .orElseGet(() -> {
                    try {
                        RegistroFolio n = RegistroFolio.builder()
                                .id(UUID.randomUUID())
                                .tipo(tipo)
                                .fecha(hoy)
                                .ultimoNumero(0)
                                .build();
                        n.setTenantId(tenantId);
                        n.setRegBorrado(1);
                        return folioRepository.saveAndFlush(n);
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        // Si otro hilo insertó el registro justo después de nuestro find inicial
                        return folioRepository.findByTenantIdAndTipoAndFechaAndRegBorrado(tenantId, tipo, hoy, 1)
                                .orElseThrow(() -> new RuntimeException("Error crítico de concurrencia al generar folio", e));
                    }
                });
        
        reg.setUltimoNumero(reg.getUltimoNumero() + 1);
        folioRepository.save(reg);
        
        return String.format("CIT-%s-%04d", hoy.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")),
                reg.getUltimoNumero());
    }

    private void validarHorarioSucursal(UUID sucursalId, OffsetDateTime fechaHora, Integer duracion) {
        DayConfig config = obtenerConfiguracionDia(sucursalId, fechaHora.toLocalDate());
        if (config != null && (!config.isActive()
                || fechaHora.toLocalTime().isBefore(LocalTime.parse(config.getStartTime()))
                || fechaHora.toLocalTime().plusMinutes(duracion).isAfter(LocalTime.parse(config.getEndTime())))) {
            throw new BusinessException("OUT_OF_HOURS", "Fuera de horario laboral", HttpStatus.BAD_REQUEST);
        }
    }

    private CitaDTO mapToDTO(Cita entity) {
        return mapToDTOOptimized(entity, null, null, null, null);
    }

    private CitaDTO mapToDTOOptimized(Cita entity, Map<UUID, Paciente> pacientes, Map<UUID, Usuario> doctores,
            Map<UUID, ServicioDental> servicios, Map<UUID, List<Pago>> pagosPorCita) {
        CitaDTO dto = CitaDTO.builder()
                .id(entity.getId())
                .pacienteId(entity.getPacienteId())
                .doctorId(entity.getDoctorId())
                .sucursalId(entity.getSucursalId())
                .servicioId(entity.getServicioId())
                .fechaHora(entity.getFechaHora())
                .duracionMinutos(entity.getDuracionMinutos())
                .estado(entity.getEstado())
                .source(entity.getSource())
                .folio(entity.getFolio())
                .motivoConsulta(entity.getMotivoConsulta())
                .notasRecepcion(entity.getNotasRecepcion())
                .montoTotal(entity.getMontoTotal())
                .montoPagado(entity.getMontoPagado())
                .precioServicio(entity.getPrecioServicio())
                .motivoRechazo(entity.getMotivoRechazo())
                .createdAt(entity.getCreatedAt())
                .build();

        if (entity.getPacienteId() != null) {
            Paciente p = (pacientes != null) ? pacientes.get(entity.getPacienteId())
                    : pacienteRepository.findById(entity.getPacienteId()).orElse(null);
            if (p != null) {
                dto.setPacienteNombre(p.getNombreCompleto());
                dto.setPacienteTelefono(p.getTelefono());
            }
        }
        if (entity.getDoctorId() != null) {
            Usuario d = (doctores != null) ? doctores.get(entity.getDoctorId())
                    : usuarioRepository.findById(entity.getDoctorId()).orElse(null);
            if (d != null)
                dto.setDoctorNombre(d.getNombreCompleto());
        }
        if (entity.getServicioId() != null) {
            ServicioDental s = (servicios != null) ? servicios.get(entity.getServicioId())
                    : servicioDentalRepository.findById(entity.getServicioId()).orElse(null);
            if (s != null) {
                dto.setServicioNombre(s.getNombre());
                dto.setProcedimientoQuirurgico(s.getProcedimientoQuirurgico());
            }
        }

        // Determinar el Estado del Ticket para el frontend
        if (dto.getMontoTotal() != null) {
            BigDecimal total = dto.getMontoTotal();
            BigDecimal pagado = dto.getMontoPagado() != null ? dto.getMontoPagado() : BigDecimal.ZERO;

            if (total.compareTo(BigDecimal.ZERO) == 0) {
                dto.setTicketStatus(TicketStatus.CORTESIA);
            } else if (pagado.compareTo(BigDecimal.ZERO) == 0) {
                dto.setTicketStatus(TicketStatus.PENDIENTE);
            } else if (pagado.compareTo(total) >= 0) {
                dto.setTicketStatus(TicketStatus.LIQUIDADO);
            } else {
                dto.setTicketStatus(TicketStatus.ABONADO);
            }
        } else {
            dto.setTicketStatus(TicketStatus.POR_DEFINIR);
        }

        // Adjuntar comprobante si existe y calcular monto pagado real
        List<Pago> pagos = (pagosPorCita != null) ? pagosPorCita.get(entity.getId())
                : pagoRepository.findByCitaIdAndRegBorrado(entity.getId(), 1);
        
        BigDecimal totalPagadoReal = BigDecimal.ZERO;
        
        if (pagos != null) {
            // Calcular el total pagado (Aprobados + Pendientes de Revisión)
            totalPagadoReal = pagos.stream()
                    .filter(p -> p.getStatus() == PagoStatus.APROBADO || p.getStatus() == PagoStatus.PENDIENTE_REVISION)
                    .map(Pago::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            pagos.stream()
                    .filter(p -> p.getStatus() == PagoStatus.PENDIENTE_REVISION)
                    .findFirst()
                    .ifPresent(p -> dto.setTicketStatus(TicketStatus.EN_REVISION));

            pagos.stream().filter(p -> p.getComprobanteUrl() != null).findFirst()
                    .ifPresent(p -> dto.setComprobanteUrl(p.getComprobanteUrl()));
        }

        // Actualizar el DTO con el monto real calculado
        dto.setMontoPagado(totalPagadoReal);

        // Recalcular el Estado del Ticket con el monto real
        if (dto.getMontoTotal() != null) {
            BigDecimal total = dto.getMontoTotal();
            BigDecimal pagado = totalPagadoReal;

            if (total.compareTo(BigDecimal.ZERO) == 0) {
                dto.setTicketStatus(TicketStatus.CORTESIA);
            } else if (pagado.compareTo(BigDecimal.ZERO) == 0) {
                dto.setTicketStatus(TicketStatus.PENDIENTE);
            } else if (pagado.compareTo(total) >= 0) {
                dto.setTicketStatus(TicketStatus.LIQUIDADO);
            } else {
                dto.setTicketStatus(TicketStatus.ABONADO);
            }
        }
        return dto;
    }

    private void validarTelefono(String telefono) {
        if (telefono == null || !telefono.matches("^[0-9]{10}$"))
            throw new BusinessException("INVALID_PHONE", "Teléfono inválido", HttpStatus.BAD_REQUEST);
    }
}
