package com.meyisoft.dental.system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Permite credenciales (cookies, headers de autorización)
        config.setAllowCredentials(true);

        // Permitir el dominio de producción y subdominios
config.setAllowedOriginPatterns(List.of(
        "https://*.meyisoft.com",
        "http://localhost:[*]",
        "https://*.run.app",
        "https://sonrisana.site",
        "https://*.sonrisana.site"
));
        // Headers y métodos permitidos
        config.setAllowedHeaders(Arrays.asList("Origin", "Content-Type", "Accept", "Authorization", "X-Tenant-ID"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Aplica a todas las rutas de la API
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
