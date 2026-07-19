# Despliegue

## Estado

No existe despliegue configurado. SEG-001 solo produce imágenes y configuración local. Terraform y Cloud Run pertenecen a segmentos posteriores.

## Ambientes

### Local

- PostgreSQL en Docker Compose;
- backend Spring Boot;
- frontend Vite;
- credenciales bootstrap locales;
- datos ficticios o archivos operativos fuera de Git.

### Test

- creado por Testcontainers;
- efímero;
- sin secretos externos;
- sin Google APIs.

### Staging

Planeado:

- Cloud Run backend/workers;
- Cloud SQL PostgreSQL;
- frontend servido por origen controlado;
- Secret Manager;
- OAuth/Workspace de staging;
- Cloud Tasks/PubSub/Scheduler;
- límites de envío en cero;
- datos ficticios o anonimizados.

### Production

Planeado y manual:

- aprobación de ambiente GitHub;
- Terraform plan revisado;
- migración validada en staging;
- backup previo;
- despliegue gradual;
- verificación de health/readiness;
- rollback documentado;
- envío deshabilitado salvo autorización independiente.

## Imagen actual

```bash
docker build -t gestudio-crm:local .
```

La imagen:

- compila con Maven/JDK;
- ejecuta sobre JRE 21;
- usa usuario no root;
- expone 8080;
- contiene solo backend.

El frontend todavía se construye por separado.

## Variables mínimas

```text
DATABASE_URL
DATABASE_USER
DATABASE_PASSWORD
CRM_BOOTSTRAP_USERNAME
CRM_BOOTSTRAP_PASSWORD
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

En cloud, ninguna credencial se define en Dockerfile, Terraform state sin cifrar o variables públicas.

## Gate de despliegue

1. CI verde;
2. análisis estático verde;
3. escaneo de dependencias e imagen;
4. migraciones probadas;
5. Terraform validate/plan;
6. secretos existentes;
7. backup y rollback;
8. aprobación manual;
9. kill switch activo;
10. monitoreo listo.

## Migraciones

Flyway debe ejecutarse una sola vez de manera coordinada. Antes de producción se definirá si migra al iniciar Cloud Run o mediante job separado; no habilitar múltiples instancias concurrentes sin revisar locking y tiempos de migración.

## Rollback

- revertir aplicación a imagen anterior;
- no revertir migraciones destructivas automáticamente;
- usar migración forward-fix;
- restaurar backup solo ante corrupción y con runbook aprobado;
- conservar auditoría del incidente.
