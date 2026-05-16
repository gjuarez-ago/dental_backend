package com.meyisoft.dental.system.controller;

import com.meyisoft.dental.system.entity.CatalogoEstado;
import com.meyisoft.dental.system.entity.CatalogoMunicipio;
import com.meyisoft.dental.system.repository.CatalogoEstadoRepository;
import com.meyisoft.dental.system.repository.CatalogoMunicipioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/catalogs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PublicCatalogController {

    private final CatalogoEstadoRepository estadoRepository;
    private final CatalogoMunicipioRepository municipioRepository;

    @GetMapping("/states")
    public List<CatalogoEstado> getStates() {
        return estadoRepository.findAll();
    }

    @GetMapping("/municipalities")
    public List<CatalogoMunicipio> getMunicipalitiesByState(@RequestParam UUID stateId) {
        return municipioRepository.findByEstadoIdOrderByNombreAsc(stateId);
    }
}
