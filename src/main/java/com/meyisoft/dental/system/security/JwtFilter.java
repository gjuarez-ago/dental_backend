package com.meyisoft.dental.system.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        try {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                // Parseamos el token UNA SOLA VEZ. extractAllClaims ya verifica la firma y expiración.
                Claims claims = jwtUtil.extractAllClaims(jwt);
                
                String userIdStr = claims.getSubject();
                String tenantIdStr = claims.get("tenantId", String.class);
                String sucursalIdStr = claims.get("sucursalId", String.class);
                String role = claims.get("role", String.class);
                String telefono = claims.get("telefono", String.class);
                String email = claims.get("email", String.class);

                UserPrincipal principal = UserPrincipal.builder()
                        .userId(UUID.fromString(userIdStr))
                        .tenantId(tenantIdStr != null ? UUID.fromString(tenantIdStr) : null)
                        .sucursalId(sucursalIdStr != null ? UUID.fromString(sucursalIdStr) : null)
                        .role(role)
                        .telefono(telefono)
                        .email(email)
                        .build();

                String authorityRole = role != null ? "ROLE_" + role : "ROLE_USER";

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        principal, null, Collections.singletonList(new SimpleGrantedAuthority(authorityRole)));

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"ok\": false, \"errorCode\": \"TOKEN_EXPIRED\", \"userMessage\": \"Su sesión ha expirado. Por favor, inicie sesión de nuevo.\"}");
            return;
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"ok\": false, \"errorCode\": \"INVALID_TOKEN\", \"userMessage\": \"Token de seguridad inválido.\"}");
            return;
        } catch (Exception e) {
            // Error inesperado al procesar el token
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
