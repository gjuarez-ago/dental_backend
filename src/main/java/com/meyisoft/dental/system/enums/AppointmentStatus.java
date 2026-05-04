package com.meyisoft.dental.system.enums;

/**
 * Representa el ciclo de vida de una cita en el sistema dental.
 */
public enum AppointmentStatus {
    /** Una solicitud que llega desde la App o Web y requiere que la clínica asigne un doctor y confirme. */
    POR_CONFIRMAR("Solicitud en Espera", "Pendiente de validación y asignación de doctor", "#FFA500"),
    
    /** Cita programada formalmente en la agenda con doctor asignado. */
    CONFIRMADA("Agendada", "Cita confirmada en calendario", "#2196F3"),
    
    /** El paciente ha llegado a la clínica y está esperando su turno en recepción. */
    LLEGADA("En Sala de Espera", "Paciente presente en la sucursal", "#4CAF50"),
    
    /** El paciente está actualmente en el consultorio siendo atendido por el dentista. */
    EN_CONSULTA("En Atención", "Paciente en sillón odontológico", "#9C27B0"),
    
    /** El médico terminó el tratamiento, pero el paciente aún no ha pasado a pagar. */
    POR_LIQUIDAR("Pendiente de Pago", "Tratamiento terminado, esperando liquidación en caja", "#FF5722"),
    
    /** Todo el proceso médico y el cobro administrativo han sido concluidos. */
    FINALIZADA("Completada", "Cita concluida y pagada totalmente", "#757575"),
    
    /** La cita fue anulada por la clínica o el paciente antes de realizarse. */
    CANCELADA("Cancelada", "Cita que no se llevará a cabo", "#F44336"),

    /** La solicitud de cita no fue aceptada por la clínica (ej: horario no disponible). */
    RECHAZADA("Rechazada", "Solicitud no aceptada por la clínica", "#B71C1C"),
    
    /** El paciente no se presentó a su cita a la hora acordada y no canceló. */
    AUSENTE("Paciente no Asistió", "No se presentó a la cita sin aviso previo", "#212121");

    private final String nombreVisual;
    private final String descripcion;
    private final String colorHex;

    AppointmentStatus(String nombreVisual, String descripcion, String colorHex) {
        this.nombreVisual = nombreVisual;
        this.descripcion = descripcion;
        this.colorHex = colorHex;
    }

    public String getNombreVisual() { return nombreVisual; }
    public String getDescripcion() { return descripcion; }
    public String getColorHex() { return colorHex; }
}
