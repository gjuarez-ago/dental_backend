package com.meyisoft.dental.system.service;

import com.meyisoft.dental.system.entity.Bitacora;
import com.meyisoft.dental.system.models.response.BitacoraResponse;
import com.meyisoft.dental.system.repository.BitacoraProjection;
import com.meyisoft.dental.system.repository.BitacoraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BitacoraService {

    private final BitacoraRepository repository;

    @Async
    @Transactional
    public void registrar(UUID tenantId, UUID usuarioId, String modulo, String accion, String descripcion, UUID entidadId) {
        try {
            Bitacora logEntry = Bitacora.builder()
                    .id(UUID.randomUUID())
                    .tenantId(tenantId)
                    .usuarioId(usuarioId)
                    .modulo(modulo)
                    .accion(accion)
                    .descripcion(descripcion)
                    .entidadRelacionadaId(entidadId)
                    .createdAt(OffsetDateTime.now(com.meyisoft.dental.system.utils.DateUtils.MEXICO_OFFSET))
                    .build();
            repository.save(logEntry);
        } catch (Exception e) {
            log.error("❌ Error asíncrono al registrar en bitácora: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<BitacoraResponse> listar(UUID tenantId, int page, int size) {
        return repository.findProjectedByTenantId(tenantId, PageRequest.of(page, size))
                .map(this::mapProjectionToResponse);
    }

    private BitacoraResponse mapProjectionToResponse(BitacoraProjection p) {
        return BitacoraResponse.builder()
                .id(p.getId())
                .usuarioId(p.getUsuarioId())
                .usuarioNombre(p.getUsuarioNombre() != null ? p.getUsuarioNombre() : "Usuario Desconocido")
                .modulo(p.getModulo())
                .accion(p.getAccion())
                .descripcion(p.getDescripcion())
                .entidadRelacionadaId(p.getEntidadRelacionadaId())
                .createdAt(p.getCreatedAt())
                .build();
    }


}
