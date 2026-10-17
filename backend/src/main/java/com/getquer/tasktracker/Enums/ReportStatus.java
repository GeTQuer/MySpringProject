package com.getquer.tasktracker.Enums;

public enum ReportStatus {
    PENDING,     // Задание создано, ожидает обработки
    PROCESSING,  // Отчёт формируется
    COMPLETED,   // Отчёт готов, можно скачать
    RETRY_PENDING, // Для докатов на случай падения сервиса или БД
    FAILED       // Формирование завершилось ошибкой

}