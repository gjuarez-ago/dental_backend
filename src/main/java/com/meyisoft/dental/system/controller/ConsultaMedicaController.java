package com.meyisoft.dental.system.controller;

import com.meyisoft.dental.system.models.dto.ConsultaMedicaDTO;
import com.meyisoft.dental.system.models.response.ApiResponse;
import com.meyisoft.dental.system.security.UserPrincipal;
import com.meyisoft.dental.system.service.ConsultaMedicaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Controller REST para la gestión de consultas médicas (ficha clínica).
 * Maneja el registro, consulta por cita e historial clínico del paciente.
 */
@RestController
@RequestMapping("/api/v1/consultas")
@RequiredArgsConstructor
@Tag(name = "Consultas Médicas", description = "Gestión de fichas clínicas y recetas")
public class ConsultaMedicaController {

    private final ConsultaMedicaService service;

    /**
     * POST /api/v1/consultas
     * Guarda o actualiza la consulta médica asociada a una cita.
     * Si ya existe una consulta para esa cita, se actualiza (upsert).
     */
    @PostMapping
    @Operation(summary = "Guardar o actualizar consulta médica de una cita")
    public ResponseEntity<ApiResponse<ConsultaMedicaDTO>> guardarConsulta(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ConsultaMedicaDTO dto) {

        // El servicio ya valida la existencia de la cita y asigna doctor/paciente
        ConsultaMedicaDTO result = service.guardarConsulta(dto);

        return ResponseEntity.ok(ApiResponse.<ConsultaMedicaDTO>builder()
                .ok(true)
                .result(result)
                .timestamp(OffsetDateTime.now())
                .build());
    }

    /**
     * GET /api/v1/consultas/cita/{citaId}
     * Obtiene la consulta médica asociada a una cita específica.
     * Retorna null en result si no existe (primera vez que se abre el drawer).
     */
    @GetMapping("/cita/{citaId}")
    @Operation(summary = "Obtener consulta médica por ID de cita")
    public ResponseEntity<ApiResponse<ConsultaMedicaDTO>> obtenerPorCita(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID citaId) {

        ConsultaMedicaDTO result = service.obtenerPorCita(citaId);

        return ResponseEntity.ok(ApiResponse.<ConsultaMedicaDTO>builder()
                .ok(true)
                .result(result)
                .timestamp(OffsetDateTime.now())
                .build());
    }

    /**
     * GET /api/v1/consultas/paciente/{pacienteId}
     * Obtiene el historial clínico completo de un paciente,
     * ordenado por fecha de creación descendente (más reciente primero).
     */
    @GetMapping("/paciente/{pacienteId}")
    @Operation(summary = "Obtener historial clínico de un paciente")
    public ResponseEntity<ApiResponse<List<ConsultaMedicaDTO>>> obtenerHistorial(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID pacienteId) {

        List<ConsultaMedicaDTO> result = service.obtenerHistorialPaciente(pacienteId);

        return ResponseEntity.ok(ApiResponse.<List<ConsultaMedicaDTO>>builder()
                .ok(true)
                .result(result)
                .timestamp(OffsetDateTime.now())
                .build());
    }
}
