# ADR-0001 — Monolito modular

- Estado: Accepted
- Fecha: 2026-07-19

## Contexto

El CRM requiere numerosos módulos e integraciones, pero todavía no tiene volumen, equipos ni límites operativos que justifiquen microservicios.

## Decisión

Usar un único despliegue Spring Boot con módulos internos explícitos y PostgreSQL compartido. Los casos de uso y adaptadores deben conservar límites que permitan extraer workers futuros.

## Consecuencias

Positivas:

- transacciones simples;
- despliegue y desarrollo local menos costosos;
- observabilidad unificada;
- refactors de dominio directos.

Negativas:

- aislamiento de fallos menor;
- disciplina arquitectónica obligatoria;
- escalado por módulo no disponible inicialmente.

## Revisión

Reevaluar ante necesidades demostradas de aislamiento, permisos, escalado o frecuencia de despliegue distintos.
