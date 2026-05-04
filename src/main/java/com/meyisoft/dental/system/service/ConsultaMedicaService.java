package com.meyisoft.dental.system.service;

import com.meyisoft.dental.system.entity.Cita;
import com.meyisoft.dental.system.entity.ConsultaMedica;
import com.meyisoft.dental.system.entity.Usuario;
import com.meyisoft.dental.system.enums.AppointmentStatus;
import com.meyisoft.dental.system.exception.BusinessException;
import com.meyisoft.dental.system.models.dto.ConsultaMedicaDTO;
import com.meyisoft.dental.system.repository.CitaRepository;
import com.meyisoft.dental.system.repository.ConsultaMedicaRepository;
import com.meyisoft.dental.system.repository.UsuarioRepository;
import com.meyisoft.dental.system.repository.CatalogoCie10Repository;
import com.meyisoft.dental.system.entity.CatalogoCie10;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConsultaMedicaService {

    private final ConsultaMedicaRepository repository;
    private final CitaRepository citaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CatalogoCie10Repository cie10Repository;

    @Transactional
    public ConsultaMedicaDTO guardarConsulta(ConsultaMedicaDTO dto) {
        // 1. Validar que la cita existe
        Cita cita = citaRepository.findById(dto.getCitaId())
                .orElseThrow(() -> new BusinessException("CITA_NOT_FOUND", "No se encontró la cita asociada", HttpStatus.NOT_FOUND));

        // 2. Crear o actualizar la entidad
        ConsultaMedica entity = repository.findByCitaId(dto.getCitaId())
                .orElseGet(() -> {
                    ConsultaMedica nueva = new ConsultaMedica();
                    nueva.setId(UUID.randomUUID());
                    nueva.setTenantId(cita.getTenantId());
                    return nueva;
                });

        entity.setCitaId(cita.getId());
        entity.setPacienteId(cita.getPacienteId());
        entity.setDoctorId(cita.getDoctorId());
        entity.setCie10Id(dto.getCie10Id());
        entity.setDiagnostico(dto.getDiagnostico());
        entity.setPresionArterial(dto.getPresionArterial());
        entity.setFrecuenciaCardiaca(dto.getFrecuenciaCardiaca());
        entity.setFrecuenciaRespiratoria(dto.getFrecuenciaRespiratoria());
        entity.setTemperatura(dto.getTemperatura());
        entity.setPeso(dto.getPeso());
        entity.setTalla(dto.getTalla());
        entity.setImc(dto.getImc());
        entity.setProcedimientoRealizado(dto.getProcedimientoRealizado());
        entity.setPrescripcionMedica(dto.getPrescripcionMedica());
        entity.setIndicaciones(dto.getIndicaciones());
        entity.setObservacionesInternas(dto.getObservacionesInternas());
        entity.setComplicaciones(dto.getComplicaciones());
        entity.setAtencionInicio(dto.getAtencionInicio());
        entity.setAtencionFin(dto.getAtencionFin() != null ? dto.getAtencionFin() : java.time.OffsetDateTime.now(com.meyisoft.dental.system.utils.DateUtils.MEXICO_OFFSET));

        ConsultaMedica saved = repository.save(entity);

        // 3. Automatización: Cambiar estado de la cita a POR_LIQUIDAR si estaba EN_CONSULTA
        if (cita.getEstado() == AppointmentStatus.EN_CONSULTA) {
            cita.setEstado(AppointmentStatus.POR_LIQUIDAR);
            citaRepository.save(cita);
        }

        return mapToDTO(saved);
    }

    public ConsultaMedicaDTO obtenerPorCita(UUID citaId) {
        return repository.findByCitaId(citaId)
                .map(this::mapToDTO)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ConsultaMedicaDTO> obtenerHistorialPaciente(UUID pacienteId) {
        return repository.findHistorialPacienteDirecto(pacienteId);
    }

    private ConsultaMedicaDTO mapToDTO(ConsultaMedica entity) {
        String doctorNombre = usuarioRepository.findById(entity.getDoctorId())
                .map(Usuario::getNombreCompleto)
                .orElse("Doctor Desconocido");

        return ConsultaMedicaDTO.builder()
                .id(entity.getId())
                .citaId(entity.getCitaId())
                .pacienteId(entity.getPacienteId())
                .doctorId(entity.getDoctorId())
                .doctorNombre(doctorNombre)
                .cie10Id(entity.getCie10Id())
                .cie10Nombre(entity.getCie10Id() != null ? 
                    cie10Repository.findById(entity.getCie10Id()).map(CatalogoCie10::getNombre).orElse(null) : null)
                .diagnostico(entity.getDiagnostico())
                .presionArterial(entity.getPresionArterial())
                .frecuenciaCardiaca(entity.getFrecuenciaCardiaca())
                .frecuenciaRespiratoria(entity.getFrecuenciaRespiratoria())
                .temperatura(entity.getTemperatura())
                .peso(entity.getPeso())
                .talla(entity.getTalla())
                .imc(entity.getImc())
                .procedimientoRealizado(entity.getProcedimientoRealizado())
                .prescripcionMedica(entity.getPrescripcionMedica())
                .indicaciones(entity.getIndicaciones())
                .observacionesInternas(entity.getObservacionesInternas())
                .complicaciones(entity.getComplicaciones())
                .recetaGenerada(entity.getRecetaGenerada())
                .atencionInicio(entity.getAtencionInicio())
                .atencionFin(entity.getAtencionFin())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
