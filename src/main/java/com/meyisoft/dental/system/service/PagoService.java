package com.meyisoft.dental.system.service;

import com.meyisoft.dental.system.entity.Cita;
import com.meyisoft.dental.system.entity.Paciente;
import com.meyisoft.dental.system.entity.Pago;
import com.meyisoft.dental.system.entity.ServicioDental;
import com.meyisoft.dental.system.enums.AppointmentStatus;
import com.meyisoft.dental.system.enums.PagoStatus;
import com.meyisoft.dental.system.enums.PaymentMethod;
import com.meyisoft.dental.system.enums.TicketStatus;
import com.meyisoft.dental.system.config.AuditAction;
import com.meyisoft.dental.system.exception.BusinessException;
import com.meyisoft.dental.system.models.dto.CitaResumenFinancieroDTO;
import com.meyisoft.dental.system.models.dto.PagoDTO;
import com.meyisoft.dental.system.repository.CitaRepository;
import com.meyisoft.dental.system.repository.PacienteRepository;
import com.meyisoft.dental.system.repository.PagoRepository;
import com.meyisoft.dental.system.repository.ServicioDentalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository repository;
    private final PacienteRepository pacienteRepository;
    private final CitaRepository citaRepository;
    private final ServicioDentalRepository servicioRepository;
    private final com.meyisoft.dental.system.repository.SucursalRepository sucursalRepository;
    private final NotificationService notificationService;

    @Transactional
    @AuditAction(modulo = "PAGOS", accion = "REGISTRAR", descripcion = "Registro de nuevo pago o abono")
    public PagoDTO registrarPago(PagoDTO dto, UUID tenantId) {
        log.info("Registrando pago de {} para cita {}", dto.getMonto(), dto.getCitaId());

        // 0. ACTUALIZAR MONTO TOTAL DE LA CITA SI SE PROVEE
        if (dto.getMontoTotalCita() != null) {
            Cita cita = citaRepository.findById(dto.getCitaId())
                    .filter(c -> c.getTenantId().equals(tenantId) && c.getRegBorrado() == 1)
                    .orElseThrow(() -> new BusinessException("NOT_FOUND", "Cita no encontrada", HttpStatus.NOT_FOUND));

            // Validar que el nuevo costo no sea menor a lo ya pagado (evitar saldos a favor
            // accidentales)
            BigDecimal yaPagado = cita.getMontoPagado() != null ? cita.getMontoPagado() : BigDecimal.ZERO;
            if (dto.getMontoTotalCita().compareTo(yaPagado) < 0) {
                throw new BusinessException("INVALID_COST",
                        "El costo total (" + dto.getMontoTotalCita() + ") no puede ser menor al monto ya pagado ("
                                + yaPagado + ")",
                        HttpStatus.BAD_REQUEST);
            }

            BigDecimal diferencia = dto.getMontoTotalCita()
                    .subtract(cita.getMontoTotal() != null ? cita.getMontoTotal() : BigDecimal.ZERO);

            cita.setMontoTotal(dto.getMontoTotalCita());
            citaRepository.save(cita);

            // Ajustar deuda del paciente por el cambio de precio
            pacienteRepository.findById(dto.getPacienteId()).ifPresent(p -> {
                if (p.getSaldoPendiente() == null)
                    p.setSaldoPendiente(BigDecimal.ZERO);
                p.setSaldoPendiente(p.getSaldoPendiente().add(diferencia));
                pacienteRepository.save(p);
            });

            log.info("Costo total de la cita actualizado. Diferencia de deuda: {}", diferencia);

            // Si el nuevo precio es 0, intentamos finalizar de una vez
            checkAndFinalizeCita(cita.getId(), tenantId);
        }

        // 1. Validar Cita y Saldo Actual (Usará el nuevo monto si se actualizó arriba)
        CitaResumenFinancieroDTO resumen = obtenerResumenCita(dto.getCitaId(), tenantId);

        if (dto.getMonto().compareTo(BigDecimal.ZERO) > 0
                && dto.getMonto().compareTo(resumen.getSaldoPendiente()) > 0) {
            throw new BusinessException("EXCESSIVE_PAYMENT",
                    "No se permite saldo a favor. El monto máximo permitido es " + resumen.getSaldoPendiente(),
                    HttpStatus.BAD_REQUEST);
        }

        // 2. Validar Paciente
        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .filter(p -> p.getTenantId().equals(tenantId) && p.getRegBorrado() == 1)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Paciente no encontrado", HttpStatus.NOT_FOUND));

        // 2. Crear Entidad Pago
        // Los pagos registrados desde el CRM se aprueban de inmediato (efectivo,
        // transferencia verificada en sitio, etc.)
        PagoStatus initialStatus = PagoStatus.APROBADO;

        Pago entity = Pago.builder()
                .id(UUID.randomUUID())
                .citaId(dto.getCitaId())
                .doctorId(resumen.getDoctorId()) // Campo desnormalizado para optimización
                .pacienteId(dto.getPacienteId())
                .monto(dto.getMonto())
                .metodoPago(dto.getMetodoPago())
                .notas(dto.getNotas())
                .folioPago(dto.getFolioPago())
                .comprobanteUrl(dto.getComprobanteUrl())
                .status(initialStatus)
                .build();

        entity.setTenantId(tenantId);
        entity.setRegBorrado(1);
        // 2. REGISTRAR EL PAGO EN DB (Solo si el monto es real)
        if (dto.getMonto().compareTo(BigDecimal.ZERO) > 0) {
            repository.save(entity);
        }

        // 3. ACTUALIZAR DEUDA DEL PACIENTE
        // La deuda disminuye con el pago
        if (paciente.getSaldoPendiente() == null) {
            paciente.setSaldoPendiente(BigDecimal.ZERO);
        }
        paciente.setSaldoPendiente(paciente.getSaldoPendiente().subtract(dto.getMonto()));
        pacienteRepository.save(paciente);

        // 4. FINALIZAR CITA SI SALDO ES 0 Y ESTÁ EN POR_LIQUIDAR
        checkAndFinalizeCita(dto.getCitaId(), tenantId);

        // 5. ENVIAR COMPROBANTE AL PACIENTE (Solo si hubo pago real y está aprobado)
        if (dto.getMonto().compareTo(BigDecimal.ZERO) > 0 && entity.getStatus() == PagoStatus.APROBADO) {
            enviarComprobantePago(entity, paciente, dto.getCitaId(), tenantId);
        }

        return mapToDTO(entity);
    }

    @Transactional(readOnly = true)
    public CitaResumenFinancieroDTO obtenerResumenCita(UUID citaId, UUID tenantId) {
        Map<String, Object> summary = citaRepository.findFinancialSummary(citaId, tenantId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Cita no encontrada", HttpStatus.NOT_FOUND));

        List<Pago> pagos = repository.findByCitaIdAndRegBorrado(citaId, 1);

        BigDecimal montoTotalCita = (BigDecimal) summary.get("montoTotal");
        BigDecimal precioBaseServicio = (BigDecimal) summary.get("precioBase");

        // Prioridad: 1. montoTotal de la cita, 2. precioBase del servicio
        BigDecimal costoBase = (montoTotalCita != null) ? montoTotalCita
                : (precioBaseServicio != null ? precioBaseServicio : BigDecimal.ZERO);

        // Sumamos los pagos APROBADOS
        BigDecimal totalPagadoAprobado = pagos.stream()
                .filter(p -> p.getStatus() == PagoStatus.APROBADO)
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Sumamos los pagos PENDIENTES DE REVISIÓN (anticipos por validar)
        BigDecimal totalPendienteRevision = pagos.stream()
                .filter(p -> p.getStatus() == PagoStatus.PENDIENTE_REVISION)
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Para fines de saldo, consideramos ambos
        BigDecimal totalPagadoConsolidado = totalPagadoAprobado.add(totalPendienteRevision);

        BigDecimal saldoPendienteCita = costoBase.subtract(totalPagadoConsolidado);

        // Determinar Estado del Ticket
        TicketStatus estadoTicket;
        boolean tienePendientesRevision = pagos.stream().anyMatch(p -> p.getStatus() == PagoStatus.PENDIENTE_REVISION);

        if (montoTotalCita == null) {
            estadoTicket = TicketStatus.POR_DEFINIR;
        } else if (tienePendientesRevision) {
            estadoTicket = TicketStatus.EN_REVISION;
        } else if (montoTotalCita.compareTo(BigDecimal.ZERO) == 0) {
            estadoTicket = TicketStatus.CORTESIA;
        } else if (totalPagadoConsolidado.compareTo(BigDecimal.ZERO) == 0) {
            estadoTicket = TicketStatus.PENDIENTE;
        } else if (totalPagadoConsolidado.compareTo(montoTotalCita) >= 0) {
            estadoTicket = TicketStatus.LIQUIDADO;
        } else {
            estadoTicket = TicketStatus.ABONADO;
        }

        return CitaResumenFinancieroDTO.builder()
                .citaId(citaId)
                .doctorId((UUID) summary.get("doctorId"))
                .pacienteNombre((String) summary.get("pacienteNombre"))
                .servicioNombre((String) summary.get("servicioNombre"))
                .precioBase(costoBase)
                .totalPagado(totalPagadoConsolidado)
                .saldoPendiente(saldoPendienteCita)
                .costoDefinido(montoTotalCita != null)
                .estadoTicket(estadoTicket)
                .historialPagos(pagos.stream().map(this::mapToDTO).collect(Collectors.toList()))
                .build();
    }

    @Transactional
    @AuditAction(modulo = "PAGOS", accion = "ACTUALIZAR_STATUS", descripcion = "Cambio de estado en pago (Aprobación/Rechazo)")
    public PagoDTO actualizarStatus(UUID pagoId, PagoStatus nuevoStatus, String motivo, UUID tenantId) {
        Pago pago = repository.findById(pagoId)
                .filter(p -> p.getTenantId().equals(tenantId) && p.getRegBorrado() == 1)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Pago no encontrado", HttpStatus.NOT_FOUND));

        pago.setStatus(nuevoStatus);
        if (nuevoStatus == PagoStatus.RECHAZADO) {
            pago.setMotivoRechazo(motivo);
            if (motivo == null || motivo.trim().isEmpty()) {
                throw new BusinessException("NOT_FOUND", "El motivo de rechazo es obligatorio", HttpStatus.BAD_REQUEST);
            }
        }
        Pago saved = repository.save(pago);

        // Si el pago fue aprobado, verificar si se debe finalizar la cita
        if (nuevoStatus == PagoStatus.APROBADO) {
            checkAndFinalizeCita(pago.getCitaId(), tenantId);

            // También enviamos el comprobante de pago
            pacienteRepository.findById(pago.getPacienteId())
                    .ifPresent(paciente -> enviarComprobantePago(saved, paciente, pago.getCitaId(), tenantId));
        } else {
            // Si se cancela o rechaza, sincronizamos el monto pagado de la cita
            sincronizarMontoPagadoCita(pago.getCitaId(), tenantId);
        }

        return mapToDTO(saved);
    }

    private void enviarComprobantePago(Pago pago, Paciente paciente, UUID citaId, UUID tenantId) {
        try {
            Cita cita = citaRepository.findById(citaId).orElse(null);
            if (cita == null)
                return;

            ServicioDental servicio = (cita.getServicioId() != null)
                    ? servicioRepository.findById(cita.getServicioId()).orElse(null)
                    : null;
            com.meyisoft.dental.system.entity.Sucursal sucursal = sucursalRepository.findById(cita.getSucursalId())
                    .orElse(null);

            CitaResumenFinancieroDTO resumen = obtenerResumenCita(citaId, tenantId);

            java.util.Map<String, String> vars = java.util.Map.of(
                    "NOMBRE_CLINICA", "Clínica Dental",
                    "NOMBRE_PACIENTE", paciente.getNombreCompleto(),
                    "FOLIO_PAGO",
                    pago.getFolioPago() != null ? pago.getFolioPago()
                            : pago.getId().toString().substring(0, 8).toUpperCase(),
                    "MONTO", "$" + pago.getMonto().toString(),
                    "METODO_PAGO", pago.getMetodoPago().name(),
                    "FECHA_PAGO", java.time.LocalDate.now().toString(),
                    "SERVICIO", servicio != null ? servicio.getNombre() : "Consulta",
                    "SALDO_PENDIENTE", "$" + resumen.getSaldoPendiente().toString(),
                    "TELEFONO_CLINICA", sucursal != null ? sucursal.getTelefono() : "");

            String wppMsg = String.format(
                    "🦷 Hola %s, hemos recibido tu pago por $%s mediante %s. Saldo actual: $%s. ¡Gracias por tu preferencia!",
                    paciente.getNombreCompleto(), pago.getMonto(), pago.getMetodoPago().name(),
                    resumen.getSaldoPendiente());

            notificationService.notifyPaciente(paciente,
                    com.meyisoft.dental.system.enums.NotificationType.COMPROBANTE_PAGO, vars, wppMsg);
        } catch (Exception e) {
            log.error("Error al enviar comprobante de pago: {}", e.getMessage());
        }
    }

    private void checkAndFinalizeCita(UUID citaId, UUID tenantId) {
        // Sincronizar el campo denormalizado monto_pagado en la tabla citas
        sincronizarMontoPagadoCita(citaId, tenantId);

        CitaResumenFinancieroDTO resumen = obtenerResumenCita(citaId, tenantId);
        // Una cita se finaliza si está LIQUIDADA o si es una CORTESÍA (monto 0)
        if (resumen.getEstadoTicket() == TicketStatus.LIQUIDADO || resumen.getEstadoTicket() == TicketStatus.CORTESIA) {
            citaRepository.findById(citaId).ifPresent(cita -> {
                // Si la cuenta está cerrada, la cita debe marcarse como FINALIZADA
                // a menos que ya haya sido cancelada o rechazada.
                if (cita.getEstado() != AppointmentStatus.FINALIZADA &&
                        cita.getEstado() != AppointmentStatus.CANCELADA &&
                        cita.getEstado() != AppointmentStatus.RECHAZADA) {

                    cita.setEstado(AppointmentStatus.FINALIZADA);
                    citaRepository.save(cita);
                    log.info("Cita {} finalizada automáticamente (Estado previo: {})", citaId, cita.getEstado());
                }
            });
        }
    }

    private void sincronizarMontoPagadoCita(UUID citaId, UUID tenantId) {
        List<Pago> pagos = repository.findByCitaIdAndRegBorrado(citaId, 1);
        BigDecimal totalPagado = pagos.stream()
                .filter(p -> p.getStatus() == PagoStatus.APROBADO)
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        citaRepository.findById(citaId).ifPresent(cita -> {
            cita.setMontoPagado(totalPagado);
            citaRepository.save(cita);
            log.info("Cita {} sincronizada con monto pagado: {}", citaId, totalPagado);
        });
    }

    private PagoDTO mapToDTO(Pago entity) {
        return PagoDTO.builder()
                .id(entity.getId())
                .citaId(entity.getCitaId())
                .pacienteId(entity.getPacienteId())
                .monto(entity.getMonto())
                .metodoPago(entity.getMetodoPago())
                .notas(entity.getNotas())
                .folioPago(entity.getFolioPago())
                .comprobanteUrl(entity.getComprobanteUrl())
                .status(entity.getStatus())
                .motivoRechazo(entity.getMotivoRechazo())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
