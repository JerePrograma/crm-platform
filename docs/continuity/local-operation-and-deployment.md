# Operación local, validación y despliegue

Actualizado: 2026-10-14

## Uso local de Scalaris para Marketing

La publicación GitHub actual está `AUTHORIZED_IN_PROGRESS`, con CI `PENDING_POST_PUSH`: el usuario autorizó add, revisión de diff, commit y push. La autoridad de esa fase es [publicación GitHub Scalaris](../validation/scalaris-github-publication-2026-10-14.md). Esta autorización no habilita PR, producción ni comunicaciones reales; el resultado remoto todavía debe verificarse.

La guía vigente es [Scalaris CRM para Marketing](../scalaris-marketing-local.md). Distingue dos modalidades:

| Modalidad | Abrir | Datos privados | Requisitos |
|---|---|---|---|
| Paquete Windows | Extraer ZIP completo y ejecutar `AbrirCRMScalarisPortable.cmd` | `%LOCALAPPDATA%\ScalarisCRM\portable` | Windows de 64 bits; Java 21, PostgreSQL 18 y Node 24 incluidos |
| Docker desde repositorio | `AbrirCRMScalaris.cmd` | `%LOCALAPPDATA%\ScalarisCRM`; PostgreSQL en volumen propio | Docker Desktop y contenedores Linux |

Los archivos `Detener...cmd` conservan la base y `Respaldar...cmd` crean copias privadas con checksum. La pantalla se abre después de comprobar salud y escucha en loopback, habitualmente puerto 8787; la primera configuración elige puertos libres cuando sea necesario. El acceso inicial se guarda en un archivo privado y separa Marketing de administración.

El camino Docker utiliza `docker-compose.scalaris.yml` y proyecto `scalaris-crm-local`, sin compartir el volumen del CRM histórico. El paquete portable incluye su propio runtime y guarda los datos fuera de la aplicación. Copiar el ZIP no transfiere contactos ni reemplaza respaldar la instalación.

El backend usa `CRM_BUSINESS_PROFILE=SCALARIS` y `SPRING_PROFILES_ACTIVE=scalaris`. Ninguna modalidad local conecta proveedores ni abre las guardas. No regenerar claves si ya existe una base y falta la configuración: recuperar la copia privada.

SCALARIS-001 tiene operación portable local aprobada con Java 21/PostgreSQL 18/Node 24: 18 comprobaciones reales PowerShell 5.1, incluida primera apertura, Marketing, loopback, persistencia, checksum, recuperación en base nueva y conservación de la anterior. Backend aprobó 62 unitarias y 6 de integración focalizada; frontend, 16 unitarias, typecheck/build/audit sin vulnerabilidades y E2E conjunto 4/4 sin omitidos, inesperados ni flaky. La fuente local está congelada y el ZIP definitivo tiene manifiesto/checksum revisados, 2.243 entradas y cero archivos privados.

La instalación de uso está activa en `http://127.0.0.1:8787`, con perfil Scalaris/rol Marketing correctos, cero prospectos, 18 etiquetas y 7 plantillas; todo escucha en loopback. QA quedó detenido. Evidencias locales: `validation-output/scalaris-release-latest.json` y `validation-output/scalaris-installed-local.json`. El siguiente paso es uso/piloto local; no recrear el paquete ni importar contactos reales automáticamente.

El launcher Docker aprobó 55 aserciones, con respaldo simulado. Docker/Compose real y el validador integral histórico están bloqueados porque Docker no está instalado. Evidencia portable ignorada por Git: `validation-output/scalaris-portable-latest.json`; manual revisado visualmente en desktop/móviles de 1440/390/320 px.

La recuperación portable crea una base nueva, verifica el checksum y conserva la anterior. El responsable técnico debe comprobar el resultado y las credenciales restauradas. Conservar paquete anterior, configuración y respaldo antes de actualizar; un cambio de versión mayor de PostgreSQL exige migración revisada.

## Requisitos técnicos del desarrollo y validación históricos

- Git;
- Docker Desktop o Docker Engine con Compose;
- Java 21 para ejecución separada;
- Node.js 22 y npm;
- PowerShell 5.1+ en Windows o Bash en Unix.

## Inicio seguro

```powershell
git status --short
git branch --show-current
git remote -v
git fetch origin
git switch main
git pull --ff-only origin main
git rev-parse HEAD
git rev-parse origin/main
```

El validador integral exige `main` y preservación del working tree. En la edición Scalaris actual hay cambios locales: no cambiar de rama ni actualizar sobrescribiéndolos. Detenerse ante conflicto, divergencia o remoto inesperado.

## Self-test del parser de contenedores

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-powershell-syntax.ps1
powershell -ExecutionPolicy Bypass -File scripts/test-container-env-assertions.ps1
```

Unix:

```bash
node scripts/test-container-env-assertions.js
```

Los self-tests verifican:

- las siete guardas presentes;
- una guarda ausente;
- un valor inseguro alternativo;
- JSON inválido;
- líneas vacías alrededor del JSON.

## Validadores canónicos

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-complete-crm.ps1
```

Unix:

```bash
bash scripts/validate-complete-crm.sh
```

O:

```bash
make validate-complete-crm
```

El cierre exige dos corridas limpias consecutivas sobre el mismo commit.

## Parseo de `.Config.Env`

Los validadores capturan:

```text
docker inspect <container> --format {{json .Config.Env}}
```

Contrato:

1. entrada no vacía;
2. JSON válido;
3. raíz de tipo array;
4. entradas convertidas a string sin alterar nombre ni valor;
5. membresía exacta;
6. fallo si falta una guarda;
7. no imprimir el entorno completo.

No usar regex, `-match` o `grep` sobre el transcript completo para decidir el gate de seguridad.

## Perfil productivo local

El perfil productivo local es un smoke técnico, no un despliegue. Debe comprobar:

- PostgreSQL, backend y frontend saludables;
- filesystem y usuarios endurecidos;
- PostgreSQL sin puerto público;
- siete variables fail-closed exactas;
- cero estados `SENT`, `DELIVERED` o `READ`;
- cleanup exclusivo de sus recursos sintéticos.

```text
PRODUCTION_PROFILE=EXECUTED_PASS_LOCALLY
```

no significa producción desplegada.

## Guardas

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
MESSAGING_REAL_NETWORK_ALLOWED=false
EMAIL_PROVIDER_MODE=NOOP
WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY
```

## Evidencia válida

Debe incluir:

- commit exacto;
- rama `main`;
- códigos de salida;
- JSON de resumen;
- fases ejecutadas y no ejecutadas;
- `productionProfileSmoke`;
- `finalTreeClean`;
- puertos e IDs de imágenes cuando corresponda;
- cleanup;
- estado de envíos;
- repository safety;
- `git diff --check`;
- estado de CI.

`validation-output/` y los ZIP son evidencia local y no deben versionarse.

## Reutilización

Solo omitir una fase cuando:

1. existe evidencia estructurada íntegra;
2. el commit/tree cubierto es identificable;
3. no cambiaron archivos ni dependencias de la fase;
4. el entorno es equivalente;
5. la omisión queda registrada.

Los cambios Scalaris de octubre no están cubiertos por la validación del 24/30 de julio. El recorrido portable se registra con su runtime propio; el cierre integral del repositorio requiere repetir sus fases sobre el commit definitivo y Docker disponible.

## Despliegue

Producción continúa:

```text
NOT_AUTHORIZED / NOT_DEPLOYED
```

Antes de cualquier despliegue se requieren infraestructura, TLS, secretos administrados, backup/restore, rollback, privacidad, observabilidad, carga, accesibilidad, providers revisados, CI verde y autorización explícita.

No desplegar como consecuencia automática de una validación local.
