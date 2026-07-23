# Convenciones de experiencia de operador

Este documento define la presentación visible del CRM. Los valores internos de API, base de datos, enums, permisos y auditoría no se traducen ni se renombran; la interfaz los representa mediante etiquetas centralizadas en `frontend/src/uiLabels.ts`.

## Lenguaje y orientación

- Todo texto principal se redacta en español claro y orientado a una acción.
- Los estados técnicos se muestran con una etiqueta humana. El valor interno solo aparece dentro de un detalle técnico cuando aporta valor operativo.
- Los vacíos explican qué falta: por ejemplo, “Sin correo cargado” o “Sin responsable asignado”.
- Los errores visibles explican el problema y el siguiente paso seguro; no muestran SQL, stack traces, secretos ni nombres internos de tablas.

## Patrones reutilizables

- `labelFor` centraliza estados, canales, roles, etapas, acciones y motivos.
- `openDecisionDialog` reemplaza confirmaciones y solicitudes de datos del navegador. Conserva foco, permite Escape, bloquea el foco dentro del diálogo y exige confirmación contextual.
- Los datos JSON de auditoría, outbox e importaciones se resumen primero y quedan disponibles bajo “Ver datos técnicos”, con claves sensibles ocultas.
- Las listas extensas usan búsqueda, filtros, conteos, paginación o revelado progresivo.

## Responsive y accesibilidad

- Los controles tienen una altura táctil mínima de 44 px.
- El foco es visible y las filas seleccionables responden a Enter y Espacio.
- Las tablas conservan desplazamiento horizontal controlado y encabezados visibles.
- En pantallas pequeñas la navegación se vuelve horizontal y compacta, los formularios pasan a una columna y los diálogos no exceden el viewport.
- El color nunca es la única señal: badges, alertas y estados incluyen texto.

## Duplicados e importaciones

`CREATE_SEPARATE` y `MARK_NOT_DUPLICATE` reconstruyen el prospecto únicamente desde campos importados conocidos y normalizados. Se conservan institución, localidad, provincia, categoría, sitio, fuente, evidencia, prioridad, fecha de verificación, correo y teléfono/WhatsApp. Campos desconocidos no se convierten en columnas ni propiedades arbitrarias.

La creación sigue pasando por `ProspectApplicationService`, por lo que conserva normalización, unicidad, exclusiones, aislamiento por organización, auditoría y cálculo de elegibilidad. El identificador externo de la fila se conserva como evidencia, mientras que la clave técnica del prospecto separado es idempotente por revisión.

## Seguridad de mensajería

La interfaz describe los bloqueos con lenguaje operativo, pero no cambia los controles técnicos. Deben permanecer:

- envíos reales deshabilitados;
- simulación activa;
- límite diario en cero;
- protección de emergencia activa;
- proveedores externos sin conexión;
- ausencia de endpoint de envío real.

## Validación

Comandos canónicos del repositorio:

```bash
npm ci --no-audit --no-fund --prefix frontend
npm run typecheck --prefix frontend
npm run test:unit --prefix frontend
npm run build --prefix frontend
./mvnw -B -f backend/pom.xml verify
bash scripts/check-repository-safety.sh
git diff --check
```
