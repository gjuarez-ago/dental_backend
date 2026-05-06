package com.meyisoft.dental.system.controller;

import com.meyisoft.dental.system.models.dto.ServicioDentalDTO;
import com.meyisoft.dental.system.models.response.ApiResponse;
import com.meyisoft.dental.system.security.UserPrincipal;
import com.meyisoft.dental.system.service.ServicioDentalService;
import com.meyisoft.dental.system.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/servicios")
@RequiredArgsConstructor
@Tag(name = "Servicios Dentales", description = "Endpoints para la gestión del catálogo de servicios")
public class ServicioDentalController {

    private final ServicioDentalService service;
    private final StorageService storageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir imagen de servicio a Cloudflare R2")
    public ResponseEntity<ApiResponse<String>> uploadImage(
            @RequestParam("file") MultipartFile file) {
        
        String url = storageService.uploadFile(file, "servicios");
        
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .ok(true)
                .result(url)
                .timestamp(OffsetDateTime.now())
                .build());
    }

    @GetMapping
    @Operation(summary = "Listar todos los servicios del tenant")
    public ResponseEntity<ApiResponse<List<ServicioDentalDTO>>> listar(
            @AuthenticationPrincipal UserPrincipal principal) {
        
        List<ServicioDentalDTO> listado = service.listarServicios(principal.getTenantId());
        
        return ResponseEntity.ok(ApiResponse.<List<ServicioDentalDTO>>builder()
                .ok(true)
                .result(listado)
                .timestamp(OffsetDateTime.now())
                .build());
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo servicio")
    public ResponseEntity<ApiResponse<ServicioDentalDTO>> crear(
            @Valid @RequestBody ServicioDentalDTO dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        ServicioDentalDTO creado = service.crear(dto, principal.getTenantId());
        
        return ResponseEntity.status(201).body(ApiResponse.<ServicioDentalDTO>builder()
                .ok(true)
                .result(creado)
                .timestamp(OffsetDateTime.now())
                .build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un servicio existente")
    public ResponseEntity<ApiResponse<ServicioDentalDTO>> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ServicioDentalDTO dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        ServicioDentalDTO actualizado = service.actualizar(id, dto, principal.getTenantId());
        
        return ResponseEntity.ok(ApiResponse.<ServicioDentalDTO>builder()
                .ok(true)
                .result(actualizado)
                .timestamp(OffsetDateTime.now())
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un servicio (Soft delete)")
    public ResponseEntity<ApiResponse<Void>> eliminar(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        service.eliminar(id, principal.getTenantId());
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .ok(true)
                .timestamp(OffsetDateTime.now())
                .build());
    }
}
