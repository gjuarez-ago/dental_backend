# 💰 Flujo de Negocio: Cobranza y Finanzas (PagoService)

Este documento detalla cómo el sistema gestiona el dinero, los saldos de los pacientes y la integridad de los cobros.

## 1. Registro de Pagos (Método `registrarPago`)
El registro de pago es flexible pero estricto en la integridad del saldo.

### ✅ Validaciones de Negocio (Sales & Ops)
*   **Actualización de Costo**: Se permite ajustar el precio total de la cita al momento de cobrar, pero el sistema prohíbe que el nuevo costo sea menor a lo que el paciente ya pagó (evita saldos a favor accidentales).
*   **No Saldo a Favor**: Por política de la clínica, no se permiten pagos que superen el saldo pendiente de la cita. El sistema bloquea montos excesivos.
*   **Estados de Pago**:
    *   **CRM (Caja)**: Los pagos entran como `APROBADO` de inmediato (Efectivo/Terminal).
    *   **APP (Web)**: Los anticipos o pagos web entran como `PENDIENTE_REVISION`. No afectan el saldo contable hasta que un administrador los valida con el comprobante adjunto.
*   **Deuda del Paciente**: El sistema descuenta automáticamente el pago del `saldo_pendiente` en el perfil global del paciente.

### 💻 Contexto Técnico (Developers)
*   **Cierre Automático**: Si el saldo de una cita llega a $0.00 y la cita estaba en estado `POR_LIQUIDAR`, el sistema la marca automáticamente como `FINALIZADA` (Método `checkAndFinalizeCita`).
*   **Notificaciones**: Al aprobar un pago, se dispara automáticamente el envío del comprobante digital por WhatsApp/Email al paciente (Método `enviarComprobantePago`).

---

## 2. Resumen Financiero y Estados de Ticket
El "Estado de Ticket" es un campo calculado que ayuda a ventas a identificar la situación de cobro de cada cita:

| Estado | Significado para Ventas | Lógica Técnica |
| :--- | :--- | :--- |
| **POR_DEFINIR** | El doctor no ha puesto precio a la cita. | `montoTotal` es NULL |
| **EN_REVISION** | Hay un pago web pendiente de validar. | Existe pago en `PENDIENTE_REVISION` |
| **CORTESIA** | Cita con costo $0.00. | `montoTotal` es 0 |
| **PENDIENTE** | No se ha recibido ningún pago. | `montoPagado` es 0 |
| **ABONADO** | Pago parcial recibido. | `montoPagado` < `montoTotal` |
| **LIQUIDADO** | Cita pagada al 100%. | `montoPagado` >= `montoTotal` |

---

## 3. Integridad SQL
Para evitar errores de redondeo o desincronización, el sistema utiliza `BigDecimal` en todos los cálculos financieros y realiza una **sincronización forzada** (`sincronizarMontoPagadoCita`) cada vez que un pago cambia de estado (Rechazado/Cancelado).
