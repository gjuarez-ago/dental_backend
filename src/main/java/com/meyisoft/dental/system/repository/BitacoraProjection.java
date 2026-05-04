package com.meyisoft.dental.system.repository;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Proyección de interfaz para optimizar la consulta de Bitácora.
 * Evita el problema N+1 al traer el nombre del usuario directamente en el JOIN.
 */
public interface BitacoraProjection {
    UUID getId();
    UUID getUsuarioId();
    String getUsuarioNombre();
    String getModulo();
    String getAccion();
    String getDescripcion();
    UUID getEntidadRelacionadaId();
    OffsetDateTime getCreatedAt();
}
