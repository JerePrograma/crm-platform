# SCALARIS-001 — validación de la entrega local para Marketing

Fecha: 14 de octubre de 2026, Argentina.

## Alcance y procedencia

Adaptación del CRM para **servicio técnico, desarrollo y venta a pedido** de Scalaris, alcance confirmado por el titular durante esta misión. La referencia comercial utilizada es la documentación local vigente del 14/10/2026: WhatsApp **11 7278-9289**, **Zona Oeste, CABA y Mar del Plata** y retiro/entrega a coordinar según el caso.

```text
rama: main
commit base: 75f37a023a0e69d991fe7640f6ed24df98d6ca18
estado del contenido: cambios locales, sin commit ni publicación
backend entregado SHA-256:
4171D23FAA13AD359B0F34308DD8CF585762AF180FA8466BC9CEC8C41ED61B01
frontend final: index-Cr-8R-6K.js / index-rXXdIgLR.css
runtime portable: Java 21.0.12.1 / PostgreSQL 18.6 / Node 24.21.0
```

Los hashes y `VERSION.json` identifican los binarios y archivos entregados. No se atribuyen los resultados de octubre al SHA histórico `d6bfe642` ni a su CI de julio. No se ejecutó una nueva CI ni se publicó Git.

## Resultados ejecutados

| Control | Resultado | Evidencia y límite |
|---|---|---|
| Backend sin Testcontainers | EXECUTED_PASS | `clean verify -Dtest=!*IntegrationTest`, 62/62; última repetición `test` sobre fuente final `tagId`, 62/62, sin fallos, errores ni omisiones |
| Perfil Scalaris en PostgreSQL | EXECUTED_PASS | 6/6 `BusinessProfileIntegrationTest` contra PostgreSQL 18.6 sintético, `verify`, empaquetado y formato aprobados |
| Flyway/Hibernate en runtime portable | EXECUTED_PASS | V1–V14 desde base privada nueva; Hibernate validate; no edición de migraciones históricas |
| Frontend instalación/tipos/unit/build | EXECUTED_PASS | `npm ci`, TypeScript, 16/16 Vitest y Vite build |
| Dependencias frontend | EXECUTED_PASS | 5 vulnerabilidades heredadas corregidas con actualizaciones compatibles; auditoría final: 0 vulnerabilidades |
| CRM general en navegador | EXECUTED_PASS | Recorrido de prospectos, contactos, tareas, actividades, importación, oportunidades, campañas, permisos, conflictos y pruebas inbound sintéticas |
| Responsive y teclado | EXECUTED_PASS | Recorrido Chromium con menú móvil y foco; no certificación de accesibilidad ni multibrowser completo |
| Manual de Marketing | EXECUTED_PASS | Revisión visual individual desktop/móvil y ancho 1440/390/320 px, sin desbordamientos |
| Gmail sin OAuth | EXECUTED_PASS | La interfaz preserva el bloqueo de la integración sin cuenta conectada |
| Marketing específico Scalaris | EXECUTED_PASS | Alta operador, separación de permisos, ficha/etiquetas/alcance/responsable, recarga, agenda, dos servicios y audiencia filtrada por `tagId` |
| Corrida conjunta final en navegador | EXECUTED_PASS | 4/4, 0 omitidos/fallos/flaky, ~24 s; inicio 13:03:29 Argentina, fuente/dist final; 16 unitarias repetidas a las 13:05 |
| Launcher Docker PowerShell 5.1 | EXECUTED_PASS | 55 aserciones: ACL/puertos reales, claves distintas/persistentes, configuraciones y carpetas rechazadas; respaldo con Docker simulado |
| Servidor web portable | EXECUTED_PASS | Prueba Node de cookies/CSRF, multipart, proxy, rutas estáticas, rechazo de `Host` ajeno y métodos no permitidos |
| Paquete Windows PowerShell 5.1 | EXECUTED_PASS | 18 comprobaciones reales; primera apertura, cuenta Marketing, ACL, loopback, reapertura y datos persistidos |
| Respaldo/restauración portable | EXECUTED_PASS | `pg_dump` custom, lectura `pg_restore --list`, SHA-256, recuperación en base nueva, mismo prospecto recuperado y base anterior preservada |
| Parada y ausencia de mensajes reales | EXECUTED_PASS | Servicios propios detenidos, base conservada, cero `SENT/DELIVERED/READ` en los escenarios QA |
| Sintaxis y repository safety | EXECUTED_PASS | Parser PowerShell, sintaxis Node, scanner del repositorio y `git diff --check` |

Los seis casos de integración prueban bootstrap idempotente sin sobrescribir claves, textos, etiquetas o preferencias; roles y rechazo de administración técnica/envío; administrador separado por organización; aislamiento del perfil; rechazo de reutilizar cuentas ajenas; filtro por etiqueta activa propia y rechazo de etiquetas inexistentes/inactivas/ajenas sin cambiar la audiencia.

El bootstrap prepara **18 etiquetas**, **7 plantillas** y el rol **MARKETING** con **14 permisos operativos**. La verificación HTTP real confirmó ausencia de `USER_MANAGE` y `MESSAGE_SEND`. Todos los listeners del paquete se verificaron en `127.0.0.1`.

## Prueba de recuperación

La prueba `scripts/test-scalaris-portable.ps1` utilizó una carpeta privada sintética separada de la instalación operativa. Creó un prospecto ficticio, confirmó reapertura sin regenerar credenciales, respaldó, restauró en `scalaris_restore_<fecha>` y volvió a ingresar con Marketing. Comparó ID y nombre antes/después, consultó la existencia de la base original y verificó cero mensajes enviados.

La evidencia estructurada se conserva fuera de Git:

```text
validation-output/scalaris-portable-latest.json
validation-output/scalaris-release-latest.json
validation-output/scalaris-installed-local.json
validation-output/complete-crm-e2e-latest.json
validation-output/scalaris-marketing/screenshots/
validation-output/manual-scalaris/
validation-output/entrega-scalaris/
```

Los registros y las bases sintéticas permanecen privados. Ningún dump, contraseña, cookie, contacto real ni `.env` se incluyó en el repositorio ni en el paquete. Las fuentes comerciales originales permanecen en su ubicación.

## Correcciones verificadas durante el recorrido

- Windows: `Start-Process -Wait` espera descendientes y bloqueaba el inicio de PostgreSQL; se espera únicamente el proceso `pg_ctl` mediante `Process.WaitForExit`, conservando su handle para leer el código de salida en PowerShell 5.1.
- Se eliminan variables heredadas de ubicación de configuración de Spring y opciones Java/Node; un valor vacío de `SPRING_CONFIG_LOCATION` impedía cargar la configuración del CRM.
- ACL idempotente sobre carpeta exclusiva; rechazo de carpetas generales, datos existentes sin configuración, puertos repetidos/ocupados y versiones mayores PostgreSQL incompatibles.
- Identificación de procesos propios por PID, instante de inicio, ejecutable y argumento; reapertura sin duplicar servicios.
- Cliente frontend: conserva los campos al fallar el guardado y recupera transiciones canónicas faltantes de seguimiento/perdido.
- Ficha de prospectos: controles con ancho mínimo compatible, columnas equilibradas y canales legibles; corrección del desbordamiento observado a 1280 px. Manual: corte de nombres de iniciadores largos para pantallas de 320 px.
- Guía/campañas/reportes: columnas apiladas en móvil; regresión geométrica en la guía de 390 px. Timeline comercial: títulos legibles, sin códigos de auditoría ni payload JSON como contenido principal de Marketing.
- Servidor local: `Host` limitado a loopback, proxy same-origin, CSRF/cookies y contenido multipart conservados, cierre correcto de respuestas interrumpidas.

Hubo fallos del harness que se corrigieron antes del PASS: Chromium esperado ausente, selectores de etiquetas accesibles en nuevas pruebas y tratamiento UTF-8 de JSON en PowerShell 5.1. Los fallos iniciales no se registran como pruebas aprobadas; se consideran únicamente los recorridos finales ejecutados.

## Límites explícitos

- **Docker no está instalado:** Compose/build/health/backup/restore sobre Docker real siguen `BLOCKED_EXTERNAL`. La regresión del launcher con mock no sustituye esa ejecución.
- La suite histórica completa de Testcontainers PostgreSQL 17 y los dos validadores integrales limpios sobre un commit definitivo no se ejecutaron. Los resultados actuales cubren los conjuntos seleccionados y el paquete Windows PostgreSQL 18.6.
- Grype de imágenes Docker y el perfil productivo contenedorizado no se ejecutaron en esta edición. La auditoría frontend con 0 hallazgos no sustituye esos controles.
- No se realizó una certificación WCAG ni un recorrido completo Firefox/WebKit. La comprobación de pantalla/teclado/móvil corresponde a Chromium.
- No se conectaron Gmail, SMTP ni WhatsApp reales, no se enviaron mensajes ni se cargaron prospectos operativos. Los borradores/enlaces manuales requieren revisión humana en la aplicación elegida.
- No se publicó el repositorio ni se desplegó producción.

## Entrega y operación

El ZIP contiene aplicaciones compiladas, runtimes Windows, iniciadores, manual y guía. Sus hashes se registran en `VERSION.json` y el checksum del ZIP se guarda junto al archivo. La base y las claves se generan en el perfil de Windows al abrirlo; quedan fuera del paquete.

La guía canónica es `docs/scalaris-marketing-local.md`. El archivo `AbrirCRMScalarisPortable.cmd` abre la edición Windows; detener conserva la base y respaldar crea `.dump` y `.sha256` privados. La recuperación crea una base nueva y conserva la anterior. Un cambio mayor de PostgreSQL exige migración con respaldo; no se destruye la base al actualizar.

Estado de la entrega: **funcional localmente con los controles ejecutados**, con los límites de Docker/integral/producción documentados. El resultado no equivale a una publicación remota.

## Artefacto definitivo y puesta en marcha

```text
ZIP: validation-output/entrega-scalaris/ScalarisCRM-Windows-20261014-130608.zip
tamaño: 242180851 bytes
SHA-256: FBA0D353616AB811A0E7F4800C05140EF5BED14AFE8B6F83E89E7EA84BBA6130
entradas del ZIP: 2243
archivos privados detectados: 0
manifiesto de aplicación/iniciadores: 8 hashes comprobados
JavaScript: FD209B24234EC8AB845488B1D55A763F57ABF8AD64018E6F0934ACE537BF07EB
CSS: 5C59A5FAEBEFB7ACD6AE76884CBBDEDCE1FBFE24227E70BB75B6EA53751962D0
```

El ZIP se comprobó leyendo su directorio real y comparando su checksum. La entrega final contiene los binarios ya probados y las últimas correcciones visuales aprobadas. Los paquetes intermedios conservados en evidencia no son la entrega definitiva.

Se inició el paquete final desde PowerShell 5.1 con una carpeta de datos nueva de uso, separada de QA: `%LOCALAPPDATA%\ScalarisCRM\portable`. Se comprobó `http://127.0.0.1:8787`, sesión Marketing, perfil Scalaris, **0 prospectos**, 18 etiquetas, 7 plantillas y listeners exclusivamente de loopback. Las cuentas y las claves iniciales se generaron fuera del paquete, en su carpeta privada. El entorno QA se detuvo y sus datos ficticios no pasaron a la instalación de uso.

La instalación de uso se dejó activa. La apertura de la pantalla en el panel Codex se solicitó y quedó encolada; el endpoint local sí se comprobó directamente. El usuario puede abrir la dirección local o el iniciador del paquete y consultar el archivo privado `Acceso-inicial.txt`.
