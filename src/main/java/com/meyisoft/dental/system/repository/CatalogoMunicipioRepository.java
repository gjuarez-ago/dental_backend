package com.meyisoft.dental.system.repository;

import com.meyisoft.dental.system.entity.CatalogoMunicipio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface CatalogoMunicipioRepository extends JpaRepository<CatalogoMunicipio, UUID> {
    List<CatalogoMunicipio> findByEstadoIdOrderByNombreAsc(UUID estadoId);
}
