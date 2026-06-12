package com.getquer.tasktracker.controllers;

import com.getquer.tasktracker.security.JwtCore;
import com.getquer.tasktracker.request.SigninRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtCore jwtCore;

    public AuthController(AuthenticationManager authenticationManager, JwtCore jwtCore) {
        this.authenticationManager = authenticationManager;
        this.jwtCore = jwtCore;
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
}
