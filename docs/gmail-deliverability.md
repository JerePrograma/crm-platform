# Gmail y entregabilidad

## Estado

No existe envío en SEG-001. Este documento establece prerrequisitos para no diseñar el adaptador aislado de reputación y cumplimiento.

## Autenticación de dominio

Antes del piloto:

- SPF correcto y sin demasiados lookups;
- DKIM habilitado y rotación de claves;
- DMARC inicialmente en monitoreo, luego política revisada;
- alineación del dominio visible;
- TLS;
- monitoreo de reportes DMARC;
- dirección de respuesta válida.

## Contenido

Cada correo deberá incluir:

- texto plano;
- HTML sobrio y equivalente;
- identidad clara del remitente;
- motivo contextual del contacto;
- opción visible de baja;
- datos de contacto;
- adjuntos estrictamente necesarios.

No usar:

- texto oculto;
- asuntos engañosos;
- tracking invasivo no autorizado;
- acortadores dudosos;
- adjuntos ejecutables;
- contenido diseñado para evadir filtros.

## Cabeceras

Futuro:

```text
List-Unsubscribe: <mailto:...>, <https://...>
List-Unsubscribe-Post: List-Unsubscribe=One-Click
```

La baja debe impactar inmediatamente la tabla de exclusiones.

## Volumen inicial

- 10 mensajes diarios;
- concurrencia 1;
- ventana 09:30–17:30;
- lunes a viernes;
- zona `America/Argentina/Buenos_Aires`;
- aumento manual solo con métricas saludables.

## Métricas

- enviados sin rebote registrado;
- rebote permanente/temporal;
- respuestas;
- bajas;
- quejas si están disponibles;
- errores de autenticación;
- cuota;
- latencia;
- reintentos;
- conversión.

## Pausas

Pausar ante:

- rebotes anómalos;
- incremento de bajas;
- autenticación fallida;
- cuota cercana al límite;
- errores consecutivos;
- adjunto faltante;
- plantilla inválida;
- kill switch;
- divergencia de reconciliación.

## Estados

Después de aceptar Gmail el mensaje:

```text
Enviado sin rebote registrado hasta el momento
```

No afirmar entrega al buzón sin evidencia suficiente.

## Rebotes y respuestas automáticas

Clasificar separadamente:

- rebote permanente;
- rebote temporal;
- out-of-office;
- auto-reply;
- respuesta humana;
- solicitud de baja.

Un rebote permanente o baja crea exclusión y cancela seguimientos.

## Reputación

- usar dominio y remitente consistentes;
- no comprar listas;
- validar pertinencia;
- personalizar con evidencia real;
- limitar frecuencia;
- respetar respuestas negativas;
- revisar manualmente segmentos iniciales;
- no rotar cuentas para eludir límites.
