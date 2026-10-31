# Pruebas guiadas — CiudadConecta Integral etapa 1

1. Como `admin@demo.local`, entra a `/admin/usuarios`; crea un usuario ficticio con rol MANTENIMIENTO, o usa `mantenimiento@demo.local` si la cuenta de demo se creó.
2. Como ciudadano, envía un reporte válido con punto dentro de Chorrillos. Comprueba que aparece en `Mis reportes`.
3. Como FUNCIONARIO, abre `/gestion`; cambia REGISTRADO a EN_REVISION.
4. Cambia EN_REVISION a ASIGNADO; prueba primero sin responsable (debe bloquear), luego selecciona área y responsable, guarda y confirma que aparece el plazo.
5. Como otro trabajador de MANTENIMIENTO, comprueba que no puede ver la ficha ni actualizar un reporte que no le está asignado. Como el asignado, comprueba que sí aparece en su panel.
6. El trabajador asignado cambia ASIGNADO a EN_ATENCION. Intenta marcarlo ATENDIDO sin foto ni comentario: debe impedirlo. Adjunta foto JPG/PNG y descripción; debe quedar ATENDIDO.
7. Como ciudadano, abre el reporte atendido. Pulsa «No, solicitar revisión», escribe un motivo de 10+ caracteres: debe volver a EN_ATENCION. El responsable debe ver el caso nuevamente.
8. Vuelve a ATENDIDO con nueva evidencia; como ciudadano, pulsa «Sí, confirmar solución». Debe pasar a CERRADO y quedar anotado en el historial.
9. Comprueba notificaciones del ciudadano en `/notificaciones` al cambiar de estado.
10. Sin iniciar sesión, abre `/transparencia`: debe mostrar solo cifras agregadas, nunca correos, DNI, fotografías o domicilios particulares.
11. Comprueba el panel de accesibilidad y el diseño en celular. Navega con teclado, prueba 200% zoom y revisa errores del formulario. No afirmes conformidad WCAG sin evaluación.
12. Los plazos de 72h son de **demostración**. Para probar vencidos sin esperar, usa una base de pruebas aislada y ajusta una fecha de vencimiento de una incidencia ficticia; no edites datos reales.
