# USAMOS UNA IMAGEN QUE YA TRAE MAVEN Y JDK 17
FROM maven:3.8.4-openjdk-17 AS build
WORKDIR /app

# Copiamos los archivos del proyecto
COPY pom.xml .
COPY src ./src

# COMPILAMOS DENTRO DE DOCKER (No hace falta ./mvnw ni Java en el host)
RUN mvn clean package -DskipTests

# ETAPA DE EJECUCIÓN (Imagen ligera)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
