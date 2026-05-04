# 🦷 Flujo de Negocio: Gestión de Citas (CitaService)

Este documento describe las reglas de negocio para agendar, reprogramar y gestionar citas médicas en el sistema dental.

## 1. Reglas de Agendamiento (Método `agendar`)
El proceso de agendar una cita es el corazón del sistema y cuenta con validaciones críticas para evitar errores humanos y conflictos operativos.

### ✅ Validaciones de Negocio (Sales & Ops)
*   **Servicio Obligatorio**: No se puede agendar sin un servicio dental válido. El sistema toma el costo y la duración base del catálogo automáticamente.
*   **Horario de Sucursal**: El sistema impide agendar citas fuera del horario de operación de la clínica (validación por día de la semana).
*   **No al Traslape (Double Booking)**: 
    *   **Consultorio**: No pueden existir dos citas al mismo tiempo en la misma sucursal.
    *   **Doctor**: Un doctor no puede estar en dos citas simultáneas. El sistema bloquea el horario si el doctor ya tiene agenda.
    *   **Paciente**: Un paciente no puede agendar dos citas que choquen entre sí.
*   **Perfil del Doctor**: Solo se pueden asignar citas a doctores con perfil **ACTIVO**. Si el doctor fue desactivado por administración, el sistema lo bloquea para evitar errores de asignación.
*   **Costo de Transparencia**: No se permite registrar citas con costo $0.00 si el servicio tiene un precio base. Para cortesías, se registra el costo real y se marca como cortesía en el pago para fines de reportes de inversión.

### 💻 Contexto Técnico (Developers)
*   **Folios (Pessimistic Locking)**: Se usa bloqueo pesado en la tabla `registro_folios` para asegurar que el correlativo `CIT-YYYYMMDD-000x` sea único y no se duplique en condiciones de alta concurrencia.
*   **Manejo de Pacientes**: Si el origen es `APP/PUBLIC`, el sistema busca al paciente por teléfono; si no existe, lo crea automáticamente ("Shadow Patient") para agilizar el flujo.
*   **Auditoría**: Cada agendamiento dispara una anotación `@AuditAction` para trazabilidad total.

---

## 2. Ordenamiento de Agenda (Método `getStatusPriority`)
Para que la clínica opere eficientemente, la lista de citas no es solo cronológica, se prioriza por estado dinámico:
1.  **EN_CONSULTA**: Paciente atendido actualmente (Máxima prioridad).
2.  **LLEGADA**: Paciente ya en sala de espera.
3.  **CONFIRMADA**: Citas listas para atender.
4.  **POR_CONFIRMAR**: Citas web pendientes de validación.
... (Siguen Finalizadas, Ausentes y Canceladas al final).

---

## 3. Optimización de Datos (N+1)
Para reportes y listas, el sistema utiliza **Manual Joins** en lugar de relaciones pesadas de JPA. Esto permite obtener nombres de pacientes, doctores y servicios en una sola consulta SQL, reduciendo la carga del servidor en un 70%.
