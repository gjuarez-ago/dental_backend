package com.meyisoft.dental.system.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meyisoft.dental.system.entity.Cita;
import com.meyisoft.dental.system.entity.Empresa;
import com.meyisoft.dental.system.entity.Sucursal;
import com.meyisoft.dental.system.entity.Usuario;
import com.meyisoft.dental.system.enums.AppointmentStatus;
import com.meyisoft.dental.system.enums.UserRole;
import com.meyisoft.dental.system.exception.BusinessException;
import com.meyisoft.dental.system.models.dto.*;
import com.meyisoft.dental.system.models.request.BookingPublicMarketplaceRequest;
import com.meyisoft.dental.system.models.response.PageResult;
import com.meyisoft.dental.system.repository.*;
import com.meyisoft.dental.system.utils.DateUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketplaceSearchService {

    @PersistenceContext
    private EntityManager em;

    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioRepository usuarioRepository;
    private final ServicioDentalRepository servicioDentalRepository;
    private final CitaRepository citaRepository;
    private final ObjectMapper objectMapper;
    private final CitaService citaService;

    private static final Map<String, String> GIRO_NOMBRES = new LinkedHashMap<>();

    static {
        GIRO_NOMBRES.put("DENTAL",        "Odontología");
        GIRO_NOMBRES.put("GENERAL",       "Medicina General");
        GIRO_NOMBRES.put("PSICOLOGIA",    "Psicología");
        GIRO_NOMBRES.put("GINECOLOGIA",   "Ginecología");
        GIRO_NOMBRES.put("PEDIATRIA",     "Pediatría");
        GIRO_NOMBRES.put("DERMATOLOGIA",  "Dermatología");
        GIRO_NOMBRES.put("NUTRICION",     "Nutrición");
        GIRO_NOMBRES.put("ORTOPEDIA",     "Ortopedia");
        GIRO_NOMBRES.put("CARDIOLOGIA",   "Cardiología");
        GIRO_NOMBRES.put("OPTOMETRIA",    "Optometría");
        GIRO_NOMBRES.put("NEUROLOGIA",    "Neurología");
        GIRO_NOMBRES.put("FISIOTERAPIA",  "Fisioterapia");
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResult<EspecialistaCardDTO> searchEspecialistas(
            String estadoId, String municipioId, String giro,
            String q, String modalidad, String sort, int page, int size) {

        String baseFrom = """
                FROM empresas e
                JOIN sucursales s ON s.id = e.sucursal_id_principal AND s.reg_borrado = 1
                JOIN catalogo_estados ce ON ce.id = s.estado_id
                LEFT JOIN catalogo_municipios cm ON cm.id = s.municipio_id
                LEFT JOIN usuarios u ON u.tenant_id = e.id
                    AND u.rol = 'OWNER' AND u.reg_borrado = 1
                """;

        StringBuilder where = new StringBuilder("""
                WHERE e.activo = true AND e.reg_borrado = 1
                """);

        Map<String, Object> params = new HashMap<>();

        if (estadoId != null && !estadoId.isBlank()) {
            where.append(" AND s.estado_id = :estadoId ");
            params.put("estadoId", UUID.fromString(estadoId));
        }

        if (municipioId != null && !municipioId.isBlank()) {
            where.append(" AND s.municipio_id = :municipioId ");
            params.put("municipioId", UUID.fromString(municipioId));
        }
        if (giro != null && !giro.isBlank()) {
            where.append(" AND e.giro = :giro ");
            params.put("giro", giro.toUpperCase());
        }
        if (q != null && !q.isBlank()) {
            where.append("""
                     AND (
                         LOWER(e.nombre_comercial) LIKE :q
                      OR LOWER(u.nombre_completo)  LIKE :q
                      OR LOWER(u.biografia)         LIKE :q
                     )
                    """);
            params.put("q", "%" + q.toLowerCase() + "%");
        }
        if (modalidad != null && !modalidad.isBlank() && !modalidad.equals("AMBAS")) {
            where.append(" AND (e.modalidad = :modalidad OR e.modalidad = 'AMBAS') ");
            params.put("modalidad", modalidad.toUpperCase());
        }

        // Count
        Query countQ = em.createNativeQuery("SELECT COUNT(*) " + baseFrom + where);
        params.forEach(countQ::setParameter);
        long total = ((Number) countQ.getSingleResult()).longValue();

        if (total == 0) {
            return PageResult.<EspecialistaCardDTO>builder()
                    .content(Collections.emptyList())
                    .totalElements(0).totalPages(0).page(page).size(size)
                    .build();
        }

        // Order
        String orderBy = switch (sort != null ? sort : "") {
            case "PRECIO_ASC"  -> "precio_desde ASC NULLS LAST";
            case "PRECIO_DESC" -> "precio_desde DESC NULLS LAST";
            default            -> "e.nombre_comercial ASC";
        };

        String selectSql = """
                SELECT
                    CAST(e.id AS text)                     AS tenant_id,
                    e.nombre_comercial,
                    e.giro,
                    e.modalidad,
                    ce.nombre                              AS estado_nombre,
                    cm.nombre                              AS municipio_nombre,
                    COALESCE(u.fotografia_url, e.logo_url) AS fotografia_url,
                    u.nombre_completo                      AS doctor_nombre,
                    u.biografia                            AS biografia,
                    COALESCE(
                        (SELECT MIN(sd.precio_base)
                         FROM servicios_dentales sd
                         WHERE sd.tenant_id = e.id AND sd.reg_borrado = 1), 0
                    )                                      AS precio_desde
                """ + baseFrom + where + " ORDER BY " + orderBy
                + " LIMIT :lim OFFSET :off";

        Query dataQ = em.createNativeQuery(selectSql);
        params.forEach(dataQ::setParameter);
        dataQ.setParameter("lim", size);
        dataQ.setParameter("off", page * size);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQ.getResultList();

        List<EspecialistaCardDTO> content = rows.stream()
                .map(this::mapRow)
                .collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) total / size);
        return PageResult.<EspecialistaCardDTO>builder()
                .content(content).totalElements(total)
                .totalPages(totalPages).page(page).size(size)
                .build();
    }

    private EspecialistaCardDTO mapRow(Object[] r) {
        String giroVal = str(r[2]);
        return EspecialistaCardDTO.builder()
                .tenantId(str(r[0]))
                .nombreComercial(str(r[1]))
                .giro(giroVal)
                .giroNombre(GIRO_NOMBRES.getOrDefault(giroVal, giroVal))
                .modalidad(str(r[3]))
                .estadoNombre(str(r[4]))
                .municipioNombre(str(r[5]))
                .fotografiaUrl(str(r[6]))
                .nombreDoctor(str(r[7]))
                .biografia(str(r[8]))
                .precioDesde(r[9] != null ? new BigDecimal(r[9].toString()) : BigDecimal.ZERO)
                .calificacion(5.0)
                .totalResenas(0)
                .build();
    }

    private static String str(Object o) {
        return o != null ? o.toString() : null;
    }

    // ─── Giros ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<GiroOptionDTO> getGiros(String q) {
        String sql = """
                SELECT e.giro, COUNT(DISTINCT e.id) AS total
                FROM empresas e
                JOIN sucursales s ON s.id = e.sucursal_id_principal AND s.reg_borrado = 1
                WHERE e.activo = true AND e.reg_borrado = 1
                GROUP BY e.giro
                ORDER BY total DESC, e.giro ASC
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql).getResultList();

        String qLower = (q != null && !q.isBlank()) ? q.trim().toLowerCase() : null;

        return rows.stream()
                .map(r -> {
                    String valor = str(r[0]);
                    String nombre = GIRO_NOMBRES.getOrDefault(valor, valor);
                    return GiroOptionDTO.builder()
                            .valor(valor)
                            .nombre(nombre)
                            .totalEspecialistas(((Number) r[1]).longValue())
                            .build();
                })
                .filter(g -> qLower == null || g.getNombre().toLowerCase().contains(qLower))
                .collect(Collectors.toList());
    }

    // ─── Agenda pública ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AgendaPublicaDTO getAgendaPublica(UUID tenantId, LocalDate fechaInicio, int dias) {
        Empresa empresa = empresaRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Especialista no encontrado", HttpStatus.NOT_FOUND));

        if (!Boolean.TRUE.equals(empresa.getActivo())) {
            throw new BusinessException("NOT_FOUND", "Especialista no disponible", HttpStatus.NOT_FOUND);
        }

        UUID sucursalId = empresa.getSucursalIdPrincipal();
        if (sucursalId == null) {
            throw new BusinessException("NOT_FOUND", "El especialista aún no ha configurado su agenda", HttpStatus.NOT_FOUND);
        }

        Sucursal sucursal = sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Sucursal no encontrada", HttpStatus.NOT_FOUND));

        // Load owner for profile data
        Usuario owner = usuarioRepository
                .findByTenantIdAndRolAndRegBorrado(tenantId, UserRole.OWNER, 1)
                .stream().findFirst().orElse(null);

        // Load all active clinical staff
        List<Usuario> doctores = usuarioRepository
                .findByTenantIdAndSucursalIdPrincipalAndRegBorrado(tenantId, sucursalId, 1)
                .stream()
                .filter(u -> Boolean.TRUE.equals(u.getActivo()) && Boolean.TRUE.equals(u.getEsPersonalClinico()))
                .collect(Collectors.toList());

        // Load services
        List<ServicioPublicoDTO> servicios = servicioDentalRepository
                .findByTenantIdAndRegBorrado(tenantId, 1)
                .stream()
                .map(s -> ServicioPublicoDTO.builder()
                        .id(s.getId())
                        .nombre(s.getNombre())
                        .descripcion(s.getDescripcion())
                        .precioBase(s.getPrecioBase())
                        .duracionMinutos(s.getDuracionMinutos() != null ? s.getDuracionMinutos() : 30)
                        .build())
                .collect(Collectors.toList());

        // Load all citas for the range (single query — no N+1)
        OffsetDateTime rangeStart = fechaInicio.atStartOfDay().atOffset(DateUtils.MEXICO_OFFSET);
        OffsetDateTime rangeEnd   = fechaInicio.plusDays(dias).atStartOfDay().atOffset(DateUtils.MEXICO_OFFSET);
        List<Cita> citasRango = citaRepository.findAllActiveByTenantAndSucursalInRange(tenantId, sucursalId, rangeStart, rangeEnd);

        // Parse schedule config once
        Map<String, CitaService.DayConfig> horarios = parseHorarios(sucursal.getHorariosLaborales());

        String plan = empresa.getPlanSuscripcion() != null ? empresa.getPlanSuscripcion() : "SOLO";
        int capacidad = "SOLO".equals(plan) ? 1
                : (sucursal.getCapacidadAtencion() != null ? sucursal.getCapacidadAtencion() : 1);

        // Default duration: first service duration or 30 min
        int duracionDefault = servicios.isEmpty() ? 30 : servicios.get(0).getDuracionMinutos();

        // Build dias
        List<DiaAgendaDTO> dias_ = new ArrayList<>();
        for (int i = 0; i < dias; i++) {
            LocalDate fecha = fechaInicio.plusDays(i);
            String key = fecha.getDayOfWeek().name().toLowerCase();
            CitaService.DayConfig config = horarios.get(key);
            boolean esLaboral = config != null && config.isActive();

            List<SlotPublicoDTO> slots = Collections.emptyList();
            boolean estaLlena = true;

            if (esLaboral && !doctores.isEmpty()) {
                List<Cita> citasDia = citasRango.stream()
                        .filter(c -> c.getFechaHora().toLocalDate().equals(fecha))
                        .collect(Collectors.toList());

                slots = calcularSlotsPublicos(fecha, config, duracionDefault, doctores, citasDia, capacidad);
                estaLlena = slots.stream().noneMatch(SlotPublicoDTO::isDisponible);
            }

            dias_.add(DiaAgendaDTO.builder()
                    .fecha(fecha.toString())
                    .esDiaLaboral(esLaboral)
                    .estaLlena(estaLlena)
                    .slots(slots)
                    .build());
        }

        String giroNombre = GIRO_NOMBRES.getOrDefault(empresa.getGiro(), empresa.getGiro());

        int diasAnticipacion = empresa.getDiasAnticipacionReserva() != null
                ? empresa.getDiasAnticipacionReserva() : 1;

        return AgendaPublicaDTO.builder()
                .tenantId(tenantId.toString())
                .nombreComercial(empresa.getNombreComercial())
                .nombreDoctor(owner != null ? owner.getNombreCompleto() : null)
                .giroNombre(giroNombre)
                .fotografiaUrl(owner != null ? owner.getFotografiaUrl() : empresa.getLogoUrl())
                .biografia(owner != null ? owner.getBiografia() : null)
                .modalidad(empresa.getModalidad())
                .calificacion(5.0)
                .totalResenas(0)
                .diasAnticipacionReserva(diasAnticipacion)
                .servicios(servicios)
                .diasDisponibles(dias_)
                .build();
    }

    private List<SlotPublicoDTO> calcularSlotsPublicos(
            LocalDate fecha, CitaService.DayConfig config, int duracion,
            List<Usuario> doctores, List<Cita> citasDia, int capacidad) {

        List<SlotPublicoDTO> slots = new ArrayList<>();
        LocalTime actual = LocalTime.parse(config.getStartTime());
        LocalTime cierre = LocalTime.parse(config.getEndTime());
        OffsetDateTime ahora = OffsetDateTime.now(DateUtils.MEXICO_OFFSET);

        while (!actual.plusMinutes(duracion).isAfter(cierre)) {
            LocalTime fin = actual.plusMinutes(duracion);
            OffsetDateTime slotStart = actual.atDate(fecha).atOffset(DateUtils.MEXICO_OFFSET);
            OffsetDateTime slotEnd   = fin.atDate(fecha).atOffset(DateUtils.MEXICO_OFFSET);

            long simultaneas = citasDia.stream()
                    .filter(c -> c.getEstado() != AppointmentStatus.CANCELADA
                            && c.getEstado() != AppointmentStatus.RECHAZADA
                            && c.getEstado() != AppointmentStatus.AUSENTE)
                    .filter(c -> c.getFechaHora().isBefore(slotEnd)
                            && c.getFechaHora().plusMinutes(
                                    c.getDuracionMinutos() != null ? c.getDuracionMinutos() : duracion).isAfter(slotStart))
                    .count();

            boolean haySillon = simultaneas < capacidad;
            boolean doctorLibre = false;

            if (haySillon) {
                for (Usuario doc : doctores) {
                    boolean ocupado = citasDia.stream()
                            .filter(c -> c.getDoctorId() != null && c.getDoctorId().equals(doc.getId()))
                            .filter(c -> c.getEstado() != AppointmentStatus.CANCELADA
                                    && c.getEstado() != AppointmentStatus.RECHAZADA
                                    && c.getEstado() != AppointmentStatus.AUSENTE)
                            .anyMatch(c -> c.getFechaHora().isBefore(slotEnd)
                                    && c.getFechaHora().plusMinutes(
                                            c.getDuracionMinutos() != null ? c.getDuracionMinutos() : duracion).isAfter(slotStart));
                    if (!ocupado) { doctorLibre = true; break; }
                }
            }

            boolean disponible = haySillon && doctorLibre && !slotStart.isBefore(ahora);

            slots.add(SlotPublicoDTO.builder()
                    .horaInicio(actual.format(DateTimeFormatter.ofPattern("HH:mm")))
                    .horaFin(fin.format(DateTimeFormatter.ofPattern("HH:mm")))
                    .disponible(disponible)
                    .build());

            actual = actual.plusMinutes(30);
        }
        return slots;
    }

    private Map<String, CitaService.DayConfig> parseHorarios(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Error parsing horariosLaborales: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    // ─── Solicitar cita ───────────────────────────────────────────────────────

    @Transactional
    public Map<String, String> solicitarCita(BookingPublicMarketplaceRequest req) {
        Empresa empresa = empresaRepository.findById(req.getTenantId())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Especialista no encontrado", HttpStatus.NOT_FOUND));

        UUID sucursalId = empresa.getSucursalIdPrincipal();
        if (sucursalId == null) {
            throw new BusinessException("NOT_FOUND", "El especialista no tiene sucursal configurada", HttpStatus.NOT_FOUND);
        }

        // Parse date/time → OffsetDateTime
        LocalDate fecha = LocalDate.parse(req.getFecha());
        LocalTime hora  = LocalTime.parse(req.getHoraInicio());
        OffsetDateTime fechaHora = hora.atDate(fecha).atOffset(DateUtils.MEXICO_OFFSET);

        com.meyisoft.dental.system.models.dto.CitaDTO dto = com.meyisoft.dental.system.models.dto.CitaDTO.builder()
                .sucursalId(sucursalId)
                .servicioId(req.getServicioId())
                .fechaHora(fechaHora)
                .pacienteNombre(req.getNombrePaciente())
                .pacienteTelefono(req.getTelefonoPaciente())
                .source("PUBLIC")
                .motivoConsulta(req.getNotas())
                .build();

        CitaDTO result = citaService.agendar(dto, req.getTenantId(), null);

        return Map.of(
                "citaId", result.getId().toString(),
                "folio",  result.getFolio() != null ? result.getFolio() : ""
        );
    }
}
