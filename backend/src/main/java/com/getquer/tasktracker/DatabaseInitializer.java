package com.getquer.tasktracker;

import com.getquer.tasktracker.Entities.UserEntity;
import com.getquer.tasktracker.Repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Спринг сам внедрит сюда репозиторий и бин кодировщика паролей
    public DatabaseInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Проверяем, существует ли уже пользователь с логином "admin"
        if (userRepository.findByUsername("admin").isEmpty()) {

            UserEntity admin = new UserEntity();
            admin.setUsername("admin");

            // Хэшируем твой новый пароль "12345" прямо в коде перед отправкой в БД
            admin.setPassword(passwordEncoder.encode("12345"));
            admin.setRole("ROLE_ADMIN");

            userRepository.save(admin);

            System.out.println("=================================================");
            System.out.println(">>> Системный скрипт: Главный ADMIN успешно создан!");
            System.out.println(">>> Логин: admin | Пароль: 12345");
            System.out.println("=================================================");
        } else {
            // Если админ уже есть в БД, скрипт просто молча проходит мимо
            System.out.println(">>> Системный скрипт: Инициализация не требуется, ADMIN уже есть в базе.");
        }
    }
}