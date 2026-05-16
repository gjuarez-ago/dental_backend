package com.meyisoft.dental.system.controller.auth;

import com.meyisoft.dental.system.models.request.LoginRequest;
import com.meyisoft.dental.system.models.request.RegisterTenantRequest;
import com.meyisoft.dental.system.models.request.ForgotPasswordRequest;
import com.meyisoft.dental.system.models.request.ResetPasswordRequest;
import com.meyisoft.dental.system.models.response.ApiResponse;
import com.meyisoft.dental.system.models.response.AuthResponse;
import com.meyisoft.dental.system.service.AuthCRMService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthCRMService authCRMService;

    @Operation(
            summary = "Login CRM (solo médicos/personal)",
            description = "Autenticación exclusiva para usuarios del CRM (staff). Los pacientes deben usar /api/v1/public/patient-auth/login.")
    @PostMapping("/crm/login")
    public ResponseEntity<AuthResponse> loginCRM(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authCRMService.loginCRM(request));
    }

    @PostMapping("/crm/register")
    public ResponseEntity<AuthResponse> registerTenant(@Valid @RequestBody RegisterTenantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authCRMService.registerTenant(request));
    }

    @PostMapping("/crm/check-email")
    public ResponseEntity<EmailCheckResponse> checkEmail(@Valid @RequestBody EmailCheckRequest request) {
        AuthCRMService.EmailCheckStatus status = authCRMService.checkEmail(request.getEmail());
        return ResponseEntity.ok(new EmailCheckResponse(status, statusMessage(status)));
    }

    @PostMapping("/crm/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authCRMService.forgotPassword(request.getEmail());
        // Siempre retornamos OK para no revelar si el correo existe o no (medida de seguridad)
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .ok(true)
                .userMessage("Si el correo existe, se enviarán las instrucciones para restablecer la contraseña.")
                .build());
    }

    @PostMapping("/crm/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authCRMService.resetPassword(request.getToken(), request.getNewNip());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .ok(true)
                .userMessage("Contraseña restablecida exitosamente.")
                .build());
    }

    private static String statusMessage(AuthCRMService.EmailCheckStatus status) {
        return switch (status) {
            case STAFF_FOUND -> "Este correo ya está registrado como cuenta de personal. Inicia sesión.";
            case PATIENT_FOUND -> "Este correo está registrado como paciente. Para crear una cuenta de profesional usa otro correo.";
            case NOT_FOUND -> "Disponible. Continúa con el registro.";
        };
    }

    @Data
    public static class EmailCheckRequest {
        @NotBlank
        @Email
        private String email;
    }

    @Data
    public static class EmailCheckResponse {
        private final AuthCRMService.EmailCheckStatus status;
        private final String message;
    }
}
