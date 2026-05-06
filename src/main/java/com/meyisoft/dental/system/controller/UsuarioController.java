package com.meyisoft.dental.system.controller;

import com.meyisoft.dental.system.models.request.UsuarioRequest;
import com.meyisoft.dental.system.models.response.ApiResponse;
import com.meyisoft.dental.system.models.response.UsuarioResponse;
import com.meyisoft.dental.system.security.UserPrincipal;
import com.meyisoft.dental.system.enums.UserRole;
import com.meyisoft.dental.system.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping("/sucursal/{sucursalId}")
    public ApiResponse<List<UsuarioResponse>> listarUsuariosPorSucursal(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sucursalId) {
        return ApiResponse.success(usuarioService.listarUsuariosPorSucursal(principal.getTenantId(), sucursalId));
    }

    @PostMapping
    public ApiResponse<UsuarioResponse> crear(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UsuarioRequest request) {
        UserRole callerRole = UserRole.valueOf(principal.getRole());
        return ApiResponse.success(usuarioService.crearUsuario(request, principal.getTenantId(), callerRole));
    }

    @PutMapping("/{id}")
    public ApiResponse<UsuarioResponse> actualizar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UsuarioRequest request) {
        return ApiResponse.success(usuarioService.actualizarUsuario(id, request, principal.getTenantId()));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<UsuarioResponse> cambiarEstado(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam boolean activo) {
        return ApiResponse.success(usuarioService.cambiarEstadoActivo(id, activo, principal.getTenantId()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> eliminar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        usuarioService.eliminarUsuario(id, principal.getTenantId());
        return ApiResponse.success(null);
    }
}
