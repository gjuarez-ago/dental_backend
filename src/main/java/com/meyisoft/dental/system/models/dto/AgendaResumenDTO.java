package com.meyisoft.dental.system.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de respuesta para el panel lateral de agenda.
 * Contiene el resumen de citas del periodo solicitado (HOY, SEMANA, MES)
 * junto con métricas útiles para el médico en su jornada.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgendaResumenDTO {

    /** Total de citas activas en el periodo solicitado (excluye canceladas/ausentes/rechazadas). */
    private long totalCitas;

    /** Citas que aún están pendientes (POR_CONFIRMAR o CONFIRMADA). */
    private long citasPendientes;

    /** Citas que ya están en proceso o finalizadas hoy (LLEGADA, EN_CONSULTA, POR_LIQUIDAR, FINALIZADA). */
    private long citasAtendidas;

    /** Citas canceladas, ausentes o rechazadas dentro del periodo. */
    private long citasCanceladas;

    /** Próxima cita activa (la más cercana en tiempo desde ahora). */
    private CitaDTO proximaCita;

    /** Lista de citas del periodo solicitado, ordenadas por fecha y hora ascendente. */
    private List<CitaDTO> citas;
}
