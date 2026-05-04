package com.meyisoft.dental.system.repository;

import com.meyisoft.dental.system.enums.AppointmentStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Proyección para los procesos de CRON. 
 * Trae toda la información necesaria para notificaciones en un solo JOIN.
 */
public interface CitaCronProjection {
    UUID getCitaId();
    OffsetDateTime getFechaHora();
    String getFolio();
    
    // Datos del Paciente
    UUID getPacienteId();
    String getPacienteNombre();
    String getPacienteTelefono();
    String getPacienteEmail();
    
    // Datos del Doctor
    UUID getDoctorId();
    String getDoctorNombre();
    String getDoctorEmail();
    UUID getDoctorSucursalPrincipalId();
    
    // Datos del Servicio
    String getServicioNombre();
    
    // Datos de Sucursal
    String getSucursalNombre();
    String getSucursalTelefono();
}
