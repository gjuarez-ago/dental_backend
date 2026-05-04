package com.meyisoft.dental.system.controller;

import com.meyisoft.dental.system.models.dto.CatalogoCie10DTO;
import com.meyisoft.dental.system.models.response.ApiResponse;
import com.meyisoft.dental.system.service.CatalogoCie10Service;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cie10")
@RequiredArgsConstructor
public class CatalogoCie10Controller {

    private final CatalogoCie10Service service;

    /**
     * Busca códigos CIE-10 por nombre o código.
     * @param query El término de búsqueda.
     */
    @GetMapping("/search")
    public ApiResponse<List<CatalogoCie10DTO>> buscar(@RequestParam(required = false) String query) {
        List<CatalogoCie10DTO> resultados = service.buscar(query);
        return ApiResponse.success(resultados);
    }
}
