package com.meyisoft.dental.system.controller;

import com.meyisoft.dental.system.models.response.ApiResponse;
import com.meyisoft.dental.system.security.UserPrincipal;
import com.meyisoft.dental.system.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Gestión del perfil del usuario logueado")
public class ProfileController {

    private final ProfileService profileService;

    @Operation(summary = "Obtener perfil actual", description = "Retorna la información completa del usuario logueado")
    @GetMapping
    public ResponseEntity<ApiResponse<com.meyisoft.dental.system.models.dto.UsuarioDTO>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(profileService.getProfile(principal.getUserId())));
    }

    @Operation(summary = "Actualizar perfil", description = "Actualiza biografía, género, fecha de nacimiento y foto de perfil (con compresión automática)")
    @PostMapping(value = "/update", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<String>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "biografia", required = false) String biografia,
            @RequestParam(value = "genero", required = false) String genero,
            @RequestParam(value = "fechaNacimiento", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fechaNacimiento,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {

        String newPhotoUrl = profileService.updateProfile(principal.getUserId(), biografia, genero, fechaNacimiento, photo);
        return ResponseEntity.ok(ApiResponse.success(newPhotoUrl));
    }
}
