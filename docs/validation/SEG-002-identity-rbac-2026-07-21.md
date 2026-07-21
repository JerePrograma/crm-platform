# Validación SEG-002 — identidad, sesión, RBAC y tenant

Fecha: 2026-07-21

Plataforma: Windows 11, PowerShell, Java 21 del wrapper, Docker Desktop 29.3.1,
PostgreSQL 17.

Commit funcional: `0546e6ed8818b627d25982d7758b43181e5f4ce5`

## Resultado

```text
SEG_002=EXECUTED_PASS
REAL_SENDING=DISABLED_BY_POLICY
PRODUCTION=NOT_DEPLOYED
```

## Validaciones ejecutadas

| Comando | Resultado |
|---|---|
| `mvn -Dtest=ProspectPersistenceIntegrationTest test` | 3/3; Flyway V1–V6 y Hibernate validate PASS |
| `mvn -Dtest=SecurityAuthorizationIntegrationTest test` | 10/10 PASS |
| `mvn -Dtest=ArchitectureTest,SecurityAuthorizationIntegrationTest test` | 11/11 PASS |
| `mvn verify` | 36/36; ArchUnit y Spotless PASS |
| `npm ci` | 24 paquetes según lockfile, PASS |
| `npm run build` | TypeScript strict + Vite, PASS |
| `docker compose --profile app up -d --build` | Maven/Vite PASS; export backend falló una vez por snapshot de Docker |
| `docker compose build backend` | reintento acotado PASS |
| `docker compose --profile app up -d` | PostgreSQL/backend/frontend healthy |
| `scripts/smoke-test.ps1` | cookie/CSRF/API/frontend PASS |
| `docker compose ... run --rm smoke` | health/CSRF/login/API/frontend PASS |
| `scripts/check-repository-safety.ps1` | PASS |
| `scripts/check-powershell-syntax.ps1` | 11 scripts PASS |
| `bash -n scripts/smoke-test.sh` | PASS |
| `docker compose config --quiet` | PASS |

Puertos del smoke integrado: PostgreSQL `25432`, backend `8080`, frontend
`5173`, vinculados a loopback según Compose/.env local.

## Contratos demostrados

- bootstrap solo con variables completas y ausencia de administradores;
- contraseña no almacenada en claro;
- login válido e inválido, bloqueo tras cinco intentos, logout y expiración;
- CSRF requerido y rotación de sesión al autenticar;
- `401` sin sesión, `403` sin permiso y `404` cross-tenant;
- roles ADMIN, MANAGER, SALES y VIEWER con permisos persistentes;
- desactivación y cambio de contraseña invalidan sesiones existentes;
- login/fallo/logout/usuario/contraseña quedan auditados;
- frontend no almacena credenciales ni usa HTTP Basic;
- migración aditiva sobre V1–V5 y desde esquema vacío.

## Fallos corregidos durante el checkpoint

1. Falta de authentication entry point producía `403` anónimo; corregido a
   `401` explícito.
2. Una dependencia de infraestructura de sesión vivía en el controller y rompía
   ArchUnit; se encapsuló en `AuthSessionService`.
3. El smoke contenedor fijaba un nombre de header CSRF distinto del contrato;
   ahora usa `headerName` retornado por `/api/v1/auth/csrf`.
4. Docker Desktop perdió una capa padre al exportar; el reintento puntual pasó
   sin prune ni borrado de volúmenes.

## Advertencias no bloqueantes

- Spring advierte que serializar `PageImpl` directamente no es un contrato JSON
  estable; SEG-003 reemplazará esas respuestas por DTO paginado explícito.
- Mockito advierte sobre carga dinámica futura del agente; no afecta JDK 21
  actual y deberá fijarse antes de subir de JDK.
- existen avisos de API deprecada ya identificados; no hubo error de compilación.
