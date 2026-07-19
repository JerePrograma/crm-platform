# ADR-0004 — Envío fail-closed

- Estado: Accepted
- Fecha: 2026-07-19

## Contexto

El CRM administrará comunicaciones comerciales. Un defecto de configuración podría producir contacto no autorizado, duplicados, reputación dañada o incumplimiento de bajas.

## Decisión

El envío requiere guardas acumulativas independientes. La configuración inicial es:

```text
sending.enabled=false
sending.dry-run=true
sending.daily-limit=0
kill switch ambiental=true
kill switch persistente=true
```

SEG-001 no contiene adaptador de envío.

## Consecuencias

- configurar una sola variable nunca habilita envío;
- campañas futuras deben aprobarse;
- toda guarda se reevalúa antes de cada intento;
- anomalías pausan automáticamente;
- producción requiere autorización explícita y manual.
