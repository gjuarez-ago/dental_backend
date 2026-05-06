package com.meyisoft.dental.system.controller;

import com.meyisoft.dental.system.models.dto.PacienteDTO;
import com.meyisoft.dental.system.models.response.ApiResponse;
import com.meyisoft.dental.system.security.UserPrincipal;
import com.meyisoft.dental.system.service.PacienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pacientes")
@RequiredArgsConstructor
public class PacienteController {

    private final PacienteService pacienteService;

    @GetMapping
    public ApiResponse<List<PacienteDTO>> listarPriorizados(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(pacienteService.obtenerListadoPriorizado(principal.getTenantId()));
    }

    @PostMapping
    public ApiResponse<PacienteDTO> crear(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PacienteDTO dto) {
        return ApiResponse.success(pacienteService.crearPaciente(dto, principal.getTenantId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<PacienteDTO> obtenerPorId(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ApiResponse.success(pacienteService.obtenerPorId(id, principal.getTenantId()));
    }

    @PutMapping("/{id}")
    public ApiResponse<PacienteDTO> actualizar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody PacienteDTO dto) {
        return ApiResponse.success(pacienteService.actualizar(id, dto, principal.getTenantId()));
    }
}
