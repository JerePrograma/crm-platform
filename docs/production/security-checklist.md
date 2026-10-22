# Controles de aceptación del entorno

- Commit, imágenes y resultados CI/scanners corresponden al release exacto. Revisar backend, frontend y PostgreSQL; las excepciones requieren responsable, motivo y vencimiento.
- TLS y redirección HTTP→HTTPS activos en `crm.scalaris.com.ar`; certificado público válido, HSTS y cookies Secure/HttpOnly/SameSite comprobados en navegador.
- Solo el peer Caddy revisado figura en la allowlist nginx. `Forwarded` y cabeceras alternativas no permiten falsificar esquema, host ni cliente. No confiar en toda la red compartida.
- PostgreSQL y backend sin puertos publicados; frontend auxiliar exclusivamente en `127.0.0.1:18095`. Solo frontend comparte `proxy` con Caddy.
- Aplicaciones sin root, read-only, no-new-privileges, tmpfs explícitos y límites de CPU/memoria/logs. Datos PostgreSQL en volumen privado persistente.
- CSP/frame denial/nosniff y políticas de navegador presentes en documentos frontend y respuestas API. Los estilos inline conservan los colores de etiquetas de la UI; scripts inline siguen bloqueados.
- Sesión, CSRF, fijación de sesión, bloqueo de intentos, roles Marketing/administración y tenant isolation comprobados. Credenciales fuera de Git y claves iniciales rotadas.
- Archivos hasta 10 MiB y requests hasta 11 MiB atraviesan edge/nginx/backend con límites coherentes; entradas mayores obtienen 413 sin afectar otras aplicaciones.
- Las siete guardas, kill switch persistente, worker y fake inbound permanecen cerrados. No hay claves ni llamadas a proveedores reales.
- Métricas bloqueadas en el edge público; `/actuator/prometheus` requiere sesión con `SETTINGS_MANAGE` en acceso administrativo privado. No abrirlo públicamente para facilitar scrapers.
- Logs/alertas sin claves, cookies, contactos, cuerpos ni payloads. Backups privados y externos cifrados, restore probado, política de privacidad/retención y responsables acordados.

Esta lista define aceptación: su presencia en Git no significa que se haya ejecutado en el VPS.
