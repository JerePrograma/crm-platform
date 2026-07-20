# Seguridad

## Postura actual

La plataforma opera con política fail-closed:

- sin credenciales bootstrap explícitas no existe usuario válido;
- health es público;
- API y OpenAPI requieren autenticación;
- cualquier otra ruta se deniega;
- sesiones HTTP stateless;
- credenciales frontend solo en memoria;
- envíos inexistentes y bloqueados;
- secretos y datos operativos excluidos de Git;
- puertos locales publicados solo en loopback.

## Autenticación temporal

SEG-001 utiliza HTTP Basic únicamente para desarrollo.

```text
CRM_BOOTSTRAP_USERNAME
CRM_BOOTSTRAP_PASSWORD
```

No tienen valores productivos por defecto. Sin ambas variables, la API de negocio queda cerrada.

HTTP Basic no es aceptable para producción. SEG-002 implementará usuarios persistentes, hashing, RBAC y una estrategia de autenticación adecuada.

## Autorización

Estado actual:

- `/actuator/health/**`: público;
- `/api/**`: autenticado;
- `/swagger-ui/**`: autenticado;
- `/v3/api-docs/**`: autenticado;
- resto: denegado.

La matriz de roles todavía no está implementada.

## CSRF y CORS

La API es stateless y exige `Authorization` explícita. `/api/**` se excluye de CSRF. Las demás rutas conservan protección por defecto.

No existe CORS global. Desarrollo utiliza proxy Vite/Nginx. Cualquier apertura futura debe definir orígenes exactos; nunca `*` con credenciales.

## Secretos prohibidos

No versionar:

- `.env`;
- contraseñas;
- tokens OAuth;
- refresh tokens;
- cookies;
- claves privadas;
- certificados privados;
- JSON de cuentas de servicio;
- client secrets;
- credenciales de base productiva;
- API keys;
- secretos Terraform;
- transcripts sin revisar.

Desarrollo utiliza `.env` ignorado. Producción futura utilizará Secret Manager.

## Escaneo centralizado del repositorio

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-repository-safety.ps1
```

Unix:

```bash
sh scripts/check-repository-safety.sh
```

Con Make:

```bash
make repository-safety
```

Bloquea archivos rastreados como:

- `.env` en cualquier subdirectorio;
- `validation-output/`;
- datos privados de importación salvo README;
- datos privados de exportación;
- `gestudio_lote_*_prospectos.xlsx`;
- `.pem`, `.key`, `.p12`, `.pfx`, `.jks`;
- JSON con nombres de credenciales, service account o client secret.

También ejecuta `git diff --check`.

Limitación: es un control por ruta/extensión, no un escáner de secretos por contenido. Debe complementarse con escaneo dedicado antes de producción.

## Datos personales

El repositorio público no contiene el lote real. Las fixtures utilizan dominios `.test` y nombres ficticios.

La auditoría:

- no registra contraseñas ni tokens;
- no registra canales completos en payloads de exclusión;
- usa SHA-256 cuando necesita correlación;
- puede conservar IDs lógicos y datos comerciales no sensibles.

La API de exclusiones devuelve valores normalizados a usuarios autenticados porque la operación lo requiere. SEG-002 deberá restringir por rol.

## Archivos de importación

Controles:

- extensiones CSV/XLSX;
- máximo 10 MB;
- procesamiento en memoria;
- parser por encabezados;
- SHA-256;
- confirmación adicional;
- basename saneado;
- sin rutas arbitrarias;
- lote real fuera de Git/CI/imágenes.

Pendiente:

- MIME por contenido;
- antivirus si se aceptan adjuntos externos;
- límites por usuario/IP;
- almacenamiento aislado;
- retención.

## Lockfile frontend

Generación:

```text
npm install --package-lock-only --ignore-scripts --no-audit --no-fund
```

Garantías del script:

- lifecycle scripts deshabilitados;
- `node_modules` no debe crearse;
- lockfile queda para revisión manual;
- no se realiza commit automático;
- Dockerfile/CI usan npm ci cuando el lockfile existe.

Pendiente:

- generar y revisar lockfile real;
- versionarlo;
- auditoría de dependencias;
- escaneo SCA continuo.

## Validación backend contenedorizada

Scripts:

```text
scripts/verify-backend-container.ps1
scripts/verify-backend-container.sh
```

El repositorio se monta en solo lectura y `backend/target` usa un volumen efímero.

Para Testcontainers se monta:

```text
/var/run/docker.sock
```

Riesgo: el socket Docker concede al contenedor capacidad equivalente a control elevado sobre el daemon y potencialmente el host.

Reglas:

- ejecutar únicamente desde `main`;
- exigir código propio y revisado;
- no usar imágenes Maven no fijadas o no confiables;
- no ejecutar sobre PRs externos sin aislamiento;
- no montar secretos adicionales;
- eliminar contenedor y volumen target al finalizar;
- conservar solamente la caché Maven esperada.

## Evidencia local

```text
validation-output/
```

Está ignorado por Git.

Los scripts no imprimen contraseñas, pero transcripts y logs pueden contener contexto técnico u operativo. Revisar antes de compartir.

La evidencia canónica debe resumirse en `docs/validation/`, no versionarse automáticamente.

## Inyección y salida

- JPA/JdbcTemplate usan parámetros enlazados;
- no se concatenan valores de usuario en SQL;
- React escapa contenido por defecto;
- no se usa `dangerouslySetInnerHTML`;
- futuros previews HTML deben sanitizarse.

## SSRF

No existe descarga de URLs externas. Cuando se implemente enriquecimiento o Drive:

- allowlists de hosts/protocolos;
- bloqueo de redes privadas y metadata;
- DNS seguro;
- límites de redirección, tamaño y tiempo;
- sin URLs arbitrarias en workers privilegiados.

## Seguridad de envío

Guardas:

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
system_setting sending.kill-switch=true
```

Además, no existe código Gmail ni SMTP. Cambiar variables no puede enviar nada mientras no exista adaptador.

## CI

CI ejecuta:

- seguridad del repositorio;
- preflight fail-closed;
- Maven verify;
- frontend typecheck/build;
- Compose/images/smoke;
- cleanup de volumen efímero.

Las credenciales CI son ficticias.

GitHub no expone actualmente runs visibles mediante el conector; no declarar CI verde sin evidencia.

## Pendientes prioritarios

1. usuarios persistentes y RBAC;
2. hashing y rotación bootstrap;
3. rate limiting;
4. correlation ID y redacción central de logs;
5. escaneo de secretos por contenido;
6. OWASP/SpotBugs;
7. escaneo de contenedores;
8. SCA frontend con lockfile;
9. IAM y Secret Manager;
10. cabeceras frontend productivas;
11. autorización por rol;
12. política de transcripts y retención de evidencia.
