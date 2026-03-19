# Stage 1: Build the application with Maven + JDK (Necesitamos JDK para compilar)
FROM maven:3.8.4-openjdk-17 AS build

# Set the working directory
WORKDIR /app

# Copy the pom.xml and download dependencies
COPY pom.xml .
# Esto descarga las librerías necesarias para que no se repita en cada cambio de código
RUN mvn dependency:go-offline -B

# Copy the rest of the source code
COPY src ./src

# Package the application
RUN mvn clean package -DskipTests

# Stage 2: Create the final, lightweight image
# Usamos Eclipse Temurin (oficial) que es más ligero y seguro que el antiguo openjdk-slim
FROM eclipse-temurin:17-jre-alpine

# Set the working directory
WORKDIR /app

# Copy the executable jar from the build stage
# Importante: el "=" en --from=build es necesario
COPY --from=build /app/target/*.jar app.jar

# Expose the port the app runs on
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]