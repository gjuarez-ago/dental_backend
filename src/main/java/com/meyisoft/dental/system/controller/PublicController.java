package com.meyisoft.dental.system.controller;

import com.meyisoft.dental.system.models.dto.CitaDTO;
import com.meyisoft.dental.system.models.dto.DisponibilidadDiaDTO;
import com.meyisoft.dental.system.models.dto.SlotDisponibilidadDTO;
import com.meyisoft.dental.system.models.dto.ServicioDentalDTO;
import com.meyisoft.dental.system.models.response.ApiResponse;
import com.meyisoft.dental.system.service.CitaService;
import com.meyisoft.dental.system.service.ServicioDentalService;
import com.meyisoft.dental.system.service.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Tag(name = "Public", description = "Endpoints públicos para landing page y App móvil")
public class PublicController {

        private final ServicioDentalService servicioDentalService;
        private final CitaService citaService;
        private final com.meyisoft.dental.system.repository.SucursalRepository sucursalRepository;
        private final com.meyisoft.dental.system.repository.UsuarioRepository usuarioRepository;
        private final StorageService storageService;
        private final ObjectMapper objectMapper;

        @GetMapping("/agenda/clinic-info")
        @Operation(summary = "Obtener información de la clínica y datos bancarios (Público)")
        public ResponseEntity<ApiResponse<com.meyisoft.dental.system.models.dto.ClinicInfoDTO>> getClinicInfo(
                        @RequestParam UUID tenantId,
                        @RequestParam UUID sucursalId) {

                // Buscamos la sucursal para obtener sus datos bancarios específicos
                com.meyisoft.dental.system.entity.Sucursal sucursal = sucursalRepository.findById(sucursalId)
                                .filter(s -> s.getTenantId().equals(tenantId) && s.getRegBorrado() == 1)
                                .orElseThrow(() -> new com.meyisoft.dental.system.exception.BusinessException(
                                                "NOT_FOUND",
                                                "Sucursal no encontrada",
                                                org.springframework.http.HttpStatus.NOT_FOUND));

                // Buscamos al OWNER del tenant para el nombre profesional/clínica (fallback o
                // base)
                com.meyisoft.dental.system.entity.Usuario owner = usuarioRepository.findByTenantIdAndRolAndRegBorrado(
                                tenantId, com.meyisoft.dental.system.enums.UserRole.OWNER, 1)
                                .stream().findFirst()
                                .orElseThrow(() -> new com.meyisoft.dental.system.exception.BusinessException(
                                                "NOT_FOUND",
                                                "No se encontró el propietario del tenant",
                                                org.springframework.http.HttpStatus.NOT_FOUND));

                com.meyisoft.dental.system.models.dto.ClinicInfoDTO info = com.meyisoft.dental.system.models.dto.ClinicInfoDTO
                                .builder()
                                .clinicName("Clínica Dental")
                                .doctorName(owner.getNombreCompleto())
                                .banco(sucursal.getBanco())
                                .cuentaBancaria(sucursal.getCuentaBancaria())
                                .clabeInterbancaria(sucursal.getClabeInterbancaria())
                                .depositPercentage(0.30)
                                .build();

                return ResponseEntity.ok(ApiResponse.<com.meyisoft.dental.system.models.dto.ClinicInfoDTO>builder()
                                .ok(true)
                                .result(info)
                                .timestamp(OffsetDateTime.now())
                                .build());
        }

        @GetMapping("/servicios")
        @Operation(summary = "Listar servicios de una empresa (Tenant) para la landing page")
        public ResponseEntity<ApiResponse<List<ServicioDentalDTO>>> listarServicios(@RequestParam UUID tenantId) {

                List<ServicioDentalDTO> result = servicioDentalService.listarServicios(tenantId);

                return ResponseEntity.ok(ApiResponse.<List<ServicioDentalDTO>>builder()
                                .ok(true)
                                .result(result)
                                .timestamp(OffsetDateTime.now())
                                .build());
        }

        @GetMapping("/agenda/disponibilidad-mes")
        @Operation(summary = "Obtener calendario mensual con disponibilidad (multi-doctor)")
        public ResponseEntity<ApiResponse<List<DisponibilidadDiaDTO>>> getDisponibilidadMes(
                        @RequestParam UUID tenantId,
                        @RequestParam UUID sucursalId,
                        @RequestParam int mes,
                        @RequestParam int anio,
                        @RequestParam(required = false) UUID servicioId) {

                List<DisponibilidadDiaDTO> result = citaService.obtenerDisponibilidadMes(tenantId, sucursalId, mes,
                                anio, servicioId);

                return ResponseEntity.ok(ApiResponse.<List<DisponibilidadDiaDTO>>builder()
                                .ok(true)
                                .result(result)
                                .timestamp(OffsetDateTime.now())
                                .build());
        }

        @GetMapping("/agenda/slots-disponibles")
        @Operation(summary = "Obtener slots horarios disponibles para una fecha y servicio específico")
        public ResponseEntity<ApiResponse<List<SlotDisponibilidadDTO>>> getSlotsDisponibles(
                        @RequestParam UUID tenantId,
                        @RequestParam UUID sucursalId,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                        @RequestParam UUID servicioId) {

                List<SlotDisponibilidadDTO> result = citaService.obtenerSlotsDisponibles(tenantId, sucursalId, fecha,
                                servicioId);

                return ResponseEntity.ok(ApiResponse.<List<SlotDisponibilidadDTO>>builder()
                                .ok(true)
                                .result(result)
                                .timestamp(OffsetDateTime.now())
                                .build());
        }

        @PostMapping(value = "/agenda/agendar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @Operation(summary = "Agendar una nueva cita desde la landing page con comprobante obligatorio")
        public ResponseEntity<ApiResponse<CitaDTO>> agendarCita(
                        @RequestPart("cita") String citaJson,
                        @RequestPart("file") MultipartFile file) throws Exception {

                // 1. Deserializar DTO
                CitaDTO dto = objectMapper.readValue(citaJson, CitaDTO.class);

                // 2. Validar archivo obligatorio
                if (file == null || file.isEmpty()) {
                        throw new com.meyisoft.dental.system.exception.BusinessException("MISSING_FILE",
                                        "El comprobante de pago es obligatorio para agendar públicamente",
                                        org.springframework.http.HttpStatus.BAD_REQUEST);
                }

                // 3. Subir archivo a R2
                String comprobanteUrl = storageService.uploadFile(file, "comprobantes");

                // 4. Obtener TenantId desde la sucursal para aislamiento
                com.meyisoft.dental.system.entity.Sucursal sucursal = sucursalRepository.findById(dto.getSucursalId())
                                .orElseThrow(() -> new com.meyisoft.dental.system.exception.BusinessException(
                                                "NOT_FOUND",
                                                "Sucursal no encontrada",
                                                org.springframework.http.HttpStatus.NOT_FOUND));

                dto.setSource("PUBLIC");

                // 5. Agendar
                CitaDTO result = citaService.agendar(dto, sucursal.getTenantId(), comprobanteUrl);

                return ResponseEntity.status(201).body(ApiResponse.<CitaDTO>builder()
                                .ok(true)
                                .result(result)
                                .timestamp(OffsetDateTime.now())
                                .build());
        }
}
