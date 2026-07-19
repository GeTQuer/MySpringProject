package com.getquer.tasktracker.controllers;

import com.getquer.tasktracker.requestDTO.SigninRequest;
import com.getquer.tasktracker.requestDTO.SignupRequest;
import com.getquer.tasktracker.service.AuthService;
import com.getquer.tasktracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;

        this.userService = userService;
    }

    // Принимает логин и пароль в  json формате
    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody SigninRequest request){
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody SignupRequest request){
        try {
            userService.registerUser(request);
            return ResponseEntity.ok("Пользователь зарегистрирован!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
