package com.meyisoft.dental.system.controller;

import com.meyisoft.dental.system.models.dto.AgendaPublicaDTO;
import com.meyisoft.dental.system.models.dto.EspecialistaCardDTO;
import com.meyisoft.dental.system.models.dto.GiroOptionDTO;
import com.meyisoft.dental.system.models.request.BookingPublicMarketplaceRequest;
import com.meyisoft.dental.system.models.response.ApiResponse;
import com.meyisoft.dental.system.models.response.PageResult;
import com.meyisoft.dental.system.security.UserPrincipal;
import com.meyisoft.dental.system.service.MarketplaceSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Tag(name = "Marketplace", description = "Búsqueda pública de especialistas y agenda")
public class PublicSearchController {

    private final MarketplaceSearchService searchService;

    // ── GET /api/v1/public/search/especialistas ───────────────────────────────

    @GetMapping("/search/especialistas")
    @Operation(summary = "Buscar especialistas por estado, municipio, giro y texto")
    public ResponseEntity<ApiResponse<PageResult<EspecialistaCardDTO>>> searchEspecialistas(
            @RequestParam(required = false) String estadoId,
            @RequestParam(required = false) String municipioId,
            @RequestParam(required = false) String giro,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String modalidad,
            @RequestParam(defaultValue = "CALIFICACION") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResult<EspecialistaCardDTO> result = searchService.searchEspecialistas(
                estadoId, municipioId, giro, q, modalidad, sort,
                Math.max(0, page),
                Math.min(50, Math.max(1, size)));

        return ResponseEntity.ok(ApiResponse.<PageResult<EspecialistaCardDTO>>builder()
                .ok(true)
                .result(result)
                .timestamp(OffsetDateTime.now())
                .build());
    }

    // ── GET /api/v1/public/search/giros ──────────────────────────────────────

    @GetMapping("/search/giros")
    @Operation(summary = "Listar giros/especialidades disponibles con conteo")
    public ResponseEntity<ApiResponse<List<GiroOptionDTO>>> getGiros(
            @RequestParam(required = false) String q) {

        List<GiroOptionDTO> result = searchService.getGiros(q);

        return ResponseEntity.ok(ApiResponse.<List<GiroOptionDTO>>builder()
                .ok(true)
                .result(result)
                .timestamp(OffsetDateTime.now())
                .build());
    }

    // ── GET /api/v1/public/especialistas/{tenantId}/agenda ────────────────────

    @GetMapping("/especialistas/{tenantId}/agenda")
    @Operation(summary = "Obtener agenda pública de un especialista (N días desde fechaInicio)")
    public ResponseEntity<ApiResponse<AgendaPublicaDTO>> getAgenda(
            @PathVariable UUID tenantId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(defaultValue = "14") int dias) {

        LocalDate inicio = fechaInicio != null ? fechaInicio : LocalDate.now();
        int diasNorm = Math.min(60, Math.max(1, dias));

        AgendaPublicaDTO result = searchService.getAgendaPublica(tenantId, inicio, diasNorm);

        return ResponseEntity.ok(ApiResponse.<AgendaPublicaDTO>builder()
                .ok(true)
                .result(result)
                .timestamp(OffsetDateTime.now())
                .build());
    }

    // ── POST /api/v1/public/especialistas/citas/solicitar ────────────────────

    @PostMapping("/especialistas/citas/solicitar")
    @Operation(summary = "Solicitar cita con un especialista desde el marketplace")
    public ResponseEntity<ApiResponse<Map<String, String>>> solicitarCita(
            @Valid @RequestBody BookingPublicMarketplaceRequest request) {

        // Si el paciente está autenticado (JWT con rol PACIENTE), forzamos la
        // identidad desde el token para evitar suplantación. El nombre del body
        // se respeta sólo como display; el matching del paciente es por teléfono.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up
                && "PACIENTE".equalsIgnoreCase(up.getRole())) {
            if (up.getTelefono() != null && !up.getTelefono().isBlank()) {
                request.setTelefonoPaciente(up.getTelefono());
            }
            if ((request.getEmailPaciente() == null || request.getEmailPaciente().isBlank())
                    && up.getEmail() != null && !up.getEmail().isBlank()) {
                request.setEmailPaciente(up.getEmail());
            }
        }

        Map<String, String> result = searchService.solicitarCita(request);

        return ResponseEntity.status(201).body(ApiResponse.<Map<String, String>>builder()
                .ok(true)
                .result(result)
                .timestamp(OffsetDateTime.now())
                .build());
    }
}
