# Корпоративный Трекер Задач (Task Tracker)

Надежная система управления задачами с упором на безопасность, строгую изоляцию данных и лучшие практики проектирования REST API. Проект представляет собой монолит на Spring Boot с интегрированным фронтендом на чистом JavaScript (Vanilla JS).

📸 Интерфейс

Авторизация

<img width="587" height="465" alt="image" src="https://github.com/user-attachments/assets/be90ca98-8378-4180-972d-2b4a24c694d0" />


Список задач

<img width="1718" height="628" alt="image" src="https://github.com/user-attachments/assets/3cc4ab90-7ed8-4613-90fb-9783b47ee455" />

Список задач если их много

<img width="1938" height="1268" alt="image" src="https://github.com/user-attachments/assets/609b364f-84eb-4821-9a13-d8dd6a110822" />


🎯 Что реализовано

✅JWT-аутентификация и регистрация пользователей.

✅Stateless Security и BCrypt-хеширование паролей.

✅Ролевая модель доступа.

✅Полный CRUD для задач (создание, чтение, обновление, удаление) с моментальным обновлением UI.

✅Динамическая клиентская фильтрация по тексту и статусам.

✅Swagger/OpenAPI для документирования эндпоинтов.

✅DTO-маппинг для безопасной передачи данных.

✅Реализовано решение проблемы N+1.

✅ Интеграция Redis (`@Cacheable`) для моментальной отдачи частых запросов и оптимизации нагрузки на БД для AI.

✅ Продвинутая ролевая модель доступа (RBAC)

🔒 Безопасность и изоляция данных

- Изоляция контекста: Все операции с задачами выполняются только в рамках пользователя, извлеченного из SecurityContext. Репозиторий использует методы вида findByUserUsername(...), что технически исключает возможность доступа к чужим данным через подмену ID в API (защита от IDOR).

- Предотвращение утечек: Внедрены DTO (Data Transfer Objects). Контроллеры никогда не возвращают внутренние сущности (Entity) напрямую, скрывая системные поля и связи базы данных от внешних клиентов.

- Защита эндпоинтов: Все маршруты API, кроме авторизации, закрыты через Spring Security.
  
- Админ автоматически создается как временное решение. # После установки роли Админ другим пользователям рекомендуется удалить из СУБД логин admin!!!

🏗 Архитектура и структура

Проект реализован как Spring Boot монолит с четким разделением ответственности:

- Статический роутинг: Фронтенд-ресурсы (.html, .js) раздаются напрямую из директории /static через настроенный ViewController.

- RESTful API: Обработка бизнес-операций осуществляется через REST-контроллеры по путям /api/tasks и /api/auth. Контракты API развиваются независимо от схемы БД.

- Слой данных: Бизнес-логика инкапсулирована в TaskService, который взаимодействует с базой через интерфейсы JpaRepository.

## Структура проекта

```text
src/main/java/com/getquer/tasktracker
├── config             # Конфигурации Security, Redis, Swagger и GlobalExceptionHandler
├── controller         # REST-контроллеры с разделением по ролям
├── service            # Бизнес-логика и кэширование
├── repository         # JpaRepository с пагинацией и кастомными запросами
├── entity             # Сущности базы данных
├── requestDTO         # Валидируемые DTO для входящих данных
├── responseDTO        # DTO для безопасной отправки ответов
└── security           # Фильтры JWT и настройка контекста
```

## Схема БД

```erDiagram
    USER ||--o{ TASK : owns
    DEPARTMENT ||--o{ USER : contains
    DEPARTMENT ||--o{ TASK : contains

    USER {
        Long id
        String username
        String password
        String role
        String seniority
        Long department_id
    }

    TASK {
        Long id
        String content
        String fullNameEmployee
        String status
        Long user_id
        Long department_id
    }
```

## 🛠 Стек технологий

### Backend

- Java 17+
- Spring Boot (Web, Data JPA, Security, AI)
- PostgreSQL
- Hibernate
- Maven
- Redis

### Frontend

- HTML5
- CSS3
- Bootstrap 5
- Vanilla JavaScript (Fetch API)

🚀 Быстрый старт

Требования:

JDK 17 или выше

Maven

Установленный сервер PostgreSQL

## Установка и запуск для запуска без docker-compose

1. Склонируйте репозиторий:
   ```
   git clone https://github.com/GeTQuer/MySpringProject.git
   ```
   
2. Настройте подключение к базе данных в файле src/main/resources/application.properties:
   ```properties
  spring.datasource.url=jdbc:postgresql://localhost:5432/tasktracker
  spring.datasource.username=your_user
  spring.datasource.password=your_password
  spring.data.redis.host=localhost
  spring.ai.google.genai.api-key= ВАШ API
  spring.ai.google.genai.chat.options.model= ВАША МОДЕЛЬ
  
   ```
   
3. Соберите и запустите приложение:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```
   
4. Откройте интерфейс в браузере
   ```text
   http://localhost:8080/login
   ```
## Установка и запуск c docker-compose (ПЕРЕД ЗАПУСКОМ ВВЕДИТЕ КОНФИГУРАЦИОННЫЕ ДАННЫЕ В .env!!!)
1. Введите команду
   ```
   docker-compose up --build

📄 Документация API

В проект интегрирован Swagger UI для прямого тестирования эндпоинтов в обход визуального интерфейса.

После запуска приложения документация доступна по адресу:

http://localhost:8080/swagger-ui/index.html
