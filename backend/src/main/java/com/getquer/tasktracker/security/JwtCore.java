package com.getquer.tasktracker.security;

import com.getquer.tasktracker.Entities.UserEntity;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtCore {

    @Value("${testing.app.secret}")
    private String secret;

    @Value("${testing.app.lifetime}")
    private int lifetime;

    /**
     * Генерирует JWT-токен при успешном логине.
     * Профильные данные загружаются отдельно через /api/users/me,
     * чтобы отдел и грейд не устаревали до истечения токена.
     */
    public String generateToken(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof UserEntity user)) {
            throw new IllegalStateException(
                    "Authentication principal is not UserEntity"
            );
        }

        SecretKey key = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );

        String role = user.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_USER");

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(
                        System.currentTimeMillis() + lifetime
                ))
                .signWith(key)
                .compact();
    }

    /**
     * Извлекает логин пользователя (subject) из зашифрованного токена.
     */
    public String getNameFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Проверяет, что токен не изменен чужаками и у него не истек срок годности.
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            // Если токен просрочен или поврежден
            return false;
        }
    }
}
