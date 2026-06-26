package com.getquer.tasktracker.controllers;

import com.getquer.tasktracker.Entities.UserEntity;
import com.getquer.tasktracker.Repositories.UserRepository;
import com.getquer.tasktracker.security.JwtCore;
import com.getquer.tasktracker.request.SigninRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtCore jwtCore;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager, JwtCore jwtCore, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtCore = jwtCore;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Принимает логин и пароль в  json формате

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody SigninRequest request){
        // Передаем логин и пароль встроенному менеджеру Spring Security, который сверит пароли
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(),request.password())
        );
        // Если менеджер обработал данные и не вывел ошибку 401, пароль и логин верные
        //Просим сгенерировать токен под текущего пользователя
        String token = jwtCore.generateToken(authentication);
        return ResponseEntity.ok(token);
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody SigninRequest request){
        if (userRepository.findByUsername(request.username()).isPresent()){
            return ResponseEntity.badRequest().body(
                    "Пользователь с таким username существует"
            );
        }
        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole("USER");
        userRepository.save(user);
        return ResponseEntity.ok("Пользователь зарегистрирован!");
    }
}
