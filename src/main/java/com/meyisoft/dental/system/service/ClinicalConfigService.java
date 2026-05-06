package com.meyisoft.dental.system.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meyisoft.dental.system.entity.Empresa;
import com.meyisoft.dental.system.entity.Sucursal;
import com.meyisoft.dental.system.entity.Usuario;
import com.meyisoft.dental.system.exception.BusinessException;
import com.meyisoft.dental.system.exception.ErrorCodes;
import com.meyisoft.dental.system.models.dto.ClinicalConfigDTO;
import com.meyisoft.dental.system.repository.SucursalRepository;
import com.meyisoft.dental.system.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClinicalConfigService {

    private final UsuarioRepository usuarioRepository;
    private final SucursalRepository sucursalRepository;
    private final com.meyisoft.dental.system.repository.EmpresaRepository empresaRepository;
    private final ObjectMapper objectMapper;

    /**
     * Obtiene la configuración clínica combinando datos de Usuario y su Sucursal
     * principal.
     */
    @Transactional(readOnly = true)
    public ClinicalConfigDTO getClinicalConfig(UUID userId, UUID sucursalId) {
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.USER_NOT_FOUND, ErrorCodes.MSG_USER_NOT_FOUND,
                        HttpStatus.NOT_FOUND));

        UUID targetSucursalId = (sucursalId != null) ? sucursalId : usuario.getSucursalIdPrincipal();

        if (targetSucursalId == null) {
            log.warn("No se especificó sucursal y el usuario {} no tiene una principal asignada", userId);
            return ClinicalConfigDTO.builder()
                    .nombreCompleto(usuario.getNombreCompleto())
                    .cedulaProfesional(usuario.getCedulaProfesional())
                    .horarios(new HashMap<>())
                    .build();
        }

        Sucursal sucursal = sucursalRepository.findById(targetSucursalId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.USER_NOT_FOUND, "Sucursal no encontrada",
                        HttpStatus.NOT_FOUND));

        Empresa empresa = empresaRepository.findById(usuario.getTenantId())
                .orElseThrow(() -> new BusinessException(ErrorCodes.USER_NOT_FOUND, "Empresa no encontrada",
                        HttpStatus.NOT_FOUND));

        Map<String, ClinicalConfigDTO.DayConfigDTO> horariosMap = parseHorarios(sucursal.getHorariosLaborales());

        return ClinicalConfigDTO.builder()
                .nombreCompleto(usuario.getNombreCompleto())
                .cedulaProfesional(usuario.getCedulaProfesional())
                .ventanaCancelacion(sucursal.getVentanaCancelacion())
                .horarios(horariosMap)
                .banco(sucursal.getBanco())
                .cuentaBancaria(sucursal.getCuentaBancaria())
                .clabeInterbancaria(sucursal.getClabeInterbancaria())
                .direccionSucursal(sucursal.getDireccion())
                .telefonoWhatsApp(empresa.getTelefonoWhatsApp())
                .horasAnticipacionCancelacion(empresa.getHorasAnticipacionCancelacion())
                .diasAnticipacionReserva(empresa.getDiasAnticipacionReserva())
                .isotypeUrl(empresa.getIsotypeUrl())
                .build();
    }

    /**
     * Actualiza la configuración clínica en ambas tablas.
     */
    @Transactional
    public void updateClinicalConfig(UUID userId, UUID sucursalId, ClinicalConfigDTO dto) {
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.USER_NOT_FOUND, ErrorCodes.MSG_USER_NOT_FOUND,
                        HttpStatus.NOT_FOUND));

        // 1. Actualizar Usuario (Identidad Profesional)
        usuario.setNombreCompleto(dto.getNombreCompleto());
        usuario.setCedulaProfesional(dto.getCedulaProfesional());
        usuarioRepository.save(usuario);

        // 2. Actualizar Sucursal (Horarios)
        UUID targetSucursalId = (sucursalId != null) ? sucursalId : usuario.getSucursalIdPrincipal();

        if (targetSucursalId != null) {
            Sucursal sucursal = sucursalRepository.findById(targetSucursalId)
                    .orElseThrow(
                            () -> new BusinessException(ErrorCodes.USER_NOT_FOUND, "Sucursal no encontrada",
                                    HttpStatus.NOT_FOUND));

            sucursal.setVentanaCancelacion(dto.getVentanaCancelacion());
            sucursal.setBanco(dto.getBanco());
            sucursal.setCuentaBancaria(dto.getCuentaBancaria());
            sucursal.setClabeInterbancaria(dto.getClabeInterbancaria());
            sucursal.setDireccion(dto.getDireccionSucursal());

            try {
                String horariosJson = objectMapper.writeValueAsString(dto.getHorarios());
                sucursal.setHorariosLaborales(horariosJson);
            } catch (JsonProcessingException e) {
                log.error("Error serializando horarios: {}", e.getMessage());
                throw new BusinessException("CONFIG-001", "Error al procesar el formato de horarios",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }

            sucursalRepository.save(sucursal);
        }

        // 3. Actualizar Empresa (Configuración Global)
        Empresa empresa = empresaRepository.findById(usuario.getTenantId())
                .orElseThrow(() -> new BusinessException(ErrorCodes.USER_NOT_FOUND, "Empresa no encontrada",
                        HttpStatus.NOT_FOUND));

        empresa.setTelefonoWhatsApp(dto.getTelefonoWhatsApp());
        empresa.setHorasAnticipacionCancelacion(dto.getHorasAnticipacionCancelacion());
        empresa.setDiasAnticipacionReserva(
                dto.getDiasAnticipacionReserva() != null ? dto.getDiasAnticipacionReserva() : 1);
        empresaRepository.save(empresa);
        log.info("Configuración clínica actualizada exitosamente para el usuario: {}", userId);
    }

    private Map<String, ClinicalConfigDTO.DayConfigDTO> parseHorarios(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, ClinicalConfigDTO.DayConfigDTO>>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Error parseando horarios JSON: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}
