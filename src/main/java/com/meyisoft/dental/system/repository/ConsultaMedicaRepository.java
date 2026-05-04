package com.meyisoft.dental.system.repository;

import com.meyisoft.dental.system.entity.ConsultaMedica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface ConsultaMedicaRepository extends JpaRepository<ConsultaMedica, UUID> {
    Optional<ConsultaMedica> findByCitaId(UUID citaId);
    List<ConsultaMedica> findByCitaIdAndRegBorrado(UUID citaId, int regBorrado);
    List<ConsultaMedica> findByPacienteIdOrderByCreatedAtDesc(UUID pacienteId);

    @org.springframework.data.jpa.repository.Query("SELECT new com.meyisoft.dental.system.models.dto.ConsultaMedicaDTO(" +
           "cm.id, cm.citaId, cm.pacienteId, cm.doctorId, u.nombreCompleto, " +
           "cm.cie10Id, c.nombre, cm.diagnostico, cm.presionArterial, " +
           "cm.frecuenciaCardiaca, cm.frecuenciaRespiratoria, cm.temperatura, " +
           "cm.peso, cm.talla, cm.imc, cm.procedimientoRealizado, " +
           "cm.indicaciones, cm.observacionesInternas, cm.recetaGenerada, " +
           "cm.atencionInicio, cm.atencionFin, cm.createdAt) " +
           "FROM ConsultaMedica cm " +
           "LEFT JOIN Usuario u ON cm.doctorId = u.id " +
           "LEFT JOIN CatalogoCie10 c ON cm.cie10Id = c.id " +
           "WHERE cm.pacienteId = :pacienteId AND cm.regBorrado = 1 " +
           "ORDER BY cm.createdAt DESC")
    List<com.meyisoft.dental.system.models.dto.ConsultaMedicaDTO> findHistorialPacienteDirecto(@org.springframework.data.repository.query.Param("pacienteId") UUID pacienteId);
}
