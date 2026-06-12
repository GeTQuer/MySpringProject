package com.getquer.tasktracker.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtCore jwtCore;
    private final UserDetailsService userDetailsService;

    public JwtFilter(JwtCore jwtCore, @Lazy UserDetailsService userDetailsService) {
        this.jwtCore = jwtCore;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String username = null;
        String jwt = null;

        // 1. Проверяем, есть ли заголовок Authorization и начинается ли он с "Bearer "
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7); // Отрезаем слово "Bearer " и берем только сам токен
            try {
                if (jwtCore.validateToken(jwt)) {
                    username = jwtCore.getNameFromToken(jwt); // Извлекаем имя пользователя
                }
            } catch (Exception e) {
                System.out.println("Ошибка токена: " + e.getMessage());
            }
        }

        // 2. Если токен валиден, а пользователь в текущем запросе еще не авторизован
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Загружаем нашего тестового пользователя (admin)
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Создаем официальный внутренний пропуск Spring Security
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
            );

            // Прописываем этот пропуск в контекст безопасности
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        // 3. Передаем запрос дальше по цепочке к контроллерам
        filterChain.doFilter(request, response);
    }
}