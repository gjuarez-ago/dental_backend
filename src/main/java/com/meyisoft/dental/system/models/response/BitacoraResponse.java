package com.meyisoft.dental.system.models.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BitacoraResponse {
    private UUID id;
    private UUID usuarioId;
    private String usuarioNombre;
    private String modulo;
    private String accion;
    private String descripcion;
    private UUID entidadRelacionadaId;
    private OffsetDateTime createdAt;
}
