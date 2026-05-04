package com.meyisoft.dental.system.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.meyisoft.dental.system.enums.UserRole;
import jakarta.annotation.PostConstruct;

@Component
public class JwtUtil {

    @Value("${jwt.secret:Y2xhdmVfcG9jaGlfbW92aWxfc2VjcmV0XzI1Nl9iaXRzX3NlZ3VyYQ==}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    private SecretKey getSigningKey() {
        return this.key;
    }

    public String generateTokenForCRM(UUID userId, UUID tenantId, UserRole role, UUID sucursalId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tenantId", tenantId);
        claims.put("role", role);
        claims.put("sucursalId", sucursalId);
        claims.put("type", "CRM");

        return Jwts.builder()
                .claims(claims)
                .subject(userId.toString())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateTokenForPatient(UUID pacienteId, String telefono, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", UserRole.PACIENTE);
        claims.put("type", "PATIENT");
        claims.put("telefono", telefono);
        claims.put("email", email);

        return Jwts.builder()
                .claims(claims)
                .subject(pacienteId.toString())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateTokenForCustomer(UUID globalUserId, UUID customerId, UUID tenantId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tenantId", tenantId);
        claims.put("customerId", customerId);
        claims.put("type", "CUSTOMER");

        return Jwts.builder()
                .claims(claims)
                .subject(globalUserId.toString())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            return !extractAllClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
