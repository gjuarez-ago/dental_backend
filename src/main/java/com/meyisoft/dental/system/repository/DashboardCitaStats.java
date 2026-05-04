package com.meyisoft.dental.system.repository;

/**
 * Proyección para consolidar estadísticas de citas en una sola consulta.
 * Optimización para el Dashboard.
 */
public interface DashboardCitaStats {
    Long getCitasHoy();
    Long getCitasAyer();
}
