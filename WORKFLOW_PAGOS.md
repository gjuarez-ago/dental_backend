# 🦷 Workflow de Pagos y Cortesías - MEYISOFT POS

Este documento detalla el flujo operativo y técnico para el manejo de cobros, abonos y cortesías en el sistema dental.

---

## 1. El Ciclo Financiero de la Cita

### 1.1. Inicialización del Costo
Cuando una cita se agenda (desde App o CRM), el sistema inicializa el campo `montoTotal` basándose en el precio base del servicio dental seleccionado. 
- **Regla:** El monto es persistido en la tabla `citas` para asegurar que futuros cambios en el catálogo no afecten citas ya agendadas.

### 1.2. El Panel de Cobro (Caja)
El panel de cobro es el punto final de la operación clínica. Su función es registrar ingresos, no definir precios.

#### Estados del Ticket (`TicketStatus`):
| Estado | Condición | Comportamiento en UI |
| :--- | :--- | :--- |
| **POR_DEFINIR** | Monto total es NULL | No permite cobrar hasta definir precio. |
| **PENDIENTE** | Saldo pagado = 0 | Muestra botón de cobro completo. |
| **ABONADO** | 0 < Pagado < Total | Muestra saldo restante para liquidar. |
| **LIQUIDADO** | Pagado >= Total | **Ticket Cerrado.** Bloquea nuevos cobros. |
| **CORTESIA** | Total = 0 | **Ticket Cerrado.** Proceso automático de $0. |

---

## 2. Flujo de Cortesías (Monto $0)

El sistema maneja las cortesías de forma inteligente para minimizar errores humanos:

1. **Detección Automática:** Si una cita llega a caja con un costo de **$0.00**, el Frontend pre-selecciona automáticamente el método de pago **"🎁 Cortesía"**.
2. **Registro de Movimiento:** Aunque el monto sea $0, el sistema registra un registro en la tabla de pagos con el método `SIN_COBRO`. Esto sirve para auditoría (saber quién y cuándo aplicó la cortesía).
3. **Finalización Automática:** Al confirmar una cortesía, el Backend detecta que el saldo es cero y mueve el estado de la cita a **`FINALIZADA`** (Completada) de inmediato, sin importar su estado previo.

---

## 3. Pagos y Abonos Reales

### 3.1. Métodos de Pago
- **💵 Efectivo:** Se aprueba de inmediato y suma al flujo de caja de la sesión activa.
- **🏦 Transferencia:** Se aprueba de inmediato (el sistema asume que el recepcionista ya validó el comprobante en sitio).
- **💳 Tarjetas:** Registro de referencia bancaria para conciliación.

### 3.2. Sincronización de Deuda (Paciente)
El sistema mantiene una cuenta corriente por paciente:
- Al registrar un pago, la deuda del paciente (`paciente.saldo_pendiente`) **disminuye** por el monto pagado.
- **Ajustes Proporcionales:** Si por alguna razón administrativa se edita el costo total de la cita antes de cerrar el ticket, el sistema ajusta la deuda global del paciente basándose en la diferencia generada.

---

## 4. Seguridad y Reglas de Oro

1. **Precios Intocables en Caja:** Para respetar los flujos administrativos, el "Costo Total" de la cita es de **solo lectura** en la pantalla de cobro. Cualquier ajuste de precio debe hacerse en la etapa clínica o de edición de cita por un rol autorizado.
2. **No Saldo a Favor:** El sistema impide registrar abonos que superen el saldo pendiente de la cita (Evita errores de captura).
3. **Cierre de Panel:** Tras confirmar un pago que liquida la cuenta o una cortesía, el panel se cierra automáticamente después de 800ms para evitar clics duplicados y registros basura.
4. **Humanización de Datos:** En todo el historial, los términos técnicos como `SIN_COBRO` se traducen automáticamente a lenguaje natural (**Cortesía**) con iconos descriptivos.

---

## 5. Resumen Técnico (Backend)

- **Servicio Principal:** `PagoService.java`
- **Entidades Involucradas:** `Cita`, `Pago`, `Paciente`.
- **Validación Clave:** `checkAndFinalizeCita(uuid, tenantId)` - Se dispara tras cada pago para evaluar si la cita puede pasar al historial como completada.

---

## 6. Análisis de Impacto Financiero

Es fundamental distinguir entre **Ingreso Real** y **Venta Bruta** para la contabilidad de la clínica:

### 6.1. Cortesía Total ($0 desde el inicio)
- **Venta:** $0 | **Ingreso:** $0
- **Uso:** Revisiones o garantías sin costo. No afecta métricas de venta.

### 6.2. Cortesía de Saldo (Perdonar deuda)
Si una cita cuesta $1,000 y el paciente solo pagó $200:
- El recepcionista registra un pago de **$800** con método **CORTESÍA**.
- **Venta Bruta:** $1,000 (Lo que valió el trabajo).
- **Ingreso Real:** $200 (Lo que entró a caja).
- **Cortesía Aplicada:** $800 (Lo que la clínica "invirtió" o perdonó).

### 6.3. Impacto en el Saldo del Paciente
El saldo del paciente siempre se calcula como:
`Saldo = (Monto Total de Citas) - (Pagos Reales + Pagos de Cortesía)`
Esto garantiza que, al aplicar una cortesía por el saldo restante, la cuenta del paciente quede en **$0** y la cita pueda ser finalizada.

---

## 7. Consideraciones Fiscales y Auditoría

Para evitar problemas con la autoridad fiscal (SAT), es vital que los reportes de MEYISOFT POS coincidan con la contabilidad real:

### 7.1. Justificación de Discrepancias
Si se emite una factura por el valor total de un servicio pero se otorga una cortesía parcial, el registro de **"Pago de Cortesía"** sirve como evidencia interna para justificar por qué el ingreso bancario es menor al facturado. 

### 7.2. Ventas vs. Recaudación
- **Reporte de Ventas:** Debe usarse para medir la productividad y el valor del trabajo realizado.
- **Reporte de Ingresos (Flujo de Caja):** Es el que debe usarse para el cálculo de impuestos, ya que solo incluye dinero real (Efectivo, Transferencia, Tarjeta).

### 7.3. Recomendación Contable
Se recomienda que todas las "Cortesías de Saldo" registradas en el sistema sean respaldadas fiscalmente mediante:
1. El uso de **Descuentos** directamente en la factura (CFDI).
2. La emisión de **Notas de Crédito** para cancelar saldos facturados que se decidieron no cobrar.

> [!IMPORTANT]
> El método de pago **Cortesía** en este sistema NO genera un flujo de efectivo, por lo tanto, nunca debe sumarse al cálculo de IVA o ISR a pagar.
