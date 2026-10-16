# Etapa 1: Compilación con Maven y JDK 25
FROM maven:3.9.6-eclipse-temurin-25 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Etapa 2: Imagen ligera de ejecución con JRE 25
FROM eclipse-temurin:25-jre-jammy
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
# En lugar de: ENTRYPOINT ["java", "-jar", "app.jar"]
ENTRYPOINT ["java", "-Xms180m", "-Xmx300m", "-XX:+UseSerialGC", "-jar", "app.jar"]