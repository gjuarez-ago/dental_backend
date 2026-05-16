package com.meyisoft.dental.system.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotent runtime schema migrations.
 *
 * Hibernate's ddl-auto=update never drops constraints, so we apply targeted
 * fixups here. Each block must be safe to re-run on every boot.
 */
@Slf4j
@Component
@Order(0) // run before DataInitializer
public class SchemaMigrations implements CommandLineRunner {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void run(String... args) {
        fixCitaFolioUniqueConstraint();
    }

    /**
     * The original schema declared `citas.folio` globally UNIQUE, but folios are
     * generated per-tenant (RegistroFolio counter). In a multi-tenant marketplace
     * two tenants will collide on the same day (CIT-yyyyMMdd-0001).
     *
     * We replace any single-column unique constraint on `folio` with a composite
     * unique on `(tenant_id, folio)`.
     */
    private void fixCitaFolioUniqueConstraint() {
        try {
            em.createNativeQuery("""
                    DO $$
                    DECLARE
                        cname TEXT;
                    BEGIN
                        -- 1) Drop any UNIQUE constraint that's only on `folio`
                        FOR cname IN
                            SELECT tc.constraint_name
                            FROM information_schema.table_constraints tc
                            JOIN information_schema.constraint_column_usage ccu
                                ON tc.constraint_name = ccu.constraint_name
                               AND tc.table_schema    = ccu.table_schema
                            WHERE tc.table_name      = 'citas'
                              AND tc.constraint_type = 'UNIQUE'
                            GROUP BY tc.constraint_name
                            HAVING COUNT(ccu.column_name) = 1
                               AND MAX(ccu.column_name)   = 'folio'
                        LOOP
                            EXECUTE 'ALTER TABLE citas DROP CONSTRAINT ' || quote_ident(cname);
                        END LOOP;

                        -- 2) Drop any UNIQUE index that's only on `folio` (created by
                        --    older Hibernate versions as an index rather than a constraint).
                        FOR cname IN
                            SELECT i.relname
                            FROM pg_index x
                            JOIN pg_class  i ON i.oid = x.indexrelid
                            JOIN pg_class  t ON t.oid = x.indrelid
                            JOIN pg_attribute a ON a.attrelid = t.oid
                                                AND a.attnum   = ANY(x.indkey)
                            WHERE t.relname = 'citas'
                              AND x.indisunique
                              AND NOT x.indisprimary
                              AND array_length(x.indkey, 1) = 1
                              AND a.attname = 'folio'
                        LOOP
                            EXECUTE 'DROP INDEX IF EXISTS ' || quote_ident(cname);
                        END LOOP;

                        -- 3) Ensure composite unique (tenant_id, folio) exists
                        IF NOT EXISTS (
                            SELECT 1 FROM pg_constraint
                            WHERE conname = 'uk_citas_tenant_folio'
                        ) THEN
                            ALTER TABLE citas
                                ADD CONSTRAINT uk_citas_tenant_folio UNIQUE (tenant_id, folio);
                        END IF;
                    END $$;
                    """).executeUpdate();
            log.info("Folio constraint migration applied (composite unique on tenant_id, folio).");
        } catch (Exception e) {
            log.error("Failed to fix cita folio unique constraint: {}", e.getMessage(), e);
        }
    }
}
