package com.meyisoft.dental.system.service;

import com.meyisoft.dental.system.entity.CatalogoCie10;
import com.meyisoft.dental.system.models.dto.CatalogoCie10DTO;
import com.meyisoft.dental.system.repository.CatalogoCie10Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogoCie10Service {

    private final CatalogoCie10Repository repository;

    @Transactional(readOnly = true)
    public List<CatalogoCie10DTO> buscar(String query) {
        List<CatalogoCie10> results;
        if (query == null || query.trim().isEmpty()) {
            results = repository.findByActivoTrueOrderByNombreAsc();
        } else {
            results = repository.findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCaseAndActivoTrue(query, query);
        }
        
        return results.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private CatalogoCie10DTO mapToDTO(CatalogoCie10 entity) {
        return CatalogoCie10DTO.builder()
                .id(entity.getId())
                .codigo(entity.getCodigo())
                .nombre(entity.getNombre())
                .categoria(entity.getCategoria())
                .build();
    }
}
