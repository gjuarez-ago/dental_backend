# 🛡️ Arquitectura de Seguridad y Datos (General)

Reglas transversales que aplican a todo el sistema MEYISOFT POS / Dental System.

## 1. Multi-Tenancy (Aislamiento de Clínicas)
El sistema es multi-clínica. Los datos de una clínica son invisibles para otra.
*   **Seguridad**: Todas las consultas (SELECT, UPDATE, DELETE) incluyen obligatoriamente el filtro `tenant_id`. 
*   **Impacto Negocio**: Garantiza la privacidad total de los datos de los pacientes entre diferentes sucursales o dueños.

## 2. Soft Delete (Eliminación Lógica)
En este sistema, **nada se borra permanentemente** de la base de datos por accidente.
*   **Regla**: El campo `reg_borrado` define la visibilidad.
    *   `1`: Registro Activo.
    *   `0`: Registro Eliminado (Oculto).
*   **Recuperación**: En caso de error humano, un administrador de base de datos puede restaurar registros simplemente cambiando el `0` por `1`.

## 3. Manejo de Concurrencia (Race Conditions)
Diseñado para entornos de alta demanda.
*   **Pessimistic Locking**: Cuando se genera un folio (Cita o Pago), el sistema "congela" la fila de contadores para esa clínica específica. Esto evita que dos recepcionistas obtengan el mismo número de folio al agendar citas simultáneamente.
*   **Atomicidad**: Las operaciones financieras (Pago + Actualización de Saldo Paciente) se ejecutan dentro de una `@Transactional`. O se guardan ambos cambios, o no se guarda ninguno, evitando que el dinero "desaparezca" en el limbo del sistema.

## 4. Auditoría
Cada cambio significativo (creación, edición, eliminación) guarda automáticamente:
*   Quién realizó el cambio (Usuario).
*   Cuándo se realizó (Fecha/Hora con zona horaria).
*   Desde qué sucursal.
Esto permite auditorías forenses en caso de discrepancias de dinero o agenda.
