# CiudadConecta PC8 — Fase 2: avisos, accesibilidad y teléfonos

## Qué se añadió al proyecto existente

- **Notificaciones internas**: conservadas de Fase 1, con indicador numérico de no leídas en el menú de ciudadanos.
- **Correos opcionales**: envío SMTP al cambiar de estado, **solo si** `EMAIL_NOTIFICATIONS_ENABLED=true`, el SMTP está configurado y el ciudadano activó el consentimiento en `/preferencias`. Se usan eventos **después del commit** para no revertir un reporte si falla el correo.
- **Preferencias del ciudadano**: `/preferencias`, estado guardado en `usuarios.correo_notificaciones` (Hibernate crea la columna). Por defecto, los correos están desactivados.
- **Barra de accesibilidad** visible en todas las pantallas: tamaño de texto 100–200%, modo alto contraste y restablecer. Preferencias de navegador almacenadas localmente. Enlace para saltar a contenido, enfoque de teclado visible y página `/accesibilidad`.
- **Vista móvil**: menús Bootstrap y tablas transformadas a tarjetas en `mis-reportes`, `gestion` y `admin-usuarios` en pantallas estrechas.

## Instalación en STS (EndeavourOS)

1. **Realiza copia de MariaDB y uploads antes de ejecutar otra versión**:
   `umask 077; mariadb-dump -u pc8_app -p pc8_ciudadana > ~/pc8_respaldo_antes_fase2.sql`
2. Descomprime `CiudadConecta_PC8_Fase2.zip`, importa con File > Import > Maven > Existing Maven Projects. El artefacto ahora se llama `ciudadconecta-pc8-fase2` y no reemplaza el proyecto antiguo.
3. Detén la versión anterior para liberar el puerto 8080.
4. Usa Spring Boot App > Run Configurations > Environment: `DB_PASSWORD` = contraseña del usuario MariaDB `pc8_app`.
5. En `application.properties` verifica `jdbc:mariadb://127.0.0.1:3306/pc8_ciudadana` (ya es el valor por defecto).
6. Ejecuta `pe.edu.pc8.Pc8Application`. Abre `http://localhost:8080`.
7. Prueba ciudadano > `/preferencias` y `/notificaciones`. Prueba A+, alto contraste, Tab y móvil.

## Para ACTIVAR el correo de verdad

Sin SMTP, **las notificaciones internas sí funcionan**; el envío de correos está deshabilitado. Necesitas un proveedor de correo/SMTP propio con autorización de envío.
Configura, *en el Environment de la ejecución de STS*, lo siguiente:

| Variable | Ejemplo / explicación |
|---|---|
| `EMAIL_NOTIFICATIONS_ENABLED` | `true` |
| `SMTP_HOST` | Host SMTP de tu proveedor (no suponer) |
| `SMTP_PORT` | `587` para STARTTLS, si tu proveedor lo admite |
| `SMTP_USER` | Usuario SMTP |
| `SMTP_PASSWORD` | Credencial de aplicación SMTP, nunca el archivo público |
| `MAIL_FROM` | Remitente autorizado por el proveedor |
| `SMTP_STARTTLS` | `true` para STARTTLS si el proveedor lo requiere |
| `SMTP_AUTH` | `true` si hay autenticación |

Reinicia Spring Boot. Como ciudadano, entra a `/preferencias` y marca «Recibir avisos por correo». Cambia el estado de un reporte desde el funcionario. Comprueba el correo del ciudadano y el aviso de la web. Nunca pegues contraseñas SMTP en el chat ni hagas commit de contraseñas al repositorio.

## Abrir desde el TELÉFONO sin publicar en Internet

1. Conecta el PC EndeavourOS y el celular al **mismo Wi-Fi**, sin aislamiento de clientes.
2. Con el proyecto ejecutándose en STS, abre una terminal y consulta `hostname -I` o `ip -4 addr`.
3. Identifica la **IP privada del PC en esa Wi-Fi**, por ejemplo `192.168.1.50` (ejemplo, no asumir que sea tu IP real).
4. En el navegador del celular escribe `http://192.168.1.50:8080` sustituyendo la IP.
5. Si no conecta, revisa el firewall local para permitir el puerto `8080` **solo desde la LAN de confianza**, y verifica que el teléfono no esté conectado solo por datos móviles. El proyecto configura `server.address=0.0.0.0` para escuchar en interfaces locales: NO expongas el puerto directamente a Internet.
6. La geolocalización del navegador desde `http://IP_PRIVADA:8080` normalmente exige un origen seguro HTTPS (excepto localhost del propio dispositivo). En modo LAN sin HTTPS, usa el mapa de forma manual o describe la dirección. Para GPS fiable en móvil, configura HTTPS en un despliegue adecuado.

**Seguridad importante:** las credenciales demostrativas `admin@demo.local` / `Demo2026!` (y otras) de versiones antiguas NO deben quedar expuestas al compartir el Wi-Fi. Antes de abrir el acceso desde otros dispositivos, usa una LAN de confianza, desactiva creación demo (`DEMO_USERS_ENABLED=false`) y desactiva/reemplaza las cuentas demo existentes en MariaDB mediante administración del sistema. Desactivar el inicializador NO elimina las cuentas ya creadas.

## Limitaciones y pruebas pendientes

- El envío SMTP depende de credenciales válidas; no fue probado con un servidor real.
- El diseño incluye ayudas WCAG pero no ha sido auditado ni certificado conforme a WCAG 2.2; se requieren pruebas reales con teclado, lector de pantalla, zoom y usuarios.
- El modo de alto contraste es global; se deben comprobar variantes y componentes externos como Leaflet en dispositivos reales.
- El servidor local no es un servicio público permanente; para Internet usar HTTPS, dominio, hosting, firewall y endurecimiento de seguridad.
- Se mantiene `spring.jpa.hibernate.ddl-auto=update` solo para desarrollo. Antes de publicar, usar migraciones.
- El código no fue compilado en este entorno; realizar Maven > Update Project y `mvn test`/Run as Spring Boot App desde STS.
