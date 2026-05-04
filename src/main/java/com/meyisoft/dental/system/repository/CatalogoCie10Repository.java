package com.meyisoft.dental.system.repository;

import com.meyisoft.dental.system.entity.CatalogoCie10;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CatalogoCie10Repository extends JpaRepository<CatalogoCie10, UUID> {
    List<CatalogoCie10> findByActivoTrueOrderByNombreAsc();
    List<CatalogoCie10> findByCategoriaAndActivoTrueOrderByNombreAsc(String categoria);
    List<CatalogoCie10> findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCaseAndActivoTrue(String nombre, String codigo);
}
