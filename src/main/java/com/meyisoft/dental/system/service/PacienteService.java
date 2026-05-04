package com.meyisoft.dental.system.service;

import com.meyisoft.dental.system.entity.Paciente;
import com.meyisoft.dental.system.exception.BusinessException;
import com.meyisoft.dental.system.exception.ErrorCodes;
import com.meyisoft.dental.system.config.AuditAction;
import com.meyisoft.dental.system.models.dto.PacienteDTO;
import com.meyisoft.dental.system.enums.NotificationType;
import com.meyisoft.dental.system.repository.PacienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<PacienteDTO> obtenerListadoPriorizado(UUID tenantId) {
        return pacienteRepository.findListadoPriorizado(tenantId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    @AuditAction(modulo = "PACIENTES", accion = "CREAR", descripcion = "Registro de nuevo expediente de paciente")
    public PacienteDTO crearPaciente(PacienteDTO dto, UUID tenantId) {
        // 1. Validar duplicidad por Email (Si se proporciona)
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            if (pacienteRepository.findByEmailAndTenantIdAndRegBorrado(dto.getEmail(), tenantId, 1).isPresent()) {
                throw new BusinessException("DUPLICATE_EMAIL",
                        "Ya existe un paciente registrado con este correo electrónico", HttpStatus.CONFLICT);
            }
        }

        // 2. Validar duplicidad por Teléfono
        if (dto.getTelefono() != null && !dto.getTelefono().trim().isEmpty()) {
            validarTelefono(dto.getTelefono());
            if (pacienteRepository.findByTelefonoAndTenantIdAndRegBorrado(dto.getTelefono(), tenantId, 1).isPresent()) {
                throw new BusinessException("DUPLICATE_PHONE",
                        "Ya existe un paciente registrado con este número de teléfono", HttpStatus.CONFLICT);
            }
        }

        // 3. Validar Estructura CURP (Si se proporciona)
        if (dto.getCurp() != null && !dto.getCurp().trim().isEmpty()) {
            validarCURP(dto.getCurp());
        }

        // Generar PIN aleatorio de 6 dígitos
        String pinRandom = String.format("%06d", new Random().nextInt(999999));

        Paciente paciente = Paciente.builder()
                .id(UUID.randomUUID())
                .nombreCompleto(dto.getNombreCompleto())
                .fechaNacimiento(dto.getFechaNacimiento())
                .telefono(dto.getTelefono())
                .email(dto.getEmail())
                .genero(dto.getGenero())
                .curp(dto.getCurp())
                .direccion(dto.getDireccion())
                .ocupacion(dto.getOcupacion())
                .alergias(dto.getAlergias())
                .enfermedadesCronicas(dto.getEnfermedadesCronicas())
                .medicamentosActuales(dto.getMedicamentosActuales())
                .emergenciaNombre(dto.getEmergenciaNombre())
                .emergenciaTelefono(dto.getEmergenciaTelefono())
                .tipoSangre(dto.getTipoSangre())
                .notasClinicas(dto.getNotasClinicas())
                .antecedentesHeredofamiliares(dto.getAntecedentesHeredofamiliares())
                .antecedentesNoPatologicos(dto.getAntecedentesNoPatologicos())
                .aceptacionPrivacidad(dto.getAceptacionPrivacidad() != null ? dto.getAceptacionPrivacidad() : false)
                .fechaAceptacionPrivacidad(dto.getFechaAceptacionPrivacidad())
                .saldoPendiente(dto.getSaldoPendiente() != null ? dto.getSaldoPendiente() : java.math.BigDecimal.ZERO)
                .expedienteCompleto(dto.getExpedienteCompleto() != null ? dto.getExpedienteCompleto() : false)
                .pinHash(passwordEncoder.encode(pinRandom))
                .pinCambiado(false)
                .emailVerificado(true) // Se marca como verificado automáticamente al crear desde CRM
                .build();

        paciente.setTenantId(tenantId);
        paciente.setRegBorrado(1);

        Paciente guardado = pacienteRepository.save(paciente);

        // Enviar notificación de bienvenida con credenciales
        enviarCorreoBienvenida(guardado, pinRandom);

        return mapToDTO(guardado);
    }

    private void enviarCorreoBienvenida(Paciente paciente, String pin) {
        if (paciente.getEmail() == null || paciente.getEmail().isBlank()) {
            return;
        }

        Map<String, String> vars = new HashMap<>();
        vars.put("NOMBRE_PACIENTE", paciente.getNombreCompleto());
        vars.put("PACIENTE_NUMERO", paciente.getTelefono());
        vars.put("PIN_TEMPORAL", pin);

        String wppMsg = String.format(
                "🦷 ¡Bienvenido(a) %s! Tu acceso al portal dental es: \nNúmero: %s \nPIN: %s \nCámbialo al ingresar.",
                paciente.getNombreCompleto(), paciente.getTelefono(), pin);

        notificationService.notifyPaciente(paciente, NotificationType.BIENVENIDA_PACIENTE, vars, wppMsg);
    }

    @Transactional(readOnly = true)
    public PacienteDTO obtenerPorId(UUID id, UUID tenantId) {
        Paciente paciente = pacienteRepository.findByIdAndTenantIdAndRegBorrado(id, tenantId, 1)
                .orElseThrow(() -> new BusinessException(ErrorCodes.USER_NOT_FOUND, "Paciente no encontrado",
                        HttpStatus.NOT_FOUND));
        return mapToDTO(paciente);
    }

    @Transactional
    @AuditAction(modulo = "PACIENTES", accion = "ACTUALIZAR", descripcion = "Modificación de datos en expediente médico")
    public PacienteDTO actualizar(UUID id, PacienteDTO dto, UUID tenantId) {
        Paciente paciente = pacienteRepository.findByIdAndTenantIdAndRegBorrado(id, tenantId, 1)
                .orElseThrow(() -> new BusinessException(ErrorCodes.USER_NOT_FOUND, "Paciente no encontrado",
                        HttpStatus.NOT_FOUND));

        // 1. Validar duplicidad de Email con otros registros
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            if (pacienteRepository.findByEmailDuplicate(dto.getEmail(), tenantId, 1, id).isPresent()) {
                throw new BusinessException("DUPLICATE_EMAIL", "El correo ya está registrado por otro paciente",
                        HttpStatus.CONFLICT);
            }
        }

        // 2. Validar duplicidad de Teléfono con otros registros
        if (dto.getTelefono() != null && !dto.getTelefono().trim().isEmpty()) {
            validarTelefono(dto.getTelefono());
            if (pacienteRepository.findByTelefonoDuplicate(dto.getTelefono(), tenantId, 1, id).isPresent()) {
                throw new BusinessException("DUPLICATE_PHONE", "El teléfono ya está registrado por otro paciente",
                        HttpStatus.CONFLICT);
            }
        }

        // 3. Validar CURP
        if (dto.getCurp() != null && !dto.getCurp().trim().isEmpty()) {
            validarCURP(dto.getCurp());
        }

        paciente.setNombreCompleto(dto.getNombreCompleto());
        paciente.setFechaNacimiento(dto.getFechaNacimiento());
        paciente.setTelefono(dto.getTelefono());
        paciente.setEmail(dto.getEmail());
        paciente.setGenero(dto.getGenero());
        paciente.setCurp(dto.getCurp());
        paciente.setDireccion(dto.getDireccion());
        paciente.setOcupacion(dto.getOcupacion());
        paciente.setAlergias(dto.getAlergias());
        paciente.setEnfermedadesCronicas(dto.getEnfermedadesCronicas());
        paciente.setMedicamentosActuales(dto.getMedicamentosActuales());
        paciente.setEmergenciaNombre(dto.getEmergenciaNombre());
        paciente.setEmergenciaTelefono(dto.getEmergenciaTelefono());
        paciente.setTipoSangre(dto.getTipoSangre());
        paciente.setNotasClinicas(dto.getNotasClinicas());
        paciente.setAntecedentesHeredofamiliares(dto.getAntecedentesHeredofamiliares());
        paciente.setAntecedentesNoPatologicos(dto.getAntecedentesNoPatologicos());

        if (dto.getAceptacionPrivacidad() != null) {
            paciente.setAceptacionPrivacidad(dto.getAceptacionPrivacidad());
        }
        if (dto.getFechaAceptacionPrivacidad() != null) {
            paciente.setFechaAceptacionPrivacidad(dto.getFechaAceptacionPrivacidad());
        }

        if (dto.getSaldoPendiente() != null)
            paciente.setSaldoPendiente(dto.getSaldoPendiente());
        if (dto.getExpedienteCompleto() != null)
            paciente.setExpedienteCompleto(dto.getExpedienteCompleto());

        return mapToDTO(pacienteRepository.save(paciente));
    }

    private void validarCURP(String curp) {
        if (curp == null || curp.trim().isEmpty()) return;
        
        String regex = "^[A-Z][AEIOU][A-Z]{2}[0-9]{2}(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])[HM](AS|BC|BS|CC|CH|CL|CM|CS|DG|GR|GT|HG|JC|MC|MN|MS|NT|NL|OC|PL|QT|QR|SP|SL|SR|TC|TS|TL|VZ|YN|ZS|NE)[B-DF-HJ-NP-TV-Z]{3}[0-9A-Z][0-9]$";
        
        // Si no cumple el formato oficial, permitimos que se guarde si tiene al menos 10 caracteres (flexibilidad)
        if (!curp.toUpperCase().matches(regex) && curp.length() < 10) {
            throw new BusinessException("INVALID_CURP", "El formato del CURP no es válido o es demasiado corto", HttpStatus.BAD_REQUEST);
        }
    }

    private void validarTelefono(String telefono) {
        if (telefono == null) return;
        
        // Limpiamos espacios, guiones y paréntesis
        String cleanPhone = telefono.replaceAll("[^0-9]", "");
        
        if (cleanPhone.length() < 10) {
            throw new BusinessException("INVALID_PHONE", "El teléfono debe contener al menos 10 dígitos numéricos",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private PacienteDTO mapToDTO(Paciente p) {
        return PacienteDTO.builder()
                .id(p.getId())
                .nombreCompleto(p.getNombreCompleto())
                .fechaNacimiento(p.getFechaNacimiento())
                .telefono(p.getTelefono())
                .email(p.getEmail())
                .genero(p.getGenero())
                .curp(p.getCurp())
                .direccion(p.getDireccion())
                .ocupacion(p.getOcupacion())
                .alergias(p.getAlergias())
                .enfermedadesCronicas(p.getEnfermedadesCronicas())
                .medicamentosActuales(p.getMedicamentosActuales())
                .emergenciaNombre(p.getEmergenciaNombre())
                .emergenciaTelefono(p.getEmergenciaTelefono())
                .tipoSangre(p.getTipoSangre())
                .notasClinicas(p.getNotasClinicas())
                .antecedentesHeredofamiliares(p.getAntecedentesHeredofamiliares())
                .antecedentesNoPatologicos(p.getAntecedentesNoPatologicos())
                .aceptacionPrivacidad(p.getAceptacionPrivacidad())
                .fechaAceptacionPrivacidad(p.getFechaAceptacionPrivacidad())
                .saldoPendiente(p.getSaldoPendiente())
                .expedienteCompleto(p.getExpedienteCompleto())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
