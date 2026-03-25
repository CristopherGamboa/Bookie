# Etapa 1: Construcción (Build)
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
# Copiamos primero el pom.xml para aprovechar la caché de Docker
COPY pom.xml .
RUN mvn dependency:go-offline
# Copiamos el código fuente y compilamos el proyecto
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución (Runtime)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Solo copiamos el .jar generado en la etapa anterior para que la imagen sea ligera
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]