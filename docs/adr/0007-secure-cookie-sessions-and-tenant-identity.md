# ADR-0007 — Sesión web segura e identidad por organización

- Estado: Accepted
- Fecha: 2026-07-21

## Contexto

SEG-001 usaba HTTP Basic con un usuario en memoria. Ese mecanismo no permitía
administrar usuarios, invalidar acceso, auditar intentos ni separar datos entre
organizaciones. El frontend es same-origin detrás del proxy Nginx existente.

## Decisión

- persistir usuarios, roles, permisos y membresías en PostgreSQL;
- crear el primer administrador solo cuando no existe otro administrador y las
  dos credenciales bootstrap están configuradas;
- usar el codificador delegado de Spring Security y exigir una política mínima
  configurable;
- autenticar mediante sesión de servidor y cookie `HttpOnly`, `SameSite=Lax`,
  `Secure` obligatoria en producción;
- rotar el identificador de sesión al autenticar, expirar por inactividad a las
  ocho horas e invalidar al cerrar sesión o cambiar la contraseña;
- exigir CSRF por token/cookie en toda mutación;
- verificar contra PostgreSQL en cada request autenticado que usuario,
  membresía y hash de contraseña continúan vigentes;
- transportar `organization_id`, usuario, rol y permisos en el principal, pero
  filtrar siempre los datos en backend por organización;
- responder `401` a falta de autenticación, `403` a permiso insuficiente y
  ocultar recursos de otro tenant con `404`.

El frontend no conserva contraseña ni tokens en `localStorage` o
`sessionStorage`. Solo mantiene en memoria el perfil no sensible retornado por
`/api/v1/auth/me`.

## Consecuencias

- desactivar un usuario o cambiar su contraseña invalida también sesiones ya
  emitidas;
- los procesos internos sin sesión usan únicamente la organización bootstrap
  explícita; los endpoints HTTP permanecen protegidos;
- cada tabla de dominio existente tiene `organization_id`, FK e índice;
- agregar una organización requiere crear sus roles controlados antes de crear
  membresías;
- el costo de una consulta liviana de vigencia por request se acepta como
  control conservador inicial y debe medirse antes de introducir cache.

## Rollback

La migración V6 no se revierte automáticamente. Ante rollback de aplicación se
conservan columnas/tablas nuevas y se restaura el binario anterior solo sobre
un backup validado. Eliminar identidad o `organization_id` perdería trazabilidad
y requiere una migración forward específica, nunca edición de V1–V6.
