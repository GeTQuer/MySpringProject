FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

COPY backend/pom.xml .
COPY backend/src ./src

# Собираем проект

RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]