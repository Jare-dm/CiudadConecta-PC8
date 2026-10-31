
# Construcción con Maven y Java 21
FROM maven:3.9-eclipse-temurin-21 AS compilacion

WORKDIR /codigo

COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp -DskipTests package


# Imagen final: Java 21 JRE
FROM eclipse-temurin:21-jre

WORKDIR /app

# Preparar directorios y permisos.
RUN mkdir -p /app/data /app/uploads \
    && chown -R 10001:1000 /app

# Comprobar que keytool está disponible.
RUN command -v keytool

# Copiar aplicación compilada.
COPY --from=compilacion \
    --chown=10001:1000 \
    /codigo/target/*.jar /app/app.jar

# Copiar geometría pública.
COPY --chown=10001:1000 \
    data/chorrillos.geojson /app/data/chorrillos.geojson

# Copiar script de inicialización.
COPY --chown=10001:1000 \
    docker/entrypoint.sh /app/entrypoint.sh

RUN chmod 755 /app/entrypoint.sh

# Desactivar cuentas demo en producción.
ENV DEMO_USERS_ENABLED=false
ENV EMAIL_NOTIFICATIONS_ENABLED=false

# Usuario no root, con grupo 1000 para acceder a Secret Files.
USER 10001:1000

EXPOSE 8080

ENTRYPOINT ["/bin/sh", "/app/entrypoint.sh"]
