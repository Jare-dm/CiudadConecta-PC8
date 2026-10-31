# CiudadConecta PC8: construccion con Maven y Java 21
FROM maven:3.9-eclipse-temurin-21 AS compilacion
WORKDIR /codigo

# Primero las dependencias: Docker reutiliza la capa si pom.xml no cambia.
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

# Compilar la aplicacion (las pruebas se ejecutaran por separado).
COPY src ./src
RUN mvn -B -ntp -DskipTests package

# Etapa de ejecucion: no incluye Maven ni archivos fuente.
FROM eclipse-temurin:21-jre
WORKDIR /app

# Por seguridad, no ejecutar el servicio como root.
RUN mkdir -p /app/data /app/uploads \
    && chown -R 10001:10001 /app

COPY --from=compilacion --chown=10001:10001 /codigo/target/*.jar /app/app.jar

# El limite georeferencial debe estar disponible sin descargas externas.
COPY --chown=10001:10001 data/chorrillos.geojson /app/data/chorrillos.geojson

# No crear cuentas de prueba automaticamente en un despliegue remoto.
ENV DEMO_USERS_ENABLED=false
ENV EMAIL_NOTIFICATIONS_ENABLED=false

USER 10001:10001
EXPOSE 8080

# Cambiar server.port a ${PORT:8080} en application.properties.
# Render asigna PORT; localmente, el valor por defecto es 8080.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
