package com.meyisoft.dental.system.controller.auth;

import com.meyisoft.dental.system.models.request.LoginRequest;
import com.meyisoft.dental.system.models.request.PatientCheckRequest;
import com.meyisoft.dental.system.models.request.PatientCompleteProfileRequest;
import com.meyisoft.dental.system.models.request.PatientRegisterRequest;
import com.meyisoft.dental.system.models.response.AuthResponse;
import com.meyisoft.dental.system.models.response.PatientCheckResponse;
import com.meyisoft.dental.system.service.AuthCRMService;
import com.meyisoft.dental.system.service.PatientAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/patient-auth")
@RequiredArgsConstructor
public class PatientAuthController {

    private final PatientAuthService patientAuthService;
    private final AuthCRMService authCRMService;

    @PostMapping("/check")
    public ResponseEntity<PatientCheckResponse> checkPhone(@Valid @RequestBody PatientCheckRequest request) {
        return ResponseEntity.ok(patientAuthService.checkPatientPhone(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // Reutilizamos la lógica global de login (que ya incluye el fallback a paciente)
        return ResponseEntity.ok(authCRMService.loginCRM(request));
    }

    @PostMapping("/complete-profile")
    public ResponseEntity<AuthResponse> completeProfile(@Valid @RequestBody PatientCompleteProfileRequest request) {
        return ResponseEntity.ok(patientAuthService.completeProfile(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody PatientRegisterRequest request) {
        return ResponseEntity.ok(patientAuthService.register(request));
    }

    @PostMapping("/setup-access")
    public ResponseEntity<AuthResponse> setupAccess(@Valid @RequestBody com.meyisoft.dental.system.models.request.PatientSetupAccessRequest request) {
        return ResponseEntity.ok(patientAuthService.setupAccess(request));
    }
}
