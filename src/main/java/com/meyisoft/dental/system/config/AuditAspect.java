package com.meyisoft.dental.system.config;

import com.meyisoft.dental.system.security.UserPrincipal;
import com.meyisoft.dental.system.service.BitacoraService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {

    private final BitacoraService bitacoraService;

    @AfterReturning(pointcut = "@annotation(auditAction)")
    public void auditAfterReturning(JoinPoint joinPoint, AuditAction auditAction) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {

                UUID tenantId = principal.getTenantId();
                UUID executorId = principal.getUserId();

                String description = auditAction.descripcion();
                if (description.isEmpty()) {
                    description = "Acción automática en " + joinPoint.getSignature().getName();
                }

                // Si el método tiene parámetros, podríamos intentar extraer IDs específicos si
                // fuera necesario
                // por ahora usamos la descripción de la anotación.

                bitacoraService.registrar(
                        tenantId,
                        executorId,
                        auditAction.modulo(),
                        auditAction.accion(),
                        description,
                        null // Podríamos extender esto para capturar el ID de la base si regresara uno
                );
            }
        } catch (Exception e) {
            log.error("Error al procesar auditoría automática: {}", e.getMessage());
        }
    }
}
