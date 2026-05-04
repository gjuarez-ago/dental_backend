package com.meyisoft.dental.system.controller;

import com.meyisoft.dental.system.models.dto.CitaDTO;
import com.meyisoft.dental.system.models.dto.CitaPatientDTO;
import com.meyisoft.dental.system.models.dto.TimelineEntryDTO;
import com.meyisoft.dental.system.models.request.PatientBookRequest;
import com.meyisoft.dental.system.models.request.ProfileSetupRequest;
import com.meyisoft.dental.system.models.response.ApiResponse;
import com.meyisoft.dental.system.security.UserPrincipal;
import com.meyisoft.dental.system.service.PatientPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patient")
@RequiredArgsConstructor
@Tag(name = "Portal del Paciente", description = "Endpoints globales para pacientes")
@PreAuthorize("hasRole('PACIENTE')")
public class PatientPortalController {

    private final PatientPortalService service;

    @GetMapping("/my-appointments")
    @Operation(summary = "Listar todas las citas del paciente en la red")
    public ResponseEntity<ApiResponse<List<CitaPatientDTO>>> getMyAppointments(
            @AuthenticationPrincipal UserPrincipal principal) {
        
        List<CitaPatientDTO> result = service.getMyAppointments(principal.getTelefono(), principal.getEmail());
        
        return ResponseEntity.ok(ApiResponse.<List<CitaPatientDTO>>builder()
                .ok(true)
                .result(result)
                .timestamp(OffsetDateTime.now())
                .build());
    }

    @GetMapping("/medical-history")
    @Operation(summary = "Obtener el historial clínico detallado (línea de tiempo) del paciente")
    public ResponseEntity<ApiResponse<List<TimelineEntryDTO>>> getMedicalHistory(
            @AuthenticationPrincipal UserPrincipal principal) {
        
        List<TimelineEntryDTO> result = service.getMedicalHistory(principal.getUserId());
        
        return ResponseEntity.ok(ApiResponse.<List<TimelineEntryDTO>>builder()
                .ok(true)
                .result(result)
                .timestamp(OffsetDateTime.now())
                .build());
    }

    @PatchMapping("/appointments/{id}/cancel")
    @Operation(summary = "Cancelar una cita desde el portal del paciente")
    public ResponseEntity<ApiResponse<String>> cancelAppointment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody String motivo) {
        
        service.cancelarCitaDesdePortal(id, motivo);
        
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .ok(true)
                .result("Cita cancelada correctamente")
                .timestamp(OffsetDateTime.now())
                .build());
    }

    @PostMapping("/setup-profile")
    @Operation(summary = "Configurar perfil inicial (PIN y Email)")
    public ResponseEntity<ApiResponse<String>> setupProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ProfileSetupRequest request) {
        
        service.setupProfile(principal.getUserId(), request.getNewPin(), request.getEmail());
        
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .ok(true)
                .result("Perfil configurado correctamente")
                .timestamp(OffsetDateTime.now())
                .build());
    }

    @GetMapping("/my-clinic")
    @Operation(summary = "Obtener tenantId y sucursalId del paciente para consultas públicas")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> getMyClinic(
            @AuthenticationPrincipal UserPrincipal principal) {

        var info = service.getPatientClinicInfo(principal.getUserId());

        return ResponseEntity.ok(ApiResponse.<java.util.Map<String, String>>builder()
                .ok(true)
                .result(info)
                .timestamp(OffsetDateTime.now())
                .build());
    }

    @PostMapping(value = "/book", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Agendar una cita desde el portal del paciente con ticket obligatorio")
    public ResponseEntity<ApiResponse<CitaDTO>> bookAppointment(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart("cita") String citaJson,
            @RequestPart("file") org.springframework.web.multipart.MultipartFile file) throws Exception {
        
        // Usar ObjectMapper con soporte para Java 8 dates (OffsetDateTime)
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                
        PatientBookRequest request = mapper.readValue(citaJson, PatientBookRequest.class);

        CitaDTO result = service.bookAppointmentFromPortalWithFile(principal.getUserId(), request, file);
        
        return ResponseEntity.status(201).body(ApiResponse.<CitaDTO>builder()
                .ok(true)
                .result(result)
                .timestamp(OffsetDateTime.now())
                .build());
    }
}

