# CiudadConecta PC8 — Fase 1 de ampliación funcional

Esta entrega **no sustituye la base de datos** existente: añade columnas opcionales a `reportes` y una tabla `notificaciones` mediante Hibernate (`ddl-auto=update`) al iniciar en el entorno de desarrollo.

## Cambios incluidos (funcionales)

- RF-03: punto en mapa opcional (Leaflet/OpenStreetMap) y geolocalización únicamente con permiso del navegador, conservando la dirección escrita obligatoria.
- RF-08: fotografía de atención municipal JPG/PNG (una foto actual por reporte, mostrada al ciudadano); el historial sigue registrando estado, autor, fecha y comentario.
- RF-09: notificaciones **internas** al ciudadano por cambios de estado, almacenadas en MariaDB y visibles en `/notificaciones`. **No se envían correos todavía**.
- RF-10: filtro municipal por estado, categoría y búsqueda de código/ubicación, más estadísticas por estado/categoría con barras Bootstrap accesibles.
- US05/RN-10: administración de usuarios existentes (roles y activar/desactivar), solo para `ADMIN`, con protección frente a perder el último administrador activo.
- No se modifican nombres de tablas ya existentes, contraseñas, roles, cuentas de demostración ni la variable `DB_PASSWORD`.

## Paso a paso desde Spring Tool Suite y MariaDB (EndeavourOS)

1. **Respalda primero tu base actual.** Fuera del monitor MariaDB, ejecuta `mariadb-dump -u pc8_app -p pc8_ciudadana > ~/pc8_respaldo_antes_fase1.sql`. Si el comando muestra error, detente y corrígelo antes de continuar.
2. **Respalda también la carpeta de fotografías `uploads/` de tu proyecto anterior**, si tienes reportes con archivos, para conservar las imágenes previas. Cierra la aplicación PC8 en STS. Descomprime este ZIP **en una carpeta NUEVA**. No borres el proyecto que ya funciona.
3. En STS: `File > Import > Maven > Existing Maven Projects` y elige la carpeta que contiene el `pom.xml`. Si STS avisa que ya tienes el mismo proyecto, cierra o retira temporalmente el proyecto anterior del workspace (sin borrar archivos) o importa el nuevo en otro workspace.
4. Asegúrate de mantener la variable `DB_PASSWORD` con la clave de `pc8_app` en `Run > Run Configurations > Spring Boot App > Environment` (la configuración anterior no se copia automáticamente a un nuevo launch).
5. Esta entrega usa `jdbc:mariadb://127.0.0.1:3306/pc8_ciudadana`, la dirección TCP que funcionó en tu equipo. Usa JDK 21.
6. Inicia la aplicación desde STS. Revisa `Started Pc8Application` y entra a `http://localhost:8080`.
7. En la terminal: `mariadb -u pc8_app -p pc8_ciudadana` y ejecuta `SHOW TABLES;` y `DESCRIBE reportes;`. Deberá aparecer `notificaciones` y las nuevas columnas `latitud`, `longitud` y `archivo_atencion` (Hibernate aplica la estrategia física snake_case).
8. Inicia sesión como ciudadano. En `/reportes/nuevo` registra una incidencia con dirección escrita y, si quieres, un punto del mapa. Abre su detalle.
9. Con una cuenta administrador, entra a `/admin/usuarios` y prueba editar el rol o estado de una cuenta no administradora de prueba. Cierra sesión, ingresa como funcionario, abre `/gestion`, utiliza filtros, actualiza el estado y adjunta una fotografía de atención. Revisa `/gestion/estadisticas`.
10. Vuelve a entrar como el ciudadano dueño del reporte. Abre `/notificaciones`, marca el aviso como leído y comprueba su detalle.

## Seguridad y límites

- Un ciudadano únicamente consulta notificaciones y fotografías pertenecientes a sus reportes; funcionarios/admin mantienen las rutas de gestión.
- Las coordenadas precisas pueden ser datos sensibles de ubicación: seleccionarlas es opcional y se muestran exclusivamente al dueño del reporte y al personal municipal autenticado.
- El mapa necesita conexión a internet para cargar Leaflet y las teselas de OpenStreetMap. La dirección escrita permite registrar reportes sin mapa.
- Filtros: se procesan en memoria para el prototipo. Si hay miles de reportes, migrar a consulta paginada en repositorio.
- Notificaciones internas: no hay email ni push móvil. Las fotografías de atención se pueden reemplazar; se conserva solo la última, no un repositorio histórico de fotos.
- `ddl-auto=update` es solo para desarrollo; en producción migrar tablas con Flyway/Liquibase.
- **No se ha validado una compilación Maven o ejecución MariaDB en el entorno de preparación**; prueba el import y arranque en tu STS antes de continuar con la siguiente fase.

## Fases posteriores (pendientes del informe)

Fase 2: creación de cuentas municipales por el administrador y áreas/categorías configurables desde MariaDB, permisos específicos para supervisor y mantenimiento.

Fase 3: notificaciones por correo (SMTP opcional) y múltiples evidencias por reporte con historial.

Fase 4: detección de duplicados basada en ubicación/categoría, clasificación/priorización y mapas municipales filtrados.

Fase 5: indicadores de tiempos, exportación, pruebas con participantes (US01–US07), accesibilidad WCAG y despliegue seguro.
