# Seguridad de campañas

## Estado

No existen campañas ejecutables ni adaptadores de envío en SEG-001. Este documento define condiciones obligatorias para segmentos posteriores.

## Guardas acumulativas

Un envío solo podrá reservarse cuando todas sean verdaderas:

1. `sending.enabled=true`;
2. `sending.dry-run=false`;
3. kill switch ambiental desactivado;
4. kill switch persistente desactivado;
5. campaña `APPROVED`;
6. destinatario aprobado;
7. canal válido y normalizado;
8. sin exclusión;
9. sin contacto previo incompatible;
10. sin reserva/comunicación equivalente;
11. plantilla válida y sin tokens pendientes;
12. adjuntos presentes, MIME/tamaño/hash válidos;
13. dentro de ventana horaria y día habilitado;
14. cuota diaria disponible;
15. reserva idempotente obtenida;
16. integración saludable;
17. tasa de rebotes/bajas debajo del umbral.

La falla de cualquier condición produce rechazo explícito y auditoría. Nunca una degradación permisiva.

## Configuración inicial futura

- 10 envíos diarios;
- concurrencia 1;
- intervalos variables;
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

Después de Gmail:

```text
Enviado sin rebote registrado hasta el momento
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
