package com.meyisoft.dental.system.repository;

import com.meyisoft.dental.system.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, UUID> {
    
    // OPTIMIZACIÓN: Obtener solo el ID de la primera empresa, evitando cargar todas
    // Soluciona Query N+1 en PatientAuthService.register()
    @Query(value = "SELECT e.id FROM empresas e LIMIT 1", nativeQuery = true)
    Optional<UUID> findFirstTenantId();
}
