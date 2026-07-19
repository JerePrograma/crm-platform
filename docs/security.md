# Seguridad

## Postura actual

La plataforma opera con política fail-closed:

- sin credenciales bootstrap explícitas no existe ningún usuario válido;
- salud es pública, API y OpenAPI requieren autenticación;
- cualquier otra ruta se deniega;
- sesiones HTTP stateless;
- credenciales del frontend solo en memoria;
- envíos inexistentes y bloqueados por cuatro controles;
- secretos y datos operativos excluidos de Git.

## Autenticación temporal

SEG-001 utiliza HTTP Basic únicamente para desarrollo. Las variables son:

```text
CRM_BOOTSTRAP_USERNAME
CRM_BOOTSTRAP_PASSWORD
```

No poseen valores por defecto. Spring Security no genera una contraseña aleatoria porque el proyecto define su propio `UserDetailsService` vacío cuando no hay configuración.

Este mecanismo no es aceptable para producción. SEG-002 implementará usuarios persistentes, RBAC y una estrategia de autenticación adecuada.

## Autorización

Estado actual:

- `/actuator/health/**`: público;
- `/api/**`: autenticado;
- `/swagger-ui/**` y `/v3/api-docs/**`: autenticados;
- resto: denegado.

La matriz OWNER/ADMIN/SALES_MANAGER/SALES_REP/REVIEWER/READ_ONLY/SERVICE_ACCOUNT aún no está implementada.

## CSRF y CORS

La API es stateless y exige cabecera `Authorization` definida explícitamente por el cliente. `/api/**` se excluye de CSRF. Las demás rutas conservan la protección por defecto.

No se habilita CORS global. Desarrollo utiliza proxy de Vite sobre el mismo origen lógico. Cualquier apertura futura de CORS debe definir orígenes exactos por ambiente; nunca `*` con credenciales.

## Secretos

Prohibido versionar:

- contraseñas;
- tokens OAuth;
- refresh tokens;
- cookies;
- claves privadas;
- JSON de cuentas de servicio;
- credenciales de base de datos productiva;
- claves de API;
- secretos Terraform.

Desarrollo utiliza `.env` ignorado. Producción utilizará Secret Manager.

## Datos personales

El repositorio público no contiene el lote real. Las fixtures utilizan dominios `.test` y nombres ficticios.

La auditoría:

- no registra contraseñas ni tokens;
- no registra correos o teléfonos completos en payloads de exclusión;
- almacena huellas SHA-256 cuando necesita correlación;
- puede conservar IDs lógicos y datos comerciales no sensibles.

La API de exclusiones sí devuelve el valor normalizado a usuarios autenticados porque la operación lo necesita. SEG-002 deberá restringirla por rol.

## Archivos

Controles actuales de importación:

- extensiones CSV/XLSX;
- máximo 10 MB;
- procesamiento en memoria;
- parser por encabezados;
- SHA-256;
- confirmación adicional para ejecución;
- nombres de archivo tratados como metadatos, no rutas;
- ninguna escritura arbitraria en filesystem.

Pendiente:

- validación MIME por contenido;
- protección antivirus si se aceptan adjuntos de terceros;
- límites por usuario/IP;
- almacenamiento aislado de adjuntos;
- políticas de retención.

## Inyección y salida

- JPA y JdbcTemplate usan parámetros enlazados;
- no se concatenan valores de usuario en SQL;
- React escapa contenido por defecto;
- no se utiliza `dangerouslySetInnerHTML`;
- el preview de plantillas HTML deberá sanitizarse antes de incorporarse.

## SSRF

No existe todavía descarga de URLs externas. Cuando se implemente enriquecimiento o Drive:

- usar allowlists de hosts/protocolos;
- bloquear direcciones privadas y metadata endpoints;
- resolver DNS de forma segura;
- limitar redirecciones, tamaño y tiempo;
- no aceptar URLs arbitrarias en workers privilegiados.

## Seguridad de envío

Guardas actuales:

```text
sending.enabled=false
sending.dry-run=true
sending.daily-limit=0
sending.environment-kill-switch=true
system_setting sending.kill-switch=true
```

Además, no hay código Gmail ni SMTP. Cambiar variables no puede enviar nada.

## Pendientes prioritarios

1. usuarios persistentes y RBAC;
2. hashing de contraseñas y rotación bootstrap;
3. rate limiting;
4. correlation ID y redacción central de logs;
5. análisis OWASP/SpotBugs;
6. escaneo de contenedor;
7. políticas IAM y Secret Manager;
8. revisión de dependencias frontend con lockfile;
9. cabeceras de seguridad del frontend desplegado;
10. pruebas de autorización por rol.
