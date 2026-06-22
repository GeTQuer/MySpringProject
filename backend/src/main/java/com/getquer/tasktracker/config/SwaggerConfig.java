package com.getquer.tasktracker.config;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
///  Общая инфа о API и требование использовать JWT
@OpenAPIDefinition(
        info = @Info(
                title = "Task Tracker API",
                description = "Система управления задачами",
                version = "1.0.0"
        ),
        security = @SecurityRequirement(name = "JWT")
)
///  Указываем что Bearer имеет формат токена JWT
@SecurityScheme(
        name = "JWT",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class SwaggerConfig {

}
