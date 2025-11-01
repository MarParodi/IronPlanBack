package com.example.ironplan.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration expiration; // <—

    public JwtService(
            @Value("${app.jwt.secret}") String base64Secret,
            @Value("${app.jwt.expiration:60m}") Duration expiration // default 60m
    ) {
        byte[] keyBytes = tryDecode(base64Secret); // BASE64 o BASE64URL
        if (keyBytes.length < 32) throw new IllegalArgumentException("JWT secret >= 32 bytes.");
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expiration = expiration;
    }

    public String generateToken(String subject, Map<String,Object> claims) {
        long now = System.currentTimeMillis();
        long expMs = expiration.toMillis();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expMs))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    private static byte[] tryDecode(String s) {
        try { return Decoders.BASE64.decode(s); }
        catch (io.jsonwebtoken.io.DecodingException e1) {
            try { return Decoders.BASE64URL.decode(s); }
            catch (io.jsonwebtoken.io.DecodingException e2) {
                return s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }
        }
    }

    /** Extrae el “subject” (email o username según tu AuthService) */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /** Valida firma, expiración y coincidencia del subject */
    public boolean isValid(String token, String expectedSubject) {
        Claims claims = extractAllClaims(token);
        boolean notExpired = claims.getExpiration() != null && claims.getExpiration().after(new Date());
        boolean subjectMatches = expectedSubject == null || expectedSubject.equals(claims.getSubject());
        return notExpired && subjectMatches;
    }

    /** Decodifica el JWT y devuelve todos los claims */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload(); // en 0.12.x es getPayload()
    }
}
