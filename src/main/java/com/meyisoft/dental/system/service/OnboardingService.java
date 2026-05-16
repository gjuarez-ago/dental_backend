package com.meyisoft.dental.system.service;

import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meyisoft.dental.system.entity.Empresa;
import com.meyisoft.dental.system.entity.ServicioDental;
import com.meyisoft.dental.system.entity.Sucursal;
import com.meyisoft.dental.system.entity.Usuario;
import com.meyisoft.dental.system.exception.BusinessException;
import org.springframework.http.HttpStatus;
import com.meyisoft.dental.system.models.request.OnboardingRequest;
import com.meyisoft.dental.system.repository.EmpresaRepository;
import com.meyisoft.dental.system.repository.ServicioDentalRepository;
import com.meyisoft.dental.system.repository.SucursalRepository;
import com.meyisoft.dental.system.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;
    private final ServicioDentalRepository servicioDentalRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    /**
     * Procesa el onboarding de un tenant (médico), asegurando que el usuario logueado
     * sea quien está configurando su consultorio.
     */
    @Transactional
    public void processOnboarding(java.util.UUID userId, OnboardingRequest request) {
        // 1. Obtener el usuario autenticado. El USUARIO ES OBLIGATORIO.
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Usuario no encontrado con ID: " + userId, HttpStatus.NOT_FOUND));

        // Validar que confirme su cédula
        if (Boolean.FALSE.equals(request.getCedulaConfirmada())) {
            throw new BusinessException("VALIDATION_ERROR", "Es obligatorio confirmar que cuenta con cédula profesional vigente.", HttpStatus.BAD_REQUEST);
        }

        // Obtener el Tenant (Empresa) asociado al usuario
        Empresa empresa = empresaRepository.findById(usuario.getTenantId())
                .orElseThrow(() -> new BusinessException("TENANT_NOT_FOUND", "Tenant no encontrado", HttpStatus.NOT_FOUND));

        if (Boolean.TRUE.equals(empresa.getOnboardingCompletado())) {
            throw new BusinessException("ONBOARDING_ALREADY_COMPLETED", "El onboarding ya ha sido completado para esta empresa.", HttpStatus.BAD_REQUEST);
        }

        // Obtener la sucursal principal
        Sucursal sucursal = sucursalRepository.findById(usuario.getSucursalIdPrincipal())
                .orElseThrow(() -> new BusinessException("SUCURSAL_NOT_FOUND", "Sucursal principal no encontrada", HttpStatus.NOT_FOUND));

        // 2. Actualizar configuración de la Empresa
        var conf = request.getEmpresa();
        if (conf.getNombreConsultorio() != null && !conf.getNombreConsultorio().isBlank()) {
            empresa.setNombreComercial(conf.getNombreConsultorio());
        }
        if (conf.getTelefono() != null && !conf.getTelefono().isBlank()) {
            empresa.setTelefonoWhatsApp(conf.getTelefono());
        }
        if (conf.getZonaHoraria() != null) {
            empresa.setZonaHoraria(conf.getZonaHoraria());
        }
        if (conf.getHabilitarComprobantes() != null) {
            empresa.setComprobanteSpei(conf.getHabilitarComprobantes());
        }
        empresa.setOnboardingCompletado(true);
        empresaRepository.save(empresa);

        // 3. Actualizar Sucursal (Horarios y Datos Bancarios)
        try {
            java.util.Map<String, Object> horariosEstandar = new java.util.LinkedHashMap<>();
            java.util.Map<String, String> dayMap = java.util.Map.of(
                "mon", "monday", "tue", "tuesday", "wed", "wednesday",
                "thu", "thursday", "fri", "friday", "sat", "saturday", "sun", "sunday"
            );
            
            if (request.getHorarios() != null) {
                request.getHorarios().forEach((key, val) -> {
                    String standardKey = dayMap.getOrDefault(key, key);
                    java.util.Map<String, Object> standardVal = new java.util.HashMap<>();
                    standardVal.put("active", val.getActive());
                    if (val.getActive() != null && val.getActive()) {
                        standardVal.put("startTime", val.getStart());
                        standardVal.put("endTime", val.getEnd());
                    }
                    horariosEstandar.put(standardKey, standardVal);
                });
            }
            
            String horariosJson = objectMapper.writeValueAsString(horariosEstandar);
            sucursal.setHorariosLaborales(horariosJson);
        } catch (JsonProcessingException e) {
            throw new BusinessException("JSON_ERROR", "Error al procesar los horarios", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (conf.getBanco() != null) sucursal.setBanco(conf.getBanco());
        if (conf.getCuentaBancaria() != null) sucursal.setCuentaBancaria(conf.getCuentaBancaria());
        if (conf.getClabeInterbancaria() != null) sucursal.setClabeInterbancaria(conf.getClabeInterbancaria());
        
        if (conf.getEstadoId() != null && !conf.getEstadoId().isBlank()) {
            sucursal.setEstadoId(java.util.UUID.fromString(conf.getEstadoId()));
        }
        if (conf.getMunicipioId() != null && !conf.getMunicipioId().isBlank()) {
            sucursal.setMunicipioId(java.util.UUID.fromString(conf.getMunicipioId()));
        }

        sucursalRepository.save(sucursal);

        // 4. Crear Servicio Inicial
        var srv = request.getServicio();
        ServicioDental servicio = ServicioDental.builder()
                .id(java.util.UUID.randomUUID())
                .tenantId(empresa.getId())
                .nombre(srv.getNombre())
                .duracionMinutos(srv.getDuracion() != null ? srv.getDuracion() : 30)
                .precioBase(srv.getPrecio() != null ? srv.getPrecio() : java.math.BigDecimal.ZERO)
                .requiereValoracion(false)
                .giro(empresa.getGiro() != null ? empresa.getGiro() : "DENTAL")
                .build();
        servicioDentalRepository.save(servicio);

        // 5. Actualizar perfil del Usuario
        if (request.getPerfil() != null) {
            var perf = request.getPerfil();
            if (perf.getBiografia() != null) usuario.setBiografia(perf.getBiografia());
            if (perf.getFotografiaUrl() != null) usuario.setFotografiaUrl(perf.getFotografiaUrl());
            if (perf.getGenero() != null) usuario.setGenero(perf.getGenero());
        }
        
        // Guardar cédula profesional del request
        if (request.getCedulaProfesional() != null && !request.getCedulaProfesional().isBlank()) {
            usuario.setCedulaProfesional(request.getCedulaProfesional());
        }
        
        usuarioRepository.save(usuario);
    }
}
