package com.meyisoft.dental.system.repository;

import com.meyisoft.dental.system.entity.CatalogoEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface CatalogoEstadoRepository extends JpaRepository<CatalogoEstado, UUID> {
    Optional<CatalogoEstado> findByNombre(String nombre);
}
