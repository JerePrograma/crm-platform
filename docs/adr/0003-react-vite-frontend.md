# ADR-0003 — React, TypeScript y Vite

- Estado: Accepted
- Fecha: 2026-07-19

## Contexto

El CRM necesita tablas, filtros, importaciones, previews, Kanban futuro y operación responsive. Un frontend server-side agregaría fricción a interacciones ricas.

## Decisión

Usar React con TypeScript estricto y Vite. Mantener el frontend como cliente de `/api/v1` y evitar lógica comercial crítica en el navegador.

## Consecuencias

- UI rica y modular;
- build separado;
- contratos API tipados manualmente por ahora;
- necesidad de estrategia de autenticación segura;
- dependencia de Node y gestión de lockfile;
- posibilidad futura de generar clientes desde OpenAPI.
