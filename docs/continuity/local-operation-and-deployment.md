# Operación local, validación y despliegue

Actualizado: 2026-07-24

## Requisitos

- Git;
- Docker Desktop o Docker Engine con Compose;
- Java 21 para ejecución separada;
- Node.js 22 y npm para frontend local;
- PowerShell 5.1+ en Windows o Bash en Unix.

El recorrido completamente contenedorizado no requiere Java, Maven o Node en el host.

## Inicio seguro

```powershell
git status --short
git branch --show-current
git remote -v
git fetch origin
git switch main
git pull --ff-only origin main
```

Detenerse si existen cambios no relacionados, conflicto, divergencia o un remoto inesperado.

## Documentación operativa existente

- `docs/local-development-and-usage.md`: procesos separados;
- `docs/containerized-quickstart.md`: recorrido Docker;
- `docs/production/`: contrato productivo local;
- `docs/runbooks/`: incidentes y recuperación;
- `docs/disaster-recovery.md`: continuidad;
- `docs/testing.md`: estrategia de pruebas.

No duplicar comandos cuando el repositorio ya tiene un script canónico.

## Validadores canónicos

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-complete-crm.ps1
```

Unix:

```bash
bash scripts/validate-complete-crm.sh
```

o:

```bash
make validate-complete-crm
```

Los comandos SEG-001 siguen disponibles para su alcance histórico, pero el cierre integral usa los validadores completos.

## Perfil productivo local

El perfil productivo local es un smoke técnico, no un despliegue. Debe verificar:

- PostgreSQL, backend y frontend saludables;
- imágenes exactas del candidato;
- filesystems, redes y usuarios endurecidos;
- PostgreSQL sin publicación pública;
- variables fail-closed;
- ausencia de credenciales de proveedores reales;
- cleanup exclusivo de los recursos creados por la corrida.

`PRODUCTION_PROFILE=EXECUTED_PASS_LOCALLY` no significa producción desplegada.

## Evidencia

La evidencia válida debe incluir:

- JSON de resumen;
- códigos de salida;
- commit y tree exactos;
- fases ejecutadas y omitidas;
- IDs de imágenes;
- puertos seleccionados;
- estado de envío;
- confirmación de checkout principal no modificado;
- confirmación de que no se realizó push.

`validation-output/` y ZIP de evidencia son artefactos locales; no deben versionarse.

## Reutilización de validaciones

Se puede omitir una fase solo cuando:

1. existe evidencia estructurada íntegra;
2. el commit/tree cubierto es identificable;
3. no cambiaron archivos, dependencias ni configuración de esa fase;
4. el entorno relevante es equivalente;
5. el script registra explícitamente qué se omitió y por qué.

Si una corrección modifica el código cubierto, volver a ejecutar la prueba específica y las validaciones generales razonables.

## Despliegue

Producción continúa `NOT_AUTHORIZED / NOT_DEPLOYED`.

Antes de cualquier despliegue real se requieren:

- infraestructura y dominio aprobados;
- TLS;
- secretos gestionados;
- backup/restore en el entorno objetivo;
- rollback probado;
- privacidad y retención;
- observabilidad y alertas;
- carga y accesibilidad manual;
- proveedores revisados en fase separada;
- CI verde sobre el commit exacto;
- autorización explícita.

No desplegar como consecuencia automática de una validación local.
