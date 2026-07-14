package com.getquer.tasktracker.controllers;

import com.getquer.tasktracker.request.SigninRequest;
import com.getquer.tasktracker.request.SignupRequest;
import com.getquer.tasktracker.service.AuthService;
import com.getquer.tasktracker.service.UserService;
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
    public ResponseEntity<String> login(@RequestBody SigninRequest request){
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody SignupRequest request){
        try {
            userService.registerUser(request);
            return ResponseEntity.ok("Пользователь зарегистрирован!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
