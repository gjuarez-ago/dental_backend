package com.meyisoft.dental.system.controller;

import com.meyisoft.dental.system.models.request.OnboardingRequest;
import com.meyisoft.dental.system.models.response.ApiResponse;
import com.meyisoft.dental.system.service.OnboardingService;
import com.meyisoft.dental.system.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
@Tag(name = "Onboarding", description = "API para configurar el consultorio después del registro")
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final StorageService storageService;

    @Operation(summary = "Completar Onboarding", description = "Recibe la configuración inicial de la clínica, horarios, y servicio")
    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<String>> completeOnboarding(
            @AuthenticationPrincipal com.meyisoft.dental.system.security.UserPrincipal principal,
            @Valid @RequestBody OnboardingRequest request) {
        
        onboardingService.processOnboarding(principal.getUserId(), request);

        return ResponseEntity.ok(ApiResponse.success("Onboarding completado exitosamente"));
    }

    @Operation(summary = "Subir foto de perfil", description = "Sube una imagen a Cloudflare R2")
    @PostMapping("/upload-photo")
    public ResponseEntity<ApiResponse<String>> uploadPhoto(
            @RequestParam("file") MultipartFile file) {
        
        String url = storageService.uploadFile(file, "avatars");
        
        return ResponseEntity.ok(ApiResponse.success(url));
    }
}
