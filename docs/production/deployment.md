# Procedimiento de release

Destino preparado: [Hostinger y crm.scalaris.com.ar](hostinger.md). La preparación local de archivos no publica la aplicación ni autoriza el corte.

1. Congelar el commit revisado, completar CI y scans sin High/Critical pendientes y conservar evidencia sanitizada del SHA. Ejecutar dos recorridos integrales limpios sobre el mismo commit cuando cambien las fases cubiertas.
2. Construir ambas imágenes sin caché, registrar IDs/digests, SBOM y resultados de escaneo de backend y frontend. nginx está fijado a `1.30.6-alpine` por digest y se retira curl/libcurl del runtime porque no se utilizan; verificar avisos y scanners al liberar, sin asumir seguridad permanente por una versión.
3. Probar en entorno sintético Scalaris, roles, login/logout/CSRF, cookies, importación >1 MiB y límite de 11 MiB de solicitud, HTTPS, cabeceras no confiables y recuperación de una copia.
4. Preparar secretos externos, almacenamiento persistente, dominio/TLS, respaldo verificado, imagen anterior y política de privacidad/retención. No conectar proveedores ni importar contactos por efecto de la instalación.
5. Ejecutar Flyway V1–V14 de forma coordinada con un único escritor. En este perfil una sola instancia backend migra al iniciar; no escalar antes de revisar locks, tiempos y sesiones. Una actualización exige respaldo previo y ensayo con una copia aislada compatible.
6. Arrancar únicamente el proyecto CRM, exigir PostgreSQL/backend/frontend saludables y revisar las guardas antes de publicar la ruta del edge. Validar Caddy completo antes de su recarga; preservar los archivos de las otras aplicaciones.
7. Tras el corte autorizado, verificar DNS/certificado, redirección HTTPS, cookies Secure/HttpOnly/SameSite, aislamiento, roles, errores, cero estados `SENT|DELIVERED|READ`, timers y copia recuperable. Registrar también resultados negativos o gates externos pendientes.

Pruebas focalizadas:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/test-database-operations.ps1
node scripts/test-production-proxy.js
```

```sh
sh scripts/test-database-operations.sh
node scripts/test-production-proxy.js
```

La regresión de proxy usa nginx y Caddy reales, backend sintético, red/CA desechables y ningún puerto publicado. No demuestra login con usuarios productivos ni un certificado público. Los ensayos de backup con Docker simulado prueban orquestación; una restauración real sigue siendo un gate separado.

Fuentes de actualización: [nginx estable](https://nginx.org/en/download.html), [avisos de seguridad](https://nginx.org/en/security_advisories.html), [catálogo oficial de imágenes](https://github.com/docker-library/official-images/blob/master/library/nginx). Digest consultado al preparar este release: `sha256:c59d1f8b1e0e42da75be973424cd72e23f7f8adfb54e982f4d1cd0216a5a5576`.
