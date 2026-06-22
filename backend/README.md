Корпоративный Трекер Задач (Task Tracker)

Надежная система управления задачами с упором на безопасность, строгую изоляцию данных и лучшие практики проектирования REST API. Проект представляет собой монолит на Spring Boot с интегрированным фронтендом на чистом JavaScript (Vanilla JS).

📸 Интерфейс

Авторизация

<img width="587" height="465" alt="image" src="https://github.com/user-attachments/assets/be90ca98-8378-4180-972d-2b4a24c694d0" />


Список задач

<img width="1718" height="628" alt="image" src="https://github.com/user-attachments/assets/3cc4ab90-7ed8-4613-90fb-9783b47ee455" />

🎯 Что реализовано

✅JWT-аутентификация и регистрация пользователей.

✅Stateless Security и BCrypt-хеширование паролей.

✅Ролевая модель доступа.

✅Полный CRUD для задач (создание, чтение, обновление, удаление) с моментальным обновлением UI.

✅Динамическая клиентская фильтрация по тексту и статусам.

✅Swagger/OpenAPI для документирования эндпоинтов.

✅DTO-маппинг для безопасной передачи данных.

🔒 Безопасность и изоляция данных

Изоляция контекста: Все операции с задачами выполняются только в рамках пользователя, извлеченного из SecurityContext. Репозиторий использует методы вида findByUserUsername(...), что технически исключает возможность доступа к чужим данным через подмену ID в API (защита от IDOR).

Предотвращение утечек: Внедрены DTO (Data Transfer Objects). Контроллеры никогда не возвращают внутренние сущности (Entity) напрямую, скрывая системные поля и связи базы данных от внешних клиентов.

Защита эндпоинтов: Все маршруты API, кроме авторизации, закрыты через Spring Security.

🏗 Архитектура и структура
Проект реализован как Spring Boot монолит с четким разделением ответственности:

Статический роутинг: Фронтенд-ресурсы (.html, .js) раздаются напрямую из директории /static через настроенный ViewController.

RESTful API: Обработка бизнес-операций осуществляется через REST-контроллеры по путям /api/tasks и /api/auth. Контракты API развиваются независимо от схемы БД.

Слой данных: Бизнес-логика инкапсулирована в TaskService, который взаимодействует с базой через интерфейсы JpaRepository.

Структура проекта

src/main/java/com/getquer/tasktracker
├── controller
├── service
├── repository
├── entity
├── dto
├── security
└── config

Схема БД
User

├─ id (Long)
├─ username (String)
├─ password (String)
└─ role (String)

Task
├─ id (Long)
├─ content (String)
├─ fullNameEmployee (String)
├─ status (String)
└─ user_id (Long, FK)

🛠 Стек технологий

Java 17+

Spring Boot (Web, Data JPA, Security)

PostgreSQL

Hibernate

Maven

HTML5, CSS3, Bootstrap 5, Vanilla JS (Fetch API)


🚀 Быстрый старт

Требования:

JDK 17 или выше

Maven

Установленный сервер PostgreSQL

## Установка и запуск

1. Склонируйте репозиторий:
   ```
   git clone https://github.com/GeTQuer/MySpringProject.git
   ```
   
2. Настройте подключение к базе данных в файле src/main/resources/application.properties:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/tasktracker
   spring.datasource.username=your_user
   spring.datasource.password=your_password
   ```
   
3. Соберите и запустите приложение:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```
   
4. Откройте интерфейс в браузере
   ```bash
   http://localhost:8080/login
   ```
   
📄 Документация API

В проект интегрирован Swagger UI для прямого тестирования эндпоинтов в обход визуального интерфейса.

После запуска приложения документация доступна по адресу:

http://localhost:8080/swagger-ui/index.html
