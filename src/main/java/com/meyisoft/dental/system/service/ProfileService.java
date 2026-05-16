package com.meyisoft.dental.system.service;

import com.meyisoft.dental.system.entity.Usuario;
import com.meyisoft.dental.system.exception.BusinessException;
import com.meyisoft.dental.system.repository.UsuarioRepository;
import com.meyisoft.dental.system.models.dto.UsuarioDTO;
import com.meyisoft.dental.system.repository.EmpresaRepository;
import com.meyisoft.dental.system.repository.SucursalRepository;
import com.meyisoft.dental.system.entity.Empresa;
import com.meyisoft.dental.system.entity.Sucursal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;
    private final StorageService storageService;
    private final ImageService imageService;
    private final S3Client s3Client;

    @org.springframework.beans.factory.annotation.Value("${cloudflare.r2.bucket-name}")
    private String bucketName;

    @org.springframework.beans.factory.annotation.Value("${cloudflare.r2.public-url}")
    private String publicUrl;

    /**
     * Obtiene la información completa del perfil del usuario.
     */
    @Transactional(readOnly = true)
    public UsuarioDTO getProfile(UUID userId) {
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Usuario no encontrado", HttpStatus.NOT_FOUND));

        Empresa empresa = empresaRepository.findById(usuario.getTenantId()).orElse(null);
        
        UsuarioDTO dto = UsuarioDTO.builder()
                .id(usuario.getId())
                .nombreCompleto(usuario.getNombreCompleto())
                .email(usuario.getEmail())
                .telefonoContacto(usuario.getTelefonoContacto())
                .rol(usuario.getRol())
                .tenantId(usuario.getTenantId())
                .sucursalIdPrincipal(usuario.getSucursalIdPrincipal())
                .tenantType(empresa != null ? empresa.getTenantType() : null)
                .activo(usuario.getActivo())
                .esPersonalClinico(usuario.getEsPersonalClinico())
                .onboardingCompletado(empresa != null ? empresa.getOnboardingCompletado() : false)
                .fotografiaUrl(usuario.getFotografiaUrl())
                .biografia(usuario.getBiografia())
                .fechaNacimiento(usuario.getFechaNacimiento())
                .genero(usuario.getGenero())
                .especialidades(usuario.getEspecialidades())
                .nombreComercial(empresa != null ? empresa.getNombreComercial() : null)
                .build();

        if (usuario.getSucursalIdPrincipal() != null) {
            Sucursal sucursal = sucursalRepository.findById(usuario.getSucursalIdPrincipal()).orElse(null);
            if (sucursal != null) {
                dto.setSucursalTelefono(sucursal.getTelefono());
                dto.setEstadoId(sucursal.getEstadoId());
                dto.setMunicipioId(sucursal.getMunicipioId());
            }
        }

        return dto;
    }

    /**
     * Actualiza el perfil del usuario (Bio, Foto, Género y Fecha Nacimiento).
     * Si se proporciona una nueva foto, la comprime y elimina la anterior de R2.
     */
    @Transactional
    public String updateProfile(UUID userId, String biografia, String genero, java.time.LocalDate fechaNacimiento, MultipartFile photoFile) {
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Usuario no encontrado", HttpStatus.NOT_FOUND));

        // 1. Actualizar Datos Básicos
        if (biografia != null) usuario.setBiografia(biografia);
        if (genero != null) usuario.setGenero(genero);
        if (fechaNacimiento != null) usuario.setFechaNacimiento(fechaNacimiento);

        // 2. Procesar Foto si viene una nueva
        if (photoFile != null && !photoFile.isEmpty()) {
            // Eliminar foto anterior si existe
            if (usuario.getFotografiaUrl() != null && usuario.getFotografiaUrl().contains(publicUrl)) {
                try {
                    storageService.deleteFileByUrl(usuario.getFotografiaUrl());
                } catch (Exception e) {
                    log.warn("No se pudo eliminar la foto anterior de R2: {}", e.getMessage());
                }
            }

            // Comprimir la nueva foto (Ancho máx 800px, Calidad 0.75)
            byte[] compressedBytes = imageService.compressImage(photoFile, 800, 0.75f);
            
            // Subir a R2 manualmente para usar los bytes comprimidos
            String fileName = "avatars/" + UUID.randomUUID() + ".jpg";
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType("image/jpeg")
                    .build();

            s3Client.putObject(putObjectRequest, 
                    RequestBody.fromInputStream(new ByteArrayInputStream(compressedBytes), (long) compressedBytes.length));

            String baseUrl = publicUrl.endsWith("/") ? publicUrl : publicUrl + "/";
            String newUrl = baseUrl + fileName;
            
            usuario.setFotografiaUrl(newUrl);
        }

        usuarioRepository.save(usuario);
        return usuario.getFotografiaUrl();
    }
}
