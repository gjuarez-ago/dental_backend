package com.meyisoft.dental.system.controller;

import com.meyisoft.dental.system.enums.AppointmentStatus;
import com.meyisoft.dental.system.models.dto.CitaDTO;
import com.meyisoft.dental.system.models.dto.DashboardStatsDTO;
import com.meyisoft.dental.system.models.response.ApiResponse;
import com.meyisoft.dental.system.security.UserPrincipal;
import com.meyisoft.dental.system.service.CitaService;
import com.meyisoft.dental.system.entity.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/citas")
@RequiredArgsConstructor
@Tag(name = "Citas", description = "Gestión de la agenda dental")
public class CitaController {

    private final CitaService service;

    @GetMapping
    @Operation(summary = "Listar citas por rango de fechas y sucursal")
    public ResponseEntity<ApiResponse<List<CitaDTO>>> listarCitas(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam UUID sucursalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end,
            @RequestParam(required = false) UUID doctorId) {
        
        List<CitaDTO> result = service.listarPorRango(principal.getTenantId(), sucursalId, start, end, doctorId);
        
        return ResponseEntity.ok(ApiResponse.<List<CitaDTO>>builder()
                .ok(true)
                .result(result)
                .timestamp(OffsetDateTime.now())
                .build());
    }

    @GetMapping("/dashboard-summary")
    @Operation(summary = "Obtener resumen para el dashboard con filtros opcionales")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> getDashboardSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID sucursalId,
            @RequestParam(required = false) UUID doctorId) {
        
        // Si no se envía sucursalId, usamos la principal del usuario
        UUID finalSucursalId = (sucursalId != null) ? sucursalId : principal.getSucursalId();
        
        DashboardStatsDTO result = service.getDashboardSummary(principal.getTenantId(), finalSucursalId, doctorId);
        
        return ResponseEntity.ok(ApiResponse.<DashboardStatsDTO>builder()
                .ok(true)
                .result(result)
                .timestamp(OffsetDateTime.now())
                .build());
    }

    @GetMapping("/doctores")
    @Operation(summary = "Listar doctores del tenant")
    public ResponseEntity<ApiResponse<List<Usuario>>> getDoctores(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID servicioId) {
        
        List<Usuario> result = service.listarDoctores(principal.getTenantId(), servicioId);
        
        return ResponseEntity.ok(ApiResponse.<List<Usuario>>builder()
                .ok(true)
                .result(result)
                .timestamp(OffsetDateTime.now())
                .build());
    }

    @PostMapping
    @Operation(summary = "Agendar una nueva cita")
    public ResponseEntity<ApiResponse<CitaDTO>> agendarCita(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CitaDTO dto) {
        
        CitaDTO result = service.agendar(dto, principal.getTenantId(), null);
        
        return ResponseEntity.ok(ApiResponse.<CitaDTO>builder()
                .ok(true)
                .result(result)
                .timestamp(OffsetDateTime.now())
                .build());
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Actualizar el estado de una cita (Confirmar, Llegada, Completada, etc.)")
    public ResponseEntity<ApiResponse<CitaDTO>> actualizarEstado(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam AppointmentStatus estado,
            @RequestParam(required = false) java.math.BigDecimal montoTotal) {
        
        CitaDTO result = service.actualizarEstado(id, estado, montoTotal, principal.getTenantId());
        
        return ResponseEntity.ok(ApiResponse.<CitaDTO>builder()
                .ok(true)
                .result(result)
                .timestamp(OffsetDateTime.now())
                .build());
    }

    @GetMapping("/por-confirmar")
    @Operation(summary = "Listar citas que requieren verificación (desde App/Web)")
    public ResponseEntity<ApiResponse<List<CitaDTO>>> listarPorConfirmar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID sucursalId) {
        
        List<CitaDTO> result = service.listarPorConfirmar(principal.getTenantId(), sucursalId);
        
        return ResponseEntity.ok(ApiResponse.<List<CitaDTO>>builder()
                .ok(true)
                .result(result)
                .timestamp(OffsetDateTime.now())
                .build());
    }

    @Operation(summary = "Confirmar una cita pendiente asignando/validando el doctor")
    @PatchMapping("/{id}/confirmar")
    public ResponseEntity<ApiResponse<CitaDTO>> confirmarCita(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam UUID doctorId,
            @RequestParam(required = false) java.math.BigDecimal montoTotal) {
        
        CitaDTO result = service.confirmarCita(id, doctorId, montoTotal, principal.getTenantId());
        
        return ResponseEntity.ok(ApiResponse.<CitaDTO>builder()
                .ok(true)
                .result(result)
                .timestamp(OffsetDateTime.now())
                .build());
    }

    @PatchMapping("/{id}/rechazar")
    @Operation(summary = "Rechazar una cita pendiente con motivo")
    public ResponseEntity<ApiResponse<CitaDTO>> rechazarCita(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam String motivo) {
        
        CitaDTO result = service.rechazarCita(id, motivo, principal.getTenantId());
        
        return ResponseEntity.ok(ApiResponse.<CitaDTO>builder()
                .ok(true)
                .result(result)
                .timestamp(OffsetDateTime.now())
                .build());
    }

    @PutMapping("/{id}/reprogramar")
    @Operation(summary = "Reprogramar fecha y hora de una cita")
    public ResponseEntity<ApiResponse<CitaDTO>> reprogramarCita(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime nuevaFechaHora,
            @RequestParam(required = false) Integer nuevaDuracion,
            @RequestParam(required = false) java.math.BigDecimal montoTotal) {
        
        CitaDTO result = service.reprogramar(id, nuevaFechaHora, nuevaDuracion, montoTotal, principal.getTenantId());
        
        return ResponseEntity.ok(ApiResponse.<CitaDTO>builder()
                .ok(true)
                .result(result)
                .timestamp(OffsetDateTime.now())
                .build());
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar una cita confirmada")
    public ResponseEntity<ApiResponse<CitaDTO>> cancelarCita(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam(required = false) String motivo,
            @RequestParam(defaultValue = "true") boolean reembolsar) {
        
        String finalMotivo = (motivo != null && !motivo.isBlank()) ? motivo : "Cancelación administrativa";
        CitaDTO result = service.cancelarCita(id, finalMotivo, principal.getTenantId(), reembolsar);
        
        return ResponseEntity.ok(ApiResponse.<CitaDTO>builder()
                .ok(true)
                .result(result)
                .timestamp(OffsetDateTime.now())
                .build());
    }

    @GetMapping("/agenda-resumen")
    @Operation(
        summary = "Resumen de agenda para el panel lateral",
        description = "Devuelve las citas del periodo indicado (HOY, SEMANA o MES) junto con métricas " +
                      "de estado (pendientes, atendidas, canceladas) y la próxima cita activa. " +
                      "Diseñado para alimentar el panel lateral del 30% en la vista de appointments."
    )
    public ResponseEntity<ApiResponse<com.meyisoft.dental.system.models.dto.AgendaResumenDTO>> getAgendaResumen(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID sucursalId,
            @RequestParam(required = false) UUID doctorId,
            @RequestParam(defaultValue = "HOY") String tipo) {

        // Si no se envía sucursalId, usamos la principal del usuario autenticado
        UUID finalSucursalId = (sucursalId != null) ? sucursalId : principal.getSucursalId();

        com.meyisoft.dental.system.models.dto.AgendaResumenDTO result =
                service.getAgendaResumen(principal.getTenantId(), finalSucursalId, doctorId, tipo);

        return ResponseEntity.ok(ApiResponse.<com.meyisoft.dental.system.models.dto.AgendaResumenDTO>builder()
                .ok(true)
                .result(result)
                .timestamp(OffsetDateTime.now())
                .build());
    }
}
