# Etiquetas operativas de Gmail para Gestudio

## Objetivo

Definir una estructura única y verificable para clasificar comunicaciones comerciales de Gestudio en Gmail y facilitar su conciliación posterior con el CRM.

## Alcance

Esta estructura organiza mensajes de la cuenta remitente conectada. No modifica estados del CRM ni habilita envíos reales desde la aplicación.

## Estructura canónica

### Flujo comercial

- `Gestudio/00 Control`: verificaciones internas y mensajes técnicos.
- `Gestudio/01 Pendiente de envío`: contacto preparado pero todavía no enviado.
- `Gestudio/02 Enviado`: mensaje aceptado por Gmail para envío. No implica entrega final.
- `Gestudio/03 Respondido`: existe una respuesta entrante relacionada con la prospección.
- `Gestudio/04 Interesado`: respuesta humana con interés comercial verificable.
- `Gestudio/05 Seguimiento`: conversación que requiere una próxima acción.
- `Gestudio/06 Demo activa`: se facilitó acceso y la evaluación sigue abierta.
- `Gestudio/07 Propuesta`: se envió o solicitó una propuesta comercial concreta.
- `Gestudio/08 Cerrado`: oportunidad terminada, con o sin venta.
- `Gestudio/09 No contactar`: baja explícita, contacto incorrecto o exclusión permanente.

### Errores

- `Gestudio/Errores/Límite de Gmail`: Gmail indicó que el mensaje no se envió por cuota o límite.
- `Gestudio/Errores/Rebote definitivo`: dirección inexistente, dominio inválido o rechazo permanente equivalente.
- `Gestudio/Errores/Bloqueado`: mensaje rechazado por políticas antispam o bloqueo del proveedor.

### Entrantes

- `Gestudio/Entrantes/No leído`: respuesta entrante todavía no revisada.
- `Gestudio/Entrantes/Leído`: respuesta entrante revisada.
- `Gestudio/Entrantes/Respuesta automática`: vacaciones, cierre temporal, autorespuesta o ticket automático.

## Reglas de interpretación

1. `Enviado` significa que Gmail creó el mensaje saliente; no prueba que el destinatario lo haya recibido.
2. Gmail personal no expone confirmación fiable de apertura. Las etiquetas `Leído` y `No leído` describen el estado del mensaje entrante dentro de la cuenta, no la apertura del correo por el prospecto.
3. Un mensaje con `Límite de Gmail`, `Rebote definitivo` o `Bloqueado` no debe contabilizarse como entrega.
4. `Respondido` puede coexistir con `Respuesta automática`. Solo una respuesta humana relevante debe promoverse a `Interesado`.
5. `No contactar` prevalece sobre cualquier estado comercial y debe bloquear futuros envíos.
6. Los estados comerciales avanzados deben aplicarse después de leer el hilo completo.

## Transiciones recomendadas

```text
Pendiente de envío
  -> Enviado
  -> Respondido
      -> Interesado
          -> Seguimiento
          -> Demo activa
          -> Propuesta
          -> Cerrado

Enviado
  -> Límite de Gmail | Rebote definitivo | Bloqueado

Cualquier estado
  -> No contactar
```

## Consultas operativas útiles

```text
label:"Gestudio/01 Pendiente de envío"
label:"Gestudio/02 Enviado" -label:"Gestudio/03 Respondido"
label:"Gestudio/03 Respondido" is:unread
label:"Gestudio/04 Interesado"
label:"Gestudio/05 Seguimiento"
label:"Gestudio/Errores/Límite de Gmail"
label:"Gestudio/Errores/Rebote definitivo"
label:"Gestudio/Errores/Bloqueado"
label:"Gestudio/09 No contactar"
```

## Conciliación con el CRM

Al sincronizar manualmente o mediante una integración futura:

- usar el correo normalizado como clave de correlación secundaria;
- conservar `message_id`, `thread_id`, fecha, remitente, destinatario y etiquetas;
- no inferir entrega a partir de `SENT`;
- no inferir interés a partir de una respuesta automática;
- registrar el motivo exacto de rebote o bloqueo;
- aplicar exclusiones de forma fail-closed;
- evitar guardar cuerpos completos cuando no sean necesarios;
- no almacenar credenciales de la demo ni datos sensibles en Git.

## Automatización futura

Una integración segura debería procesar eventos de forma idempotente y mantener una tabla de correspondencia entre etiquetas Gmail y estados del CRM. Antes de habilitarla deben revisarse autorización, aislamiento por organización, deduplicación, cuotas, reintentos, auditoría y bajas.
