package com.getquer.tasktracker.security;

import ch.qos.logback.core.encoder.Encoder;
import com.getquer.tasktracker.Repositories.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtFilter jwtFilter;
    private final UserRepository userRepository;

    public SecurityConfig(JwtFilter jwtFilter, UserRepository userRepository){
        this.jwtFilter = jwtFilter;
        this.userRepository = userRepository;
    }
    //Фильтр безопасности, который определяет, какие запросы пропустить или заблокировать
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login",
                                "/login.html",
                                "/register",
                                "/register.html",
                                "/tasks",
                                "/tasks.html",
                                "/api/auth/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/error",
                                "/admin",
                                "/admin-panel.html",
                                "/api/tasks/my",
                                "/api/tasks/all"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                // САМАЯ ВАЖНАЯ СТРОЧКА: Вставляем наш JwtFilter перед стандартным фильтром Spring Security!
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    //Для вручной проверки паролей
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
    // Временное хранилище пользователей в Оперативной памяти
    // Позже добавлю БД
    // Временно создаю тестового пользователя
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            // Проверку на "admin" из оперативки полностью УБРАЛИ.
            // Теперь сразу идем в PostgreSQL для любого логина:
            return userRepository.findByUsername(username)
                    .map(user -> new User(
                            user.getUsername(),
                            user.getPassword(),
                            java.util.Collections.singletonList(
                                    new SimpleGrantedAuthority("ROLE_" + user.getRole())
                            )
                    ))
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "Пользователь " + username + " не найден"
                    ));
        };
    }
    //Шифрует пароли
    // Надежный алгоритм BCrypt
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
