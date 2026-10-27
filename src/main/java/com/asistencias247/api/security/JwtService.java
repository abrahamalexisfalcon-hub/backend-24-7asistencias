package com.asistencias247.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secretKey;

    @Value("${jwt.expiration.admin:7200000}")
    private long jwtExpirationAdmin = 7200000L; // 2 horas (2 * 60 * 60 * 1000)

    @Value("${jwt.expiration.tecnico:43200000}")
    private long jwtExpirationTecnico = 43200000L; // 12 horas (12 * 60 * 60 * 1000)

    @Value("${jwt.expiration:7200000}")
    private long jwtExpiration = 7200000L;

    public long getExpirationForRole(String rol) {
        if (rol != null && (rol.equalsIgnoreCase("ADMIN") || rol.equalsIgnoreCase("ADMINISTRADOR") || rol.toUpperCase().contains("ADMIN"))) {
            return jwtExpirationAdmin;
        } else if (rol != null && (rol.equalsIgnoreCase("TECNICO") || rol.toUpperCase().contains("TECNIC"))) {
            return jwtExpirationTecnico;
        }
        return jwtExpiration;
    }

    public String generateToken(UserDetails userDetails, Long usuarioId, String rol) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("usuarioId", usuarioId);
        extraClaims.put("rol", rol);
        long expirationMs = getExpirationForRole(rol);
        return generateToken(extraClaims, userDetails.getUsername(), expirationMs);
    }

    public String generateToken(Map<String, Object> extraClaims, String subject, long expirationMs) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateToken(Map<String, Object> extraClaims, String subject) {
        return generateToken(extraClaims, subject, jwtExpiration);
    }

    public String extractUsername(String token) {
        return extractClaim(token, claims -> claims.getSubject());
    }

    public Long extractUsuarioId(String token) {
        return extractClaim(token, claims -> {
            Object id = claims.get("usuarioId");
            if (id instanceof Number number) {
                return number.longValue();
            }
            return null;
        });
    }

    public String extractRol(String token) {
        return extractClaim(token, claims -> claims.get("rol", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, claims -> claims.getExpiration());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
