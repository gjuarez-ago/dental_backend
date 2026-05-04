package com.meyisoft.dental.system.service;

import com.meyisoft.dental.system.entity.Usuario;
import com.meyisoft.dental.system.enums.UserRole;
import com.meyisoft.dental.system.enums.NotificationType;
import com.meyisoft.dental.system.config.AuditAction;
import com.meyisoft.dental.system.exception.BusinessException;
import com.meyisoft.dental.system.models.request.UsuarioRequest;
import com.meyisoft.dental.system.models.response.UsuarioResponse;
import com.meyisoft.dental.system.repository.EmpresaRepository;
import com.meyisoft.dental.system.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final Random random = new Random();

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarUsuariosPorSucursal(UUID tenantId, UUID sucursalId) {
        return usuarioRepository.findByTenantIdAndSucursalIdPrincipalAndRegBorrado(tenantId, sucursalId, 1)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @AuditAction(modulo = "USUARIOS", accion = "CREAR", descripcion = "Registro de nuevo integrante al equipo")
    public UsuarioResponse crearUsuario(UsuarioRequest request, UUID tenantId, UserRole callerRole) {
        // 1. Validar que solo el OWNER pueda crear usuarios
        if (callerRole != UserRole.OWNER && callerRole != UserRole.SUPER_ADMIN) {
            throw new BusinessException("SOLO_OWNER_PUEDE_CREAR", "Solo el propietario puede crear nuevos usuarios.");
        }

        // 2. Generar NIP aleatorio de 6 dígitos
        String nipGenerado = String.format("%06d", random.nextInt(1000000));

        // 3. Validar límites por rol
        if (request.getRol() == UserRole.OWNER) {
            throw new BusinessException("DUENO_UNICO", "El sistema solo permite un dueño único por clínica.");
        }

        // Validaciones comerciales según el plan de la empresa
        com.meyisoft.dental.system.entity.Empresa empresa = empresaRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException("EMPRESA_NOT_FOUND", "No se encontró la configuración de la clínica."));
        
        String plan = empresa.getPlanSuscripcion() != null ? empresa.getPlanSuscripcion() : "SOLO";

        // Si se intenta agregar personal médico, validamos los límites del plan
        if (Boolean.TRUE.equals(request.getEsPersonalClinico()) || request.getRol() == UserRole.DOCTOR) {
            long staffClinicoActual = usuarioRepository.countClinicalStaffByTenant(tenantId);
            
            if ("SOLO".equals(plan) && staffClinicoActual >= 1) {
                throw new BusinessException("LIMITE_PLAN_EXCEDIDO", 
                    "Tu plan actual (SOLO) solo permite 1 profesional clínico (el doctor). Para agregar más doctores o asistentes médicos, por favor mejora tu plan a CONSULTORIO.");
            }
            
            if ("CONSULTORIO".equals(plan) && staffClinicoActual >= 5) {
                throw new BusinessException("LIMITE_PLAN_EXCEDIDO", 
                    "Tu plan actual (CONSULTORIO) ha llegado al límite de 5 doctores. Por favor mejora a plan RED para personal ilimitado.");
            }
        }

        // Validación de Recepcionistas para el plan SOLO
        if (request.getRol() == UserRole.RECEPTIONIST) {
            if ("SOLO".equals(plan)) {
                long recepActuales = usuarioRepository.countByTenantIdAndRolAndRegBorrado(tenantId, UserRole.RECEPTIONIST, 1);
                if (recepActuales >= 1) {
                    throw new BusinessException("LIMITE_PLAN_EXCEDIDO", 
                        "Tu plan actual (SOLO) solo permite tener 1 recepcionista. Para crecer tu equipo administrativo, te recomendamos el plan CONSULTORIO.");
                }
            }
        }

        if (request.getRol() == UserRole.DOCTOR) {
            if (request.getEspecialidades() == null || request.getEspecialidades().isEmpty()) {
                throw new BusinessException("ESPECIALIDAD_REQUERIDA", "Es obligatorio asignar al menos una especialidad al doctor.");
            }
        }

        // 4. Validar datos duplicados (Teléfono y Email)
        usuarioRepository.findByTelefonoContactoAndActive(request.getTelefonoContacto())
                .ifPresent(u -> {
                    throw new BusinessException("TELEFONO_DUPLICADO",
                            "Ya existe un integrante del equipo con este número de teléfono.");
                });

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            usuarioRepository.findByEmailAndRegBorrado(request.getEmail(), 1)
                    .ifPresent(u -> {
                        throw new BusinessException("EMAIL_DUPLICADO",
                                "Ya existe un usuario registrado con este correo electrónico.");
                    });
        }

        // 5. Crear entidad
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .sucursalIdPrincipal(request.getSucursalId())
                .nombreCompleto(request.getNombreCompleto())
                .telefonoContacto(request.getTelefonoContacto())
                .email(request.getEmail())
                .rol(request.getRol())
                .cedulaProfesional(request.getCedulaProfesional())
                .especialidades(request.getEspecialidades() != null ? request.getEspecialidades().toArray(new String[0]) : null)
                .genero(request.getGenero())
                .nipHash(passwordEncoder.encode(nipGenerado))
                .requiereCambioNip(true)
                .activo(true)
                .regBorrado(1)
                .build();

        Usuario saved = usuarioRepository.save(usuario);

        // 6. Notificar al nuevo colaborador
        if (saved.getEmail() != null && !saved.getEmail().isBlank()) {
            Map<String, String> vars = Map.of(
                    "NOMBRE_USUARIO", saved.getNombreCompleto(),
                    "TELEFONO", saved.getTelefonoContacto(),
                    "NIP_TEMPORAL", nipGenerado);
            notificationService.notifyDoctor(saved, NotificationType.BIENVENIDA_STAFF, vars);
        }

        return mapToResponse(saved);
    }

    @Transactional
    @AuditAction(modulo = "USUARIOS", accion = "ACTUALIZAR", descripcion = "Actualización de datos de perfil")
    public UsuarioResponse actualizarUsuario(UUID id, UsuarioRequest request, UUID tenantId) {
        Usuario usuario = usuarioRepository.findById(id)
                .filter(u -> u.getTenantId().equals(tenantId) && u.getRegBorrado() == 1)
                .orElseThrow(() -> new BusinessException("USUARIO_NO_ENCONTRADO", "El usuario no existe."));

        // Validar email duplicado si está cambiando
        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(usuario.getEmail())) {
            usuarioRepository.findByEmailAndRegBorrado(request.getEmail(), 1)
                    .ifPresent(u -> {
                        if (!u.getId().equals(id)) {
                            throw new BusinessException("EMAIL_DUPLICADO", "Este correo ya está siendo utilizado por otro usuario.");
                        }
                    });
        }

        usuario.setNombreCompleto(request.getNombreCompleto());
        usuario.setEmail(request.getEmail());
        
        if (usuario.getRol() == UserRole.DOCTOR && (request.getEspecialidades() == null || request.getEspecialidades().isEmpty())) {
            throw new BusinessException("ESPECIALIDAD_REQUERIDA", "Un doctor no puede quedarse sin especialidades.");
        }

        usuario.setCedulaProfesional(request.getCedulaProfesional());
        usuario.setGenero(request.getGenero());
        usuario.setEspecialidades(request.getEspecialidades() != null ? request.getEspecialidades().toArray(new String[0]) : null);

        return mapToResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse cambiarEstadoActivo(UUID id, boolean status, UUID tenantId) {
        Usuario usuario = usuarioRepository.findById(id)
                .filter(u -> u.getTenantId().equals(tenantId) && u.getRegBorrado() == 1)
                .orElseThrow(() -> new BusinessException("USUARIO_NO_ENCONTRADO", "Usuario no encontrado."));

        if (!status && usuario.getRol() == UserRole.OWNER) {
            throw new BusinessException("ERROR", "No puedes desactivar al dueño.");
        }

        usuario.setActivo(status);
        return mapToResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public void eliminarUsuario(UUID id, UUID tenantId) {
        Usuario usuario = usuarioRepository.findById(id)
                .filter(u -> u.getTenantId().equals(tenantId) && u.getRegBorrado() == 1)
                .orElseThrow(() -> new BusinessException("USUARIO_NO_ENCONTRADO", "Usuario no encontrado."));

        if (usuario.getRol() == UserRole.OWNER) {
            throw new BusinessException("ERROR", "No puedes borrar al dueño.");
        }

        usuario.setRegBorrado(0);
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    private UsuarioResponse mapToResponse(Usuario usuario) {
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .nombreCompleto(usuario.getNombreCompleto())
                .telefonoContacto(usuario.getTelefonoContacto())
                .email(usuario.getEmail())
                .rol(usuario.getRol())
                .cedulaProfesional(usuario.getCedulaProfesional())
                .activo(usuario.getActivo())
                .especialidades(usuario.getEspecialidades() != null ? java.util.Arrays.asList(usuario.getEspecialidades()) : java.util.Collections.emptyList())
                .sucursalIdPrincipal(usuario.getSucursalIdPrincipal())
                .genero(usuario.getGenero())
                .requiereCambioNip(usuario.getRequiereCambioNip())
                .createdAt(usuario.getCreatedAt())
                .build();
    }
}
