package it.unipi.riskDeV.security;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {
    
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expirationMs}")
    private long jwtExpirationMs;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String userId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
            .subject(userId)
            .claim("role", role)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith((SecretKey) getSigningKey(), Jwts.SIG.HS256)
            .compact();
    }

    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith((SecretKey) getSigningKey()) 
            .build()
            .parseSignedClaims(token) 
            .getPayload();
        return claims.getSubject();
    }

    public String getUserRoleFromToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith((SecretKey) getSigningKey()) 
            .build()
            .parseSignedClaims(token) 
            .getPayload();
        return claims.get("role", String.class);
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith((SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
