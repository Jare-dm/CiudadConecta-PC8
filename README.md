# CiudadConecta PC8 — Versión integral, etapa 1

Proyecto universitario **Java 21 + Spring Boot 3 + Thymeleaf + Bootstrap 5 + MariaDB**, importable desde Spring Tool Suite en EndeavourOS.

> Esta es la primera etapa de la versión integral. No representa un sistema municipal listo para producción ni una implementación completa de todas las ideas propuestas.

## Mejoras implementadas en esta etapa

- Mantiene las funcionalidades previas: login por roles, creación y seguimiento de reportes, mapa limitado a Chorrillos (referencial), categorías personalizadas, fotografías, notificaciones internas, preferencias y correo opcional, estadísticas municipales, accesibilidad básica y diseño móvil.
- Agrega roles **MANTENIMIENTO** y **SUPERVISOR** con comprobaciones en servidor.
- Al pasar un reporte a *Asignado* exige área y un funcionario o trabajador activo responsable.
- Calcula un plazo **orientativo de 72 horas** desde la primera asignación (configurable vía `SLA_HORAS`, no es un plazo oficial municipal).
- Destaca reportes vencidos y por vencer en el panel municipal; mantenimiento ve únicamente los asignados a su cuenta.
- Exige comentario y fotografía para cambiar a *Atendido*.
- Permite al ciudadano confirmar solución (cierra) o solicitar revisión motivada (reabre a *En atención* y renueva plazo).
- Permite a supervisores y administradores cerrar excepcionalmente un reporte atendido, indicando motivo.
- Agrega formulario para crear personal municipal desde administración.
- Agrega `/transparencia`: cifras agregadas visibles sin iniciar sesión.

## Instalación segura en STS

1. **Detener la versión anterior de Spring Boot** en STS.
2. Respaldar los datos: `umask 077; mariadb-dump -u pc8_app -p pc8_ciudadana > ~/pc8_antes_integral.sql`.
3. Respaldar **también la carpeta `uploads/` de la versión anterior**. El nuevo proyecto utiliza su propia carpeta relativa; para conservar fotos existentes copia el contenido de la carpeta original a `uploads/` del nuevo proyecto o configura `app.upload.dir` con una ruta absoluta existente. Si la Fase 2 ya tenía `data/chorrillos.geojson`, copia también esa caché al nuevo proyecto para disponer del límite sin depender de su descarga inmediata.
4. Descomprimir este ZIP e importar la carpeta `CiudadConecta_PC8_Integral_Etapa1` mediante **File → Import → Maven → Existing Maven Projects**.
5. Crear una **nueva Run Configuration** Spring Boot App para este proyecto, con `DB_PASSWORD` y, si lo deseas, `SLA_HORAS=72`. Conservar las variables de correo si ya las utilizabas.
6. Ejecutar `pe.edu.pc8.Pc8Application` y comprobar `http://localhost:8080`. **No ejecutar dos versiones simultáneamente** contra la misma base con `ddl-auto=update`.
7. Verificar en MariaDB: `DESCRIBE reportes;` y comprobar columnas nuevas `responsable_id` y `fecha_limite`.
8. Ejecutar las pruebas descritas en `PRUEBAS_ETAPA1.md`.

### Seguridad de la base de datos

La configuración sigue utilizando `spring.jpa.hibernate.ddl-auto=update` únicamente para desarrollo. Al conectar con la base real, Hibernate intentará agregar las dos columnas nuevas (opcionales). En producción necesitarías una migración versionada y revisada. **Nunca elimines tablas ni ejecutes `CREATE ... DROP` sobre la base de datos existente**. Si el esquema actual difiere, detén el arranque y revisa el error antes de reintentar.

### Cuentas DEMO

Con `app.demo.enabled=true` y solo para pruebas locales:

| Rol | Cuenta | Clave inicial (solo si la cuenta no existía) |
|---|---|---|
| Ciudadano | `ciudadano@demo.local` | `Demo2026!` |
| Funcionario | `funcionario@demo.local` | `Demo2026!` |
| Administrador | `admin@demo.local` | `Demo2026!` |
| Mantenimiento | `mantenimiento@demo.local` | `Demo2026!` |
| Supervisor | `supervisor@demo.local` | `Demo2026!` |

Una cuenta que ya existía conserva su contraseña; los usuarios DEMO no se reinician. **No publicar con cuentas de demostración activas**: `DEMO_USERS_ENABLED=false`, credenciales propias y HTTPS antes de cualquier despliegue.

## Acceso desde el celular

En tu computadora local: `ip -4 route get 1.1.1.1` y usa la IP después de `src`. Con Spring Boot ejecutándose, abre `http://IP_ACTUAL:8080` desde el teléfono conectado a la misma red. Si `firewalld` bloquea, abre el puerto temporalmente en una red de confianza. Para GPS se requiere HTTPS en navegadores móviles modernos; se puede escoger el punto manualmente sobre el mapa.

## Pendientes deliberadamente **no incluidos**

Reporte anónimo, moderación de categorías y subcategorías, límites territoriales legales certificados, geolocalización HTTPS, PWA/offline, exportación PDF/Excel, notificaciones de vencimiento por correo a responsables, panel configurable de plazos, protección completa de datos personales y auditoría integral. Estas funciones se implementarán en etapas posteriores, con sus correspondientes pruebas de seguridad y accesibilidad.

## Verificación

La estructura y el código fueron revisados estáticamente. **No se ejecutó compilación Maven ni prueba real contra MariaDB en el entorno de preparación**; debes ejecutar las pruebas en tu STS y enviar el primer `Caused by:` si falla.
