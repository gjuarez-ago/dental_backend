package com.meyisoft.dental.system.repository;

import com.meyisoft.dental.system.entity.RegistroFolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegistroFolioRepository extends JpaRepository<RegistroFolio, UUID> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RegistroFolio> findByTenantIdAndTipoAndFechaAndRegBorrado(UUID tenantId, String tipo, LocalDate fecha, Integer regBorrado);
}
