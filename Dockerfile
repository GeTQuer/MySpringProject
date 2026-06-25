# Stage 1: Build the application using Java 25
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Копируем файлы твоего бэкенд-модуля
COPY backend/pom.xml .
COPY backend/src ./src

# Собираем проект, принудительно указав компилятору Maven использовать Java 25,
# если это прописано в pom.xml, либо полагаемся на настройки toolchain
RUN mvn clean package -DskipTests

# Stage 2: Run the application using Java 25 Runtime
FROM eclipse-temurin:25-jre.ЕЩ
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]