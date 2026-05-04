package com.meyisoft.dental.system.service;

import com.meyisoft.dental.system.entity.ServicioDental;
import com.meyisoft.dental.system.exception.BusinessException;
import com.meyisoft.dental.system.exception.ErrorCodes;
import com.meyisoft.dental.system.config.AuditAction;
import com.meyisoft.dental.system.models.dto.ServicioDentalDTO;
import com.meyisoft.dental.system.repository.ServicioDentalRepository;
import com.meyisoft.dental.system.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServicioDentalService {

    private final ServicioDentalRepository repository;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public List<ServicioDentalDTO> listarServicios(UUID tenantId) {
        return repository.findByTenantIdAndRegBorrado(tenantId, 1).stream()
                .map(s -> mapToDTO(s))
                .collect(Collectors.toList());
    }

    @Transactional
    @AuditAction(modulo = "SERVICIOS", accion = "CREAR", descripcion = "Registro de nuevo servicio en el catálogo")
    public ServicioDentalDTO crear(ServicioDentalDTO dto, UUID tenantId) {
        ServicioDental entity = ServicioDental.builder()
                .id(UUID.randomUUID())
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .precioBase(dto.getPrecioBase())
                .duracionMinutos(dto.getDuracionMinutos())
                .colorEtiqueta(dto.getColorEtiqueta())
                .imagenUrl(dto.getImagenUrl())
                .requiereValoracion(dto.getRequiereValoracion() != null ? dto.getRequiereValoracion() : false)
                .giro(dto.getGiro() != null ? dto.getGiro() : "DENTAL")
                .especialidadRequerida(dto.getEspecialidadRequerida())
                .build();

        entity.setTenantId(tenantId);
        entity.setRegBorrado(1);

        return mapToDTO(repository.save(entity));
    }

    @Transactional
    @AuditAction(modulo = "SERVICIOS", accion = "ACTUALIZAR", descripcion = "Actualización de precio o datos de servicio")
    public ServicioDentalDTO actualizar(UUID id, ServicioDentalDTO dto, UUID tenantId) {
        ServicioDental entity = repository.findByIdAndTenantIdAndRegBorrado(id, tenantId, 1)
                .orElseThrow(() -> new BusinessException(ErrorCodes.USER_NOT_FOUND, "Servicio no encontrado",
                        HttpStatus.NOT_FOUND));

        entity.setNombre(dto.getNombre());
        entity.setDescripcion(dto.getDescripcion());
        entity.setPrecioBase(dto.getPrecioBase());
        entity.setDuracionMinutos(dto.getDuracionMinutos());
        entity.setColorEtiqueta(dto.getColorEtiqueta());

        // Manejo de cambio de imagen en Cloudflare R2
        if (dto.getImagenUrl() != null && !dto.getImagenUrl().equals(entity.getImagenUrl())) {
            // Si tenía una imagen previa en R2 (URL), intentamos borrarla
            if (entity.getImagenUrl() != null && entity.getImagenUrl().startsWith("http")) {
                try {
                    storageService.deleteFileByUrl(entity.getImagenUrl());
                } catch (Exception e) {
                    log.warn("No se pudo limpiar la imagen anterior de R2: {}", e.getMessage());
                }
            }
            entity.setImagenUrl(dto.getImagenUrl());
        }
        
        if (dto.getRequiereValoracion() != null) {
            entity.setRequiereValoracion(dto.getRequiereValoracion());
        }

        if (dto.getGiro() != null) {
            entity.setGiro(dto.getGiro());
        }

        if (dto.getEspecialidadRequerida() != null) {
            entity.setEspecialidadRequerida(dto.getEspecialidadRequerida());
        }

        return mapToDTO(repository.save(entity));
    }

    @Transactional
    @AuditAction(modulo = "SERVICIOS", accion = "ELIMINAR", descripcion = "Baja de servicio del catálogo clínico")
    public void eliminar(UUID id, UUID tenantId) {
        ServicioDental entity = repository.findByIdAndTenantIdAndRegBorrado(id, tenantId, 1)
                .orElseThrow(() -> new BusinessException(ErrorCodes.USER_NOT_FOUND, "Servicio no encontrado",
                        HttpStatus.NOT_FOUND));

        // Limpiar imagen de R2 si existe
        if (entity.getImagenUrl() != null && entity.getImagenUrl().startsWith("http")) {
            try {
                storageService.deleteFileByUrl(entity.getImagenUrl());
            } catch (Exception e) {
                log.warn("No se pudo eliminar la imagen de R2 al borrar servicio: {}", e.getMessage());
            }
        }

        entity.setRegBorrado(0); // Soft delete
        repository.save(entity);
    }

    private ServicioDentalDTO mapToDTO(ServicioDental entity) {
        return ServicioDentalDTO.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .descripcion(entity.getDescripcion())
                .precioBase(entity.getPrecioBase())
                .duracionMinutos(entity.getDuracionMinutos())
                .colorEtiqueta(entity.getColorEtiqueta())
                .imagenUrl(entity.getImagenUrl())
                .requiereValoracion(entity.getRequiereValoracion())
                .giro(entity.getGiro())
                .especialidadRequerida(entity.getEspecialidadRequerida())
                .build();
    }
}
