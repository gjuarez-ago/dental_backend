package com.meyisoft.dental.system.config;

import com.meyisoft.dental.system.entity.CatalogoCie10;
import com.meyisoft.dental.system.entity.Empresa;
import com.meyisoft.dental.system.entity.Sucursal;
import com.meyisoft.dental.system.entity.Usuario;
import com.meyisoft.dental.system.entity.ServicioDental;
import com.meyisoft.dental.system.enums.UserRole;
import com.meyisoft.dental.system.repository.CatalogoCie10Repository;
import com.meyisoft.dental.system.repository.EmpresaRepository;
import com.meyisoft.dental.system.repository.ServicioDentalRepository;
import com.meyisoft.dental.system.repository.SucursalRepository;
import com.meyisoft.dental.system.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

        private final EmpresaRepository empresaRepository;
        private final SucursalRepository sucursalRepository;
        private final UsuarioRepository usuarioRepository;
        private final ServicioDentalRepository servicioRepository;
        private final CatalogoCie10Repository cie10Repository;
        private final PasswordEncoder passwordEncoder;

        @Override
        public void run(String... args) {
                UUID tenantId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

                if (empresaRepository.existsById(tenantId)
                                || usuarioRepository.findByTelefonoContactoAndActive("9991234567").isPresent()) {
                        log.info("Los datos de prueba de Sarai ya existen en el sistema. Saltando inicialización.");
                        return;
                }

                log.info("Cargando datos iniciales de prueba...");

                // 0. Cargar Catálogo CIE-10 Dental Sugerido
                List<CatalogoCie10> cie10Data = List.of(
                                crearCie10("K02.1", "Caries de la dentina", "CARIES"),
                                crearCie10("K04.0", "Pulpitis", "PULPA"),
                                crearCie10("K05.0", "Gingivitis aguda", "PERIODONCIA"),
                                crearCie10("K01.1", "Dientes impactados", "CIRUGIA"),
                                crearCie10("K07.2", "Anomalías de la relación de los arcos dentarios", "ORTODONCIA"),
                                crearCie10("K03.6", "Depósitos [acreciones] sobre los dientes", "ESTETICA"),
                                crearCie10("Z01.2", "Examen odontológico", "PREVENCION"),
                                crearCie10("K02.5", "Caries con exposición pulpar", "CARIES"));
                cie10Repository.saveAll(cie10Data);

                // 1. Crear Empresa (Tenant)
                Empresa empresa = Empresa.builder()
                                .id(tenantId)
                                .tenantId(tenantId)
                                .nombreComercial("Dental Sonrisana")
                                .planSuscripcion("SOLO")
                                .logoUrl("https://pub-8c6866b9de504c61a0aa8938f5cdc44c.r2.dev/empresas/logo_blue-removebg-preview.png")
                                .sitioWeb("https://sonrisana.site")
                                .giro("DENTAL")
                                .telefonoWhatsApp("7581082962")
                                .diasAnticipacionReserva(1)
                                .build();
                empresaRepository.save(empresa);

                // 2. Crear Sucursal
                UUID sucursalId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
                Sucursal sucursal = Sucursal.builder()
                                .id(sucursalId)
                                .tenantId(tenantId)
                                .nombreSucursal("Matriz Centro")
                                .horariosLaborales("{" +
                                                "\"monday\": {\"active\": true, \"startTime\": \"09:00\", \"endTime\": \"20:00\"},"
                                                +
                                                "\"tuesday\": {\"active\": true, \"startTime\": \"09:00\", \"endTime\": \"20:00\"},"
                                                +
                                                "\"wednesday\": {\"active\": true, \"startTime\": \"09:00\", \"endTime\": \"20:00\"},"
                                                +
                                                "\"thursday\": {\"active\": true, \"startTime\": \"09:00\", \"endTime\": \"20:00\"},"
                                                +
                                                "\"friday\": {\"active\": true, \"startTime\": \"09:00\", \"endTime\": \"20:00\"},"
                                                +
                                                "\"saturday\": {\"active\": true, \"startTime\": \"09:00\", \"endTime\": \"14:00\"},"
                                                +
                                                "\"sunday\": {\"active\": false}" +
                                                "}")
                                .banco("BBVA México")
                                .cuentaBancaria("1234567890")
                                .clabeInterbancaria("012345678901234567")
                                .telefono("7581082962")
                                .direccion("Niños Héroes 8, Centro, 40831 San Jerónimo de Juárez, Gro.")
                                .capacidadAtencion(1)
                                .build();
                sucursalRepository.save(sucursal);

                // NIP común para pruebas
                String commonNipHash = passwordEncoder.encode("123456");

                // 3. Crear Usuarios

                // Sarai Rios (OWNER)
                Usuario sarai = Usuario.builder()
                                .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440002"))
                                .tenantId(tenantId)
                                .sucursalIdPrincipal(sucursalId)
                                .rol(UserRole.OWNER)
                                .telefonoContacto("7581082962")
                                .email("sarairiosluviano@gmail.com")
                                .nipHash(commonNipHash)
                                .nombreCompleto("Sarai Rios")
                                .requiereCambioNip(false)
                                .esPersonalClinico(true)
                                .especialidades(new String[] { "GENERAL", "ENDODONCIA", "CIRUGIA", "ESTETICA",
                                                "ORTODONCIA" })
                                .cedulaProfesional("SARAI-12345")
                                .build();
                usuarioRepository.save(sarai);

                /*
                 * ESCENARIO CONSULTORIO (Comentado para uso futuro)
                 * // Dr. Julián Galavis (ENDODONCISTA)
                 * Usuario julian = Usuario.builder()
                 * .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440003"))
                 * .tenantId(tenantId)
                 * .sucursalIdPrincipal(sucursalId)
                 * .rol(UserRole.DOCTOR)
                 * .telefonoContacto("9992223344")
                 * .nipHash(commonNipHash)
                 * .nombreCompleto("Dr. Julián Galavis")
                 * .requiereCambioNip(false)
                 * .esPersonalClinico(true)
                 * .especialidades(new String[] { "ENDODONCIA", "CIRUGIA" })
                 * .cedulaProfesional("JULIAN-54321")
                 * .build();
                 * usuarioRepository.save(julian);
                 * 
                 * // Dra. Elena Ruiz (ORTODONCISTA)
                 * Usuario elena = Usuario.builder()
                 * .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440004"))
                 * .tenantId(tenantId)
                 * .sucursalIdPrincipal(sucursalId)
                 * .rol(UserRole.DOCTOR)
                 * .telefonoContacto("9995556677")
                 * .nipHash(commonNipHash)
                 * .nombreCompleto("Dra. Elena Ruiz")
                 * .requiereCambioNip(false)
                 * .esPersonalClinico(true)
                 * .especialidades(new String[] { "ORTODONCIA" })
                 * .cedulaProfesional("ELENA-98765")
                 * .build();
                 * usuarioRepository.save(elena);
                 */

                // 4. Crear Servicios Reales (Basados en imagen de Sarai Rios)
                List<ServicioDental> servicios = List.of(
                                crearServicio(tenantId, "Limpieza Dental Profunda",
                                                "Eliminación de sarro y placa con ultrasonido.", "650.00", 45,
                                                "#10B981", "GENERAL", false, false),
                                crearServicio(tenantId, "Resina Estética",
                                                "Restauración dental con material del color del diente.", "750.00", 60,
                                                "#3B82F6", "GENERAL", false, false),
                                crearServicio(tenantId, "Consulta y Diagnóstico",
                                                "Evaluación completa y plan de tratamiento.", "300.00", 30, "#6366F1",
                                                "GENERAL", true, false),
                                crearServicio(tenantId, "Endodoncia",
                                                "Tratamiento de conductos para salvar la pieza dental.", "3500.00", 90,
                                                "#EC4899", "ENDODONCIA", false, false),
                                crearServicio(tenantId, "Extracción Dental",
                                                "Remoción de pieza dental con anestesia local.", "800.00", 45,
                                                "#F59E0B", "CIRUGIA", false, true),
                                crearServicio(tenantId, "Blanqueamiento Dental",
                                                "Aclaramiento dental profesional en una sesión.", "2500.00", 90,
                                                "#06B6D4", "ESTETICA", false, false),
                                crearServicio(tenantId, "Valoración de Brackets",
                                                "Estudio inicial para tratamiento de ortodoncia.", "500.00", 45,
                                                "#8B5CF6", "ORTODONCIA", true, false),
                                crearServicio(tenantId, "Cirugía de Tercer Molar", "Extracción de muela del juicio.",
                                                "2800.00", 90, "#EF4444", "CIRUGIA", false, true));
                servicioRepository.saveAll(servicios);

                log.info("Datos iniciales cargados exitosamente con el catálogo real de Sarai Rios.");
        }

        private ServicioDental crearServicio(UUID tenantId, String nombre, String desc, String precio, int duracion,
                        String color, String especialidad, Boolean requiereValoracion, Boolean procQuirurgico) {
                return ServicioDental.builder()
                                .id(UUID.randomUUID())
                                .tenantId(tenantId)
                                .nombre(nombre)
                                .descripcion(desc)
                                .precioBase(new java.math.BigDecimal(precio))
                                .duracionMinutos(duracion)
                                .colorEtiqueta(color)
                                .giro("DENTAL")
                                .especialidadRequerida(especialidad)
                                .requiereValoracion(requiereValoracion)
                                .procedimientoQuirurgico(procQuirurgico)
                                .regBorrado(1)
                                .build();
        }

        private CatalogoCie10 crearCie10(String codigo, String nombre, String categoria) {
                return CatalogoCie10.builder()
                                .id(UUID.randomUUID())
                                .codigo(codigo)
                                .nombre(nombre)
                                .categoria(categoria)
                                .activo(true)
                                .build();
        }
}
