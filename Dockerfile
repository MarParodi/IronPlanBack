# ============================================
# IRONPLAN API - Dockerfile para Railway
# ============================================

# Etapa 1: Build
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copiar archivos de Maven
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Dar permisos de ejecución al wrapper
RUN chmod +x mvnw

# Descargar dependencias (cacheado)
RUN ./mvnw dependency:go-offline -B

# Copiar código fuente
COPY src src

# Compilar la aplicación
RUN ./mvnw package -DskipTests -Pprod

# Etapa 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copiar el JAR compilado
COPY --from=builder /app/target/*.jar app.jar

# Puerto expuesto
EXPOSE 8080

# Variables de entorno por defecto
ENV SPRING_PROFILES_ACTIVE=prod
# Limita la RAM total de la JVM (~512 MB), no solo el heap
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAM=512m -XX:MaxRAMPercentage=75.0"

# Comando de inicio
ENTRYPOINT ["java", "-jar", "app.jar"]
