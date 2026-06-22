# Корпоративный Трекер Задач (Task Tracker)

Надежная система управления задачами с упором на безопасность, строгую изоляцию данных и лучшие практики проектирования REST API. Проект представляет собой монолит на Spring Boot с интегрированным фронтендом на чистом JavaScript (Vanilla JS).

## 🛠 Стек технологий

**Бэкенд:**
* Java 17+
* Spring Boot (Web, Data JPA, Security)
* Spring Security & JWT (JSON Web Tokens)
* Hibernate
* PostgreSQL
* Maven

**Фронтенд:**
* HTML5, CSS3
* Vanilla JavaScript (Fetch API)
* Bootstrap 5

## ✨ Основной функционал

* **Безопасная авторизация:** Stateless-управление сессиями с помощью JWT-токенов.
* **Строгая изоляция данных:** Запросы через Hibernate жестко привязаны к контексту авторизованного пользователя (`findByUserUsername`). Пользователь имеет доступ исключительно к собственным записям.
* **REST архитектура:** Разделение слоев с использованием DTO (Data Transfer Objects) для предотвращения утечки данных (Data Leakage) и независимого развития контрактов API от схемы БД.
* **Динамический UI:** Быстрая клиентская фильтрация и манипуляции с DOM без перезагрузки страниц и избыточных запросов к базе данных.
* **Полный CRUD:** Создание, чтение, обновление и удаление задач с моментальным обновлением интерфейса.

## 🏗 Архитектура приложения

Проект реализован как Spring Boot монолит с четким разделением ответственности:
* **Статический роутинг:** Фронтенд-ресурсы (`.html`, `.js`) раздаются напрямую из директории `/static` через настроенный `ViewController`.
* **Слой API:** Обработка бизнес-операций осуществляется через REST-контроллеры (`@RestController`) по путям `/api/tasks` и `/api/auth`.
* **Слой данных:** Бизнес-логика инкапсулирована в `TaskService`, который взаимодействует с базой через интерфейсы `JpaRepository` и маппит сущности `TaskEntity` в безопасные `TaskDTO` перед сериализацией в JSON.

## 🚀 Быстрый старт

### Требования
* JDK 17 или выше
* Maven
* Установленный сервер PostgreSQL

##Установка и запуск
1. Склонируйте репозиторий:
   ```
   git clone https://github.com/GeTQuer/MySpringProject.git
2. Настройте подключение к базе данных в файле src/main/resources/application.properties:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/tasktracker
   spring.datasource.username=your_user
   spring.datasource.password=your_password
3. Соберите и запустите приложение:
   ```bash
   mvn clean install
   mvn spring-boot:run
4. Откройте интерфейс в браузере
   ```bash
   http://localhost:8080/login
📄 Документация API
В проект интегрирован Swagger UI для прямого тестирования эндпоинтов в обход визуального интерфейса.
После запуска приложения документация доступна по адресу:
http://localhost:8080/swagger-ui/index.html
