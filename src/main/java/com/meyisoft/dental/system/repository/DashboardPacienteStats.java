package com.meyisoft.dental.system.repository;

/**
 * Proyección para consolidar estadísticas de nuevos pacientes en una sola consulta.
 */
public interface DashboardPacienteStats {
    Long getPacientesHoy();
    Long getPacientesAyer();
}
