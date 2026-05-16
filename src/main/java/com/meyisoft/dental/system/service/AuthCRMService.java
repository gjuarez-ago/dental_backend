package com.meyisoft.dental.system.service;

import com.meyisoft.dental.system.entity.Empresa;
import com.meyisoft.dental.system.entity.Sucursal;
import com.meyisoft.dental.system.entity.Usuario;
import com.meyisoft.dental.system.enums.UserRole;
import com.meyisoft.dental.system.models.request.LoginRequest;
import com.meyisoft.dental.system.models.request.RegisterTenantRequest;
import com.meyisoft.dental.system.models.response.AuthResponse;
import com.meyisoft.dental.system.repository.EmpresaRepository;
import com.meyisoft.dental.system.repository.SucursalRepository;
import com.meyisoft.dental.system.repository.UsuarioRepository;
import com.meyisoft.dental.system.entity.PasswordResetToken;
import com.meyisoft.dental.system.repository.PasswordResetTokenRepository;
import com.meyisoft.dental.system.repository.PacienteRepository;
import com.meyisoft.dental.system.models.dto.UsuarioDTO;
import com.meyisoft.dental.system.models.dto.PacienteDTO;
import com.meyisoft.dental.system.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.meyisoft.dental.system.exception.BusinessException;
import com.meyisoft.dental.system.exception.ErrorCodes;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthCRMService {

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SucursalRepository sucursalRepository;
    private final PacienteRepository pacienteRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    /**
     * Registro público de tenant SaaS. Crea Empresa + Sucursal única + Usuario OWNER
     * en una sola transacción. El nuevo usuario inicia sesión inmediatamente con el
     * token retornado. Plan inicial: TRIAL.
     */
    @Transactional
    public AuthResponse registerTenant(RegisterTenantRequest request) {
        String email = request.getAdminEmail().trim().toLowerCase();
        String phone = request.getAdminPhone().trim();

        // 1. OPTIMIZACIÓN: Validar unicidad de email y teléfono en una sola query
        // Antes: 2 queries separadas | Ahora: 1 query combinada
        usuarioRepository.findByEmailOrPhoneAndRegBorrado(email, phone).ifPresent(u -> {
            if (u.getEmail().equalsIgnoreCase(email)) {
                throw new BusinessException("EMAIL_ALREADY_REGISTERED",
                        "Este correo ya está registrado. Inicia sesión o usa otro correo.",
                        HttpStatus.CONFLICT);
            } else {
                throw new BusinessException("PHONE_ALREADY_REGISTERED",
                        "Este teléfono ya está registrado. Inicia sesión o usa otro número.",
                        HttpStatus.CONFLICT);
            }
        });

        // 2. Crear Empresa (Tenant)
        UUID tenantId = UUID.randomUUID();
        UUID sucursalId = UUID.randomUUID();
        Empresa empresa = Empresa.builder()
                .nombreComercial(request.getTenantName().trim())
                .giro(request.getGiro())
                .planSuscripcion("TRIAL")
                .pais("México")
                .zonaHoraria("America/Mexico_City")
                .moneda("MXN")
                .prefijoTelefono("+52")
                .idioma("es")
                .activo(true)
                .tenantType(request.getTenantType())
                .sucursalIdPrincipal(sucursalId)
                .onboardingCompletado(false)
                .build();
        empresa.setId(tenantId);
        empresa.setTenantId(tenantId); // self-reference: el tenant_id de la Empresa es ella misma
        empresa = empresaRepository.save(empresa);

        // 3. Crear Sucursal única (el flujo público está limitado a 1)
        String nombreSucursal = "DOCTOR_INDEPENDIENTE".equals(request.getTenantType())
                ? "Consultorio principal"
                : "Sucursal principal";

        Sucursal sucursal = Sucursal.builder()
                .id(sucursalId)
                .tenantId(tenantId)
                .nombreSucursal(nombreSucursal)
                .direccion(request.getSucursalDireccion().trim())
                .telefono(request.getSucursalTelefono())
                .estadoId(UUID.fromString(request.getEstadoId()))
                .municipioId(UUID.fromString(request.getMunicipioId()))
                .ventanaCancelacion(24)
                .capacidadAtencion(1)
                .build();
        sucursal.setId(sucursalId);
        sucursal.setTenantId(tenantId);
        sucursal = sucursalRepository.save(sucursal);

        // 4. Crear Usuario OWNER
        UUID usuarioId = UUID.randomUUID();
        Usuario owner = Usuario.builder()
                .rol(UserRole.OWNER)
                .nombreCompleto(request.getAdminFullName().trim())
                .email(email)
                .telefonoContacto(phone)
                .nipHash(passwordEncoder.encode(request.getAdminNip()))
                .requiereCambioNip(false)
                .sucursalIdPrincipal(sucursalId)
                .esPersonalClinico("DOCTOR_INDEPENDIENTE".equals(request.getTenantType()))
                .activo(true)
                .build();
        owner.setId(usuarioId);
        owner.setTenantId(tenantId);
        owner = usuarioRepository.save(owner);

        // 5. Generar token y respuesta
        UsuarioDTO userDto = UsuarioDTO.builder()
                .id(owner.getId())
                .nombreCompleto(owner.getNombreCompleto())
                .email(owner.getEmail())
                .telefonoContacto(owner.getTelefonoContacto())
                .rol(owner.getRol())
                .tenantId(owner.getTenantId())
                .sucursalIdPrincipal(owner.getSucursalIdPrincipal())
                .tenantType(empresa.getTenantType())
                .activo(owner.getActivo())
                .esPersonalClinico(owner.getEsPersonalClinico())
                .nombreComercial(empresa.getNombreComercial())
                .sucursalTelefono(sucursal.getTelefono())
                .estadoId(sucursal.getEstadoId())
                .municipioId(sucursal.getMunicipioId())
                .fotografiaUrl(owner.getFotografiaUrl())
                .biografia(owner.getBiografia())
                .fechaNacimiento(owner.getFechaNacimiento())
                .genero(owner.getGenero())
                .build();

        return AuthResponse.builder()
                .token(jwtUtil.generateTokenForCRM(owner.getId(), tenantId, owner.getRol(), sucursalId))
                .user(userDto)
                .giro(empresa.getGiro())
                .plan(empresa.getPlanSuscripcion())
                .build();
    }

    /**
     * Verifica si un correo ya está registrado en el sistema (Usuario o Paciente).
     * Usado por el front para feedback en tiempo real durante el signup/login.
     */
    public EmailCheckStatus checkEmail(String emailRaw) {
        if (emailRaw == null || emailRaw.isBlank()) return EmailCheckStatus.NOT_FOUND;
        String email = emailRaw.trim().toLowerCase();

        if (usuarioRepository.findByEmailAndRegBorrado(email, 1).isPresent()) {
            return EmailCheckStatus.STAFF_FOUND;
        }
        if (!pacienteRepository.findAllByEmailAndRegBorrado(email, 1).isEmpty()) {
            return EmailCheckStatus.PATIENT_FOUND;
        }
        return EmailCheckStatus.NOT_FOUND;
    }

    public enum EmailCheckStatus {
        STAFF_FOUND, PATIENT_FOUND, NOT_FOUND
    }

    /**
     * Login CRM exclusivo para personal médico/administrativo.
     * Acepta correo o teléfono como identificador.
     * No permite autenticación de pacientes.
     */
    public AuthResponse loginCRM(LoginRequest request) {
        String identifier = request.getUser() == null ? "" : request.getUser().trim();
        boolean looksLikeEmail = identifier.contains("@");

        // 1. Intentar como Usuario (Personal Clínico)
        var usuarioOpt = looksLikeEmail
                ? usuarioRepository.findByEmailAndRegBorrado(identifier.toLowerCase(), 1)
                : usuarioRepository.findByTelefonoContactoAndActive(identifier);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();

            // 1.1 Validar que el usuario esté habilitado
            if (Boolean.FALSE.equals(usuario.getActivo())) {
                throw new BusinessException("USUARIO_DESHABILITADO",
                    "USUARIO DESACTIVADO: No tienes permiso para entrar al sistema en este momento.",
                    HttpStatus.FORBIDDEN);
            }

            // 1.2 Obtener Empresa
            Empresa empresa = empresaRepository.findById(usuario.getTenantId()).orElse(null);

            // 1.3 Validar que la clínica esté habilitada
            if (empresa != null && Boolean.FALSE.equals(empresa.getActivo())) {
                throw new BusinessException("CLINICA_SUSPENDIDA",
                    "SERVICIO SUSPENDIDO: El acceso para toda la clínica ha sido desactivado.",
                    HttpStatus.FORBIDDEN);
            }

            if (!passwordEncoder.matches(request.getNip(), usuario.getNipHash())) {
                throw new BusinessException(ErrorCodes.AUTH_INVALID_CREDENTIALS, "Credenciales inválidas", HttpStatus.UNAUTHORIZED);
            }

            String giro = empresa != null ? empresa.getGiro() : "GENERAL";
            String plan = empresa != null ? (empresa.getPlanSuscripcion() != null ? empresa.getPlanSuscripcion() : "SOLO") : "SOLO";

            UsuarioDTO userDto = UsuarioDTO.builder()
                    .id(usuario.getId())
                    .nombreCompleto(usuario.getNombreCompleto())
                    .email(usuario.getEmail())
                    .telefonoContacto(usuario.getTelefonoContacto())
                    .rol(usuario.getRol())
                    .tenantId(usuario.getTenantId())
                    .sucursalIdPrincipal(usuario.getSucursalIdPrincipal())
                    .tenantType(empresa != null ? empresa.getTenantType() : "DOCTOR_INDEPENDIENTE")
                    .activo(usuario.getActivo())
                    .esPersonalClinico(usuario.getEsPersonalClinico())
                    .onboardingCompletado(empresa != null ? empresa.getOnboardingCompletado() : false)
                    .especialidades(usuario.getEspecialidades())
                    .nombreComercial(empresa != null ? empresa.getNombreComercial() : null)
                    .fotografiaUrl(usuario.getFotografiaUrl())
                    .biografia(usuario.getBiografia())
                    .fechaNacimiento(usuario.getFechaNacimiento())
                    .genero(usuario.getGenero())
                    .build();

            if (usuario.getSucursalIdPrincipal() != null) {
                var sucursal = sucursalRepository.findById(usuario.getSucursalIdPrincipal()).orElse(null);
                if (sucursal != null) {
                    userDto.setSucursalTelefono(sucursal.getTelefono());
                    userDto.setEstadoId(sucursal.getEstadoId());
                    userDto.setMunicipioId(sucursal.getMunicipioId());
                }
            }

            return AuthResponse.builder()
                    .token(jwtUtil.generateTokenForCRM(usuario.getId(), usuario.getTenantId(), usuario.getRol(), usuario.getSucursalIdPrincipal()))
                    .user(userDto)
                    .giro(giro)
                    .plan(plan)
                    .build();
        }

        throw new BusinessException(ErrorCodes.USER_NOT_FOUND, ErrorCodes.MSG_USER_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    /**
     * Inicia el flujo de recuperación de contraseña generando un token y enviando un correo.
     */
    @Transactional
    public void forgotPassword(String emailRaw) {
        if (emailRaw == null || emailRaw.isBlank()) return;
        String email = emailRaw.trim().toLowerCase();

        var usuarioOpt = usuarioRepository.findByEmailAndRegBorrado(email, 1);
        if (usuarioOpt.isEmpty()) {
            // No lanzar error para evitar enumeración de correos, simplemente ignorar
            return;
        }

        Usuario usuario = usuarioOpt.get();

        // Borrar tokens previos del usuario para que solo haya uno activo
        passwordResetTokenRepository.deleteByUsuario(usuario);

        // Generar nuevo token
        String rawToken = UUID.randomUUID().toString();
        
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .tenantId(usuario.getTenantId())
                .token(rawToken)
                .usuario(usuario)
                .fechaExpiracion(OffsetDateTime.now().plusMinutes(30)) // Expira en 30 minutos
                .build();
        
        passwordResetTokenRepository.save(resetToken);

        // Enviar correo
        String resetLink = frontendUrl + "/reset-password?token=" + rawToken;
        Map<String, String> emailVars = Map.of(
                "NOMBRE_USUARIO", usuario.getNombreCompleto(),
                "RESET_LINK", resetLink
        );

        emailService.sendHtmlEmail(usuario.getEmail(), "Recuperación de Contraseña - MEYISOFT POS", "restablecer-password", emailVars);
    }

    /**
     * Valida el token y actualiza la contraseña (NIP).
     */
    @Transactional
    public void resetPassword(String token, String newNip) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenWithUsuario(token)
                .orElseThrow(() -> new BusinessException("INVALID_TOKEN", "El enlace de recuperación es inválido o ya fue utilizado.", HttpStatus.BAD_REQUEST));

        if (resetToken.getFechaExpiracion().isBefore(OffsetDateTime.now())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new BusinessException("EXPIRED_TOKEN", "El enlace de recuperación ha expirado. Por favor, solicita uno nuevo.", HttpStatus.BAD_REQUEST);
        }

        Usuario usuario = resetToken.getUsuario();
        usuario.setNipHash(passwordEncoder.encode(newNip));
        usuarioRepository.save(usuario);

        // Eliminar el token después de usarlo
        passwordResetTokenRepository.delete(resetToken);
    }
}
