# Política de seguridad

## Versiones soportadas

El proyecto está en desarrollo inicial. Solo la rama activa indicada en `docs/status.md` recibe correcciones. No existe una versión productiva soportada todavía.

## Reporte de vulnerabilidades

No publicar vulnerabilidades, secretos ni datos personales en issues públicos.

Utilizar un canal privado del propietario del repositorio y proporcionar:

- descripción técnica;
- componente y commit afectados;
- pasos mínimos de reproducción;
- impacto;
- evidencia sin datos reales;
- propuesta de mitigación si existe.

No incluir tokens, credenciales ni exportaciones comerciales.

## Respuesta

Una vulnerabilidad confirmada debe:

1. activar kill switches cuando afecte comunicaciones o datos;
2. bloquear despliegues;
3. rotar credenciales comprometidas;
4. preservar evidencia;
5. corregirse en rama privada si la publicación anticipada aumenta el riesgo;
6. incorporar pruebas de regresión;
7. documentar impacto y recuperación;
8. divulgarse responsablemente cuando corresponda.

## Alcance sensible

Prioridad crítica:

- bypass de autenticación/autorización;
- exposición de prospectos o conversaciones;
- habilitación no autorizada de envíos;
- duplicación masiva de comunicaciones;
- ejecución remota;
- SQL injection;
- SSRF hacia metadata cloud;
- robo de OAuth refresh tokens;
- acceso indebido a Gmail/Drive/Sheets;
- escalada IAM;
- filtración de secretos en logs o CI.

## Reglas de investigación

- usar únicamente datos ficticios;
- no enviar correos reales;
- no probar contra instituciones o terceros;
- no degradar disponibilidad;
- no extraer información más allá de la prueba mínima.

## Estado de envío

El código actual no contiene adaptadores Gmail/SMTP. La configuración debe permanecer:

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```
