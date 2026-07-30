# Seguridad de campañas

## Estado

SEG-001 implementa campañas `SIMULATION|LIVE` y Gmail exclusivamente mediante
el outbox existente. Google real no está conectado y los defaults mantienen
LIVE bloqueado. Estas condiciones son guardas efectivas, no un diseño futuro.

## Guardas acumulativas

Un envío solo podrá reservarse cuando todas sean verdaderas:

1. `sending.enabled=true`;
2. `sending.dry-run=false`;
3. `MESSAGING_REAL_NETWORK_ALLOWED=true`;
4. provider Gmail explícito;
5. kill switch ambiental desactivado;
6. kill switch persistente desactivado;
7. campaña LIVE aprobada y audiencia congelada;
8. sender `CONNECTED`;
9. actor con `MESSAGE_SEND`;
10. confirmación `SEND_LIVE_CAMPAIGN`;
11. destinatario asociado, canal válido, consentimiento no `DENIED`;
12. sin exclusión, baja ni rebote permanente conocido;
13. plantilla válida y baja visible;
14. dentro de ventana horaria y día habilitado;
15. cuota diaria positiva y disponible, intervalo y concurrencia respetados;
16. reserva idempotente y lease obtenidos;
17. integración saludable.

La falla de cualquier condición produce rechazo explícito y auditoría. Nunca una degradación permisiva.

## Configuración inicial

- 10 envíos diarios;
- concurrencia 1;
- intervalo configurable, sin jitter humano;
- 09:30–17:30;
- `America/Argentina/Buenos_Aires`;
- lunes a viernes;
- aumento gradual manual;
- pausa automática por anomalías.

## Pausas automáticas

- autenticación Gmail fallida;
- cuota cercana al límite;
- rebotes por encima del umbral;
- incremento de bajas;
- errores consecutivos;
- adjunto faltante o cambiado;
- plantilla inválida;
- kill switch;
- inconsistencia de idempotencia;
- reconciliación pendiente fuera de SLA.

## Idempotencia

Restricción conceptual:

```text
UNIQUE(campaign_id, prospect_id, template_version, communication_type)
```

Además:

- clave idempotente externa;
- reserva transaccional;
- locking;
- intentos persistentes;
- reconciliación Gmail;
- auditoría;
- recuperación de caída después de enviar y antes de confirmar DB.

## Exclusiones

La exclusión domina campañas, seguimientos y reintentos. Antes de cada intento se vuelve a consultar, no se confía en una evaluación vieja.

## Estado de entrega

Después de aceptación por Gmail:

```text
Aceptado por Gmail
```

No usar “entregado” sin evidencia técnica suficiente.

## Prohibiciones

- BCC para simular mensajes individuales;
- técnicas para evadir filtros antispam;
- rotación engañosa de dominios;
- contenido oculto;
- compra de listas;
- recontactar bajas;
- ignorar rebotes permanentes;
- habilitación mediante un solo flag;
- producción automática desde `main`.
