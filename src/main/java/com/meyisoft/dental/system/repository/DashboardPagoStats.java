package com.meyisoft.dental.system.repository;

import java.math.BigDecimal;

/**
 * Proyección para consolidar estadísticas de pagos en una sola consulta.
 * Optimización crítica para el Dashboard en Cloud Run.
 */
public interface DashboardPagoStats {
    BigDecimal getIngresosHoy();
    BigDecimal getIngresosAyer();
    BigDecimal getIngresosPendientes();
    Long getCountPendientes();
}
