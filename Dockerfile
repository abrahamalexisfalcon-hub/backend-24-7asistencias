# Etapa 1: Compilación con Maven y JDK 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -Dmaven.test.skip=true

# Etapa 2: Imagen ligera de ejecución con JRE 21
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
# En lugar de: ENTRYPOINT ["java", "-jar", "app.jar"]
ENTRYPOINT ["java", "-Xms180m", "-Xmx300m", "-XX:+UseSerialGC", "-jar", "app.jar"]