package com.meyisoft.dental.system.service;

import com.meyisoft.dental.system.entity.Empresa;
import com.meyisoft.dental.system.entity.Paciente;
import com.meyisoft.dental.system.exception.BusinessException;
import com.meyisoft.dental.system.exception.ErrorCodes;
import com.meyisoft.dental.system.models.request.PatientCheckRequest;
import com.meyisoft.dental.system.models.request.PatientCompleteProfileRequest;
import com.meyisoft.dental.system.models.request.PatientRegisterRequest;
import com.meyisoft.dental.system.models.response.AuthResponse;
import com.meyisoft.dental.system.models.response.PatientCheckResponse;
import com.meyisoft.dental.system.repository.CitaRepository;
import com.meyisoft.dental.system.repository.EmpresaRepository;
import com.meyisoft.dental.system.repository.PacienteRepository;
import com.meyisoft.dental.system.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientAuthService {

    private final PacienteRepository pacienteRepository;
    private final EmpresaRepository empresaRepository;
    private final EmailService emailService;
    private final CitaRepository citaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional(readOnly = true)
    public PatientCheckResponse checkPatientPhone(PatientCheckRequest request) {
        var pacientes = pacienteRepository.findAllByTelefonoAndRegBorrado(request.getTelefono(), 1);

        if (pacientes.isEmpty()) {
            return PatientCheckResponse.builder()
                    .status("NOT_FOUND")
                    .message("No existe un paciente con ese número.")
                    .build();
        }

        // Un paciente solo está verificado si ya cambió su PIN inicial Y verificó su email
        boolean isVerified = pacientes.stream().anyMatch(
                p -> Boolean.TRUE.equals(p.getPinCambiado()) && Boolean.TRUE.equals(p.getEmailVerificado()));

        return PatientCheckResponse.builder()
                .status(isVerified ? "EXISTS_VERIFIED" : "EXISTS_UNVERIFIED")
                .message(isVerified ? "Paciente verificado." : "Requiere completar perfil.")
                .build();
    }

    @Transactional
    public AuthResponse completeProfile(PatientCompleteProfileRequest request) {
        List<Paciente> vinculados = pacienteRepository.findAllByTelefonoAndRegBorrado(request.getTelefono(), 1);

        if (vinculados.isEmpty()) {
            throw new BusinessException(ErrorCodes.USER_NOT_FOUND, "El paciente no existe.", HttpStatus.NOT_FOUND);
        }

        String hashedPin = passwordEncoder.encode(request.getNip());

        // Actualizamos todos los perfiles asociados a este teléfono (Sincronización
        // global)
        Paciente pacientePrincipal = vinculados.get(0);
        for (Paciente p : vinculados) {
            p.setEmail(request.getEmail());
            p.setGenero(request.getGenero());
            p.setPinHash(hashedPin);
            p.setPinCambiado(true);
            p.setEmailVerificado(true);
            pacienteRepository.save(p);
        }

        // Generamos el token usando el primero encontrado
        return AuthResponse.builder()
                .token(jwtUtil.generateTokenForPatient(pacientePrincipal.getId(), pacientePrincipal.getTelefono(),
                        pacientePrincipal.getEmail()))
                .user(pacientePrincipal)
                .build();
    }

    @Transactional
    public AuthResponse register(PatientRegisterRequest request) {
        // Verificar si ya existe globalmente
        var existente = pacienteRepository.findAllByTelefonoAndRegBorrado(request.getTelefono(), 1);
        if (!existente.isEmpty()) {
            throw new BusinessException("TELEFONO_DUPLICADO", "Este número ya está registrado. Intente iniciar sesión.",
                    HttpStatus.BAD_REQUEST);
        }

        UUID assignedTenantId = request.getTenantId();

        // Si no envía TenantId, intentamos asignar la clínica por defecto para que no
        // quede huérfano
        if (assignedTenantId == null) {
            assignedTenantId = empresaRepository.findAll().stream()
                    .findFirst()
                    .map(Empresa::getId)
                    .orElseThrow(() -> new BusinessException("CLINICA_NO_ENCONTRADA",
                            "No hay clínicas registradas en el sistema para asociar al paciente.",
                            HttpStatus.INTERNAL_SERVER_ERROR));
        }

        Paciente nuevoPaciente = Paciente.builder()
                .nombreCompleto(request.getNombreCompleto())
                .telefono(request.getTelefono())
                .email(request.getEmail())
                .genero(request.getGenero())
                .pinHash(passwordEncoder.encode(request.getNip()))
                .pinCambiado(true)
                .emailVerificado(true)
                .build();

        nuevoPaciente.setId(UUID.randomUUID());
        nuevoPaciente.setTenantId(assignedTenantId);
        nuevoPaciente.setRegBorrado(1);

        pacienteRepository.save(nuevoPaciente);

        return AuthResponse.builder()
                .token(jwtUtil.generateTokenForPatient(nuevoPaciente.getId(), nuevoPaciente.getTelefono(),
                        nuevoPaciente.getEmail()))
                .user(nuevoPaciente)
                .build();
    }

    @Transactional
    public AuthResponse setupAccess(com.meyisoft.dental.system.models.request.PatientSetupAccessRequest request) {
        // 1. Buscar paciente por teléfono (el teléfono es el identificador de la sesión de reserva)
        List<Paciente> vinculados = pacienteRepository.findAllByTelefonoAndRegBorrado(request.getTelefono(), 1);

        if (vinculados.isEmpty()) {
            throw new BusinessException(ErrorCodes.USER_NOT_FOUND,
                    "No encontramos un paciente con ese número de teléfono.", HttpStatus.NOT_FOUND);
        }

        // ─── GUARDIA DE SEGURIDAD ANTI ACCOUNT-TAKEOVER ─────────────────────────
        // Solo se permite crear/activar acceso si el paciente tiene una cita
        // registrada en las últimas 2 horas. Esto garantiza que el que llama
        // a este endpoint es la misma persona que acaba de completar el booking.
        // Un atacante externo que solo conoce el teléfono NO puede activar la cuenta.
        OffsetDateTime ventana = OffsetDateTime.now().minusHours(2);
        boolean tieneReservaReciente = citaRepository.existeCitaRecientePorTelefono(request.getTelefono(), ventana);

        if (!tieneReservaReciente) {
            throw new BusinessException("ACCESO_NO_PERMITIDO",
                    "No se encontró una reserva reciente asociada a este número. El acceso solo puede activarse inmediatamente después de agendar una cita.",
                    HttpStatus.FORBIDDEN);
        }
        // ─────────────────────────────────────────────────────────────────────────

        // 2. Buscar si ALGUNO de los registros vinculados ya tiene cuenta activa.
        //    Puede haber múltiples registros por el mismo teléfono (multi-sucursal).
        //    Basta con que UNO tenga pinCambiado=true para considerar que el usuario ya se registró.
        Paciente cuentaActiva = vinculados.stream()
                .filter(p -> Boolean.TRUE.equals(p.getPinCambiado()) && p.getEmail() != null)
                .findFirst()
                .orElse(null);

        if (cuentaActiva != null) {
            // El paciente ya configuró su acceso. No sobreescribir. Devolver su token actual.
            return AuthResponse.builder()
                    .token(jwtUtil.generateTokenForPatient(cuentaActiva.getId(),
                            cuentaActiva.getTelefono(), cuentaActiva.getEmail()))
                    .user(cuentaActiva)
                    .temporaryPin("YA_TIENES_CUENTA")
                    .build();
        }

        // El pacientePrincipal es el primer registro sin cuenta activa para el resto de operaciones
        Paciente pacientePrincipal = vinculados.get(0);

        // 3. Validar unicidad del correo: no puede estar ya asociado a OTRO paciente en el mismo tenant.
        UUID tenantId = pacientePrincipal.getTenantId();
        pacienteRepository.findByEmailAndTenantIdAndRegBorrado(request.getEmail(), tenantId, 1)
                .filter(existente -> !existente.getTelefono().equals(request.getTelefono()))
                .ifPresent(dup -> {
                    throw new BusinessException("EMAIL_EN_USO",
                            "Este correo ya está asociado a otro paciente. Usa otro correo electrónico.",
                            HttpStatus.CONFLICT);
                });

        // 4. Generar NIP aleatorio de 6 dígitos seguros (100,000–999,999 combinaciones)
        String generatedPin = String.format("%06d", new java.security.SecureRandom().nextInt(1000000));
        String hashedPin = passwordEncoder.encode(generatedPin);

        // 5. Actualizar todos los registros vinculados (multi-sucursal)
        for (Paciente p : vinculados) {
            p.setEmail(request.getEmail());
            p.setPinHash(hashedPin);
            p.setPinCambiado(true);      // Marca que no está usando el PIN por defecto
            p.setEmailVerificado(true);  // Email capturado directamente, se considera verificado
            pacienteRepository.save(p);
        }

        // 6. Enviar correo de bienvenida con el NIP (asíncrono, no bloquea)
        try {
            com.meyisoft.dental.system.entity.Empresa empresa = empresaRepository.findById(tenantId).orElse(null);
            String nombreClinica = empresa != null ? empresa.getNombreComercial() : "Clínica Dental";
            String sitioWeb = empresa != null && empresa.getSitioWeb() != null ? empresa.getSitioWeb() : "#";
            String telefonoClinica = empresa != null && empresa.getTelefonoWhatsApp() != null ? empresa.getTelefonoWhatsApp() : "";
            String logoUrl = empresa != null && empresa.getLogoUrl() != null && !empresa.getLogoUrl().isBlank()
                    ? empresa.getLogoUrl()
                    : "https://pub-8c6866b9de504c61a0aa8938f5cdc44c.r2.dev/empresas/logo_blue-removebg-preview.png";

            emailService.sendHtmlEmail(
                    request.getEmail(),
                    "🦷 Tus datos de acceso - " + nombreClinica,
                    "bienvenida-paciente",
                    java.util.Map.of(
                            "NOMBRE_PACIENTE", pacientePrincipal.getNombreCompleto(),
                            "NOMBRE_CLINICA", nombreClinica,
                            "PACIENTE_NUMERO", pacientePrincipal.getTelefono(),
                            "PIN_TEMPORAL", generatedPin,
                            "SITIO_WEB", sitioWeb,
                            "LOGO_URL", logoUrl,
                            "TELEFONO_CLINICA", telefonoClinica
                    )
            );
        } catch (Exception e) {
            // El envío de email es no-crítico. Si falla, el NIP sigue visible en la UI.
            // Se loguea para monitoreo pero no interrumpe la respuesta al cliente.
            org.slf4j.LoggerFactory.getLogger(PatientAuthService.class)
                    .error("⚠️ Error enviando email de bienvenida al paciente {}: {}",
                            request.getEmail(), e.getMessage());
        }

        // 7. Devolver JWT + NIP en claro (el frontend lo muestra en el modal)
        return AuthResponse.builder()
                .token(jwtUtil.generateTokenForPatient(pacientePrincipal.getId(),
                        pacientePrincipal.getTelefono(), request.getEmail()))
                .user(pacientePrincipal)
                .temporaryPin(generatedPin)
                .build();
    }
}
