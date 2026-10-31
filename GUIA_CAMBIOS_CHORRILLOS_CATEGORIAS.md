# CiudadConecta PC8 — Cambios de Chorrillos y categorías

## Funciones nuevas

1. El mapa en `/reportes/nuevo` se centra en Chorrillos (UBIGEO `150108`), dibuja el límite y solo deja seleccionar puntos dentro. Se verifica el polígono también en el servidor Java; ya no se acepta reportar sin coordenadas. Existe alternativa de coordenadas manuales para accesibilidad, con la misma validación.
2. En la misma lista de categorías hay **«＋ Agregar nueva categoría…»**. La persona escribe un nombre y pulsa **Guardar categoría**. Se inserta inmediatamente en MariaDB, aparece seleccionado y permanece en la lista de próximos reportes, incluso tras reiniciar.
3. Se conservan categorías enum ya existentes y reportes antiguos. Nuevas categorías se guardan en `categorias_personalizadas`; se enlazan desde `reportes.categoria_personalizada_id`. Los registros anteriores mantienen su etiqueta.
4. Se actualizan panel municipal, detalle, lista de reportes y estadísticas para mostrar categorías personalizadas.

## Requisitos e instalación en STS

- Realiza un backup **antes de iniciar**: `mariadb-dump -u pc8_app -p pc8_ciudadana > ~/pc8_antes_fase21.sql` y conserva la carpeta `uploads`.
- Importa este proyecto como **Existing Maven Project**; no elimines la Fase 2.
- Conserva el valor `DB_PASSWORD` en la configuración de ejecución de Spring Boot.
- Detén el servidor anterior antes de iniciar el nuevo; puerto 8080.
- En desarrollo `spring.jpa.hibernate.ddl-auto=update` crea la tabla y la columna nuevas. Para un despliegue real usa migraciones Flyway/Liquibase.
- **Se necesita Internet al abrir el formulario por primera vez tras reiniciar Spring Boot**: el servidor busca el GeoJSON del distrito en un servicio público de OSINERGMIN y, como alternativa, OSINFOR. Se conserva en caché de memoria durante la ejecución y también en `./data/chorrillos.geojson` para reinicios posteriores. Si el servicio no responde, el sistema **no acepta ubicaciones** para evitar validar zonas incorrectas. Los mosaicos del mapa provienen de OpenStreetMap.
- El contorno proviene de cartografía distrital *referencial*, no garantiza constituir un deslinde legal definitivo. Revisar la cartografía para un uso municipal en producción.
- Los clientes en red local pueden ver el mapa; el botón GPS en móvil puede necesitar HTTPS.

## Pruebas sugeridas

1. Inicia sesión como ciudadano. Crea una categoría como `Robo de tapas de buzón` desde la lista y comprueba que se selecciona.
2. Regresa al formulario: la nueva categoría debe seguir apareciendo. Reinicia la aplicación y comprueba de nuevo.
3. Selecciona un punto dentro de Chorrillos y envía un reporte. Debe guardarse.
4. Selecciona un punto fuera (p. ej., otro distrito). El mapa no debe aceptar el marcador. Si manipulas la petición HTTP, Java debe rechazarlo también.
5. Revisa `SHOW TABLES;` y `SELECT id,nombre FROM categorias_personalizadas;` en MariaDB.
6. Entra como funcionario, revisa el nombre de la nueva categoría y consulta estadísticas.
7. Si aparece error `No se pudo cargar el límite`, verifica la conexión de Internet **del equipo que ejecuta Spring Boot** y la disponibilidad de la fuente geográfica. No elimines esta comprobación para eludir el límite.

## Seguridad y uso responsable

Crear categorías está permitido únicamente a ciudadanos autenticados. Nombres con longitud 3–65, validación de caracteres, detección de duplicados y protección CSRF. Almacenamiento permanente significa que las nuevas categorías son visibles para todos los ciudadanos; para una versión municipal pública se recomienda moderación y controles de abuso. No publiques las cuentas de prueba de la aplicación.
