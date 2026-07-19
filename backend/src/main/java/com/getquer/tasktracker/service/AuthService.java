package com.getquer.tasktracker.service;


import com.getquer.tasktracker.requestDTO.SigninRequest;
import com.getquer.tasktracker.security.JwtCore;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtCore jwtCore;

    public AuthService(AuthenticationManager authenticationManager, JwtCore jwtCore) {
        this.authenticationManager = authenticationManager;
        this.jwtCore = jwtCore;
    }

    public String authenticate(SigninRequest request){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(),request.password())
        );
        // Если менеджер обработал данные и не вывел ошибку 401, пароль и логин верные
        //Просим сгенерировать токен под текущего пользователя
        return (jwtCore.generateToken(authentication));
    }
}
