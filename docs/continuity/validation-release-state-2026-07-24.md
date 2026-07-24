# Estado de validación y publicación — 2026-07-24

## Estado ejecutivo

```text
baseline funcional previo: 83e181ce614f145bbfe141cc7603c3042569be51
candidato post-hardening: NO PUBLICADO
tree del candidato: 9e058d7044415b80af554ab8ae4fe3170585b1c9
producción real: NO DESPLEGADA
envíos reales: DESHABILITADOS
```

La publicación de estos documentos de continuidad puede mover `origin/main` después del baseline. El SHA anterior sigue siendo la base histórica del candidato, no necesariamente la punta remota actual.

## Cadena reproducible del candidato

```text
v6 candidate tree:
e3a9728e717b7c8a4d92f9fab31f709bf5d66464

+ locators E2E:
24df4c7f26ffde0f044f681f9130fa254f15debd

+ primera restauración de foco:
fa8c15172dfa9a0cfa5cbd00f7aab42733d516ba

+ disparador de foco explícito:
9e058d7044415b80af554ab8ae4fe3170585b1c9
```

Los SHAs de commits temporales cambian en cada reconstrucción; los trees son la referencia estable de contenido.

## Cambios incluidos en el candidato

- eliminación de automatización UX remota obsoleta;
- métricas tenant-wide del dashboard;
- navegación accesible y drawer móvil;
- resultados de importación paginados y filtrados en backend;
- aislamiento tenant y corrección de búsqueda PostgreSQL;
- paginación de outbox e inbound;
- modularización incremental del frontend;
- feedback para operaciones lentas;
- reporting de destinatarios excluidos;
- pruebas multibrowser;
- corrección de retorno de foco explícito en WebKit;
- alineación y endurecimiento de validadores.

## Evidencia más reciente

Archivo:

```text
gestudio-runtime-resume-evidence-9e058d704441-20260724-124206.zip
```

SHA-256:

```text
C70E6105E0D0AFA0A902BBAC2F1F7E1B0DD646F2B9406391FC405249328908ED
```

Resumen estructurado:

```text
status: EXECUTED_FAIL
productionProfileSmoke: EXECUTED_FAIL
finalTreeClean: NOT_RUN
checkoutModified: false
remotePushPerformed: false
```

## Interpretación exacta del último fallo

El perfil productivo local:

- seleccionó el puerto libre `18081`;
- levantó PostgreSQL, backend y frontend como `healthy`;
- ejecutó con los IDs de imágenes esperados;
- mostró en el entorno del backend las guardas fail-closed, incluida `SENDING_ENABLED=false`;
- limpió contenedores, redes y volumen creados.

Sin embargo, el harness terminó con:

```text
Falta el bloqueo de producción: SENDING_ENABLED=false
```

La evidencia demuestra que la variable sí estaba presente dentro del JSON retornado por:

```text
docker inspect <backend> --format {{json .Config.Env}}
```

Por tanto, el bloqueo pendiente es una aserción/parsing defectuoso del reanudador, no ausencia de la guarda en el contenedor. Aun así, el estado formal sigue siendo `EXECUTED_FAIL` porque el script no completó `finalTreeClean`.

## Próxima corrección obligatoria

Localizar el script real usado para la reanudación o trasladar la corrección al validador canónico. Parsear el resultado JSON como arreglo antes de comprobar membresía. En PowerShell, el comportamiento esperado es conceptualmente:

```powershell
$environment = $json | ConvertFrom-Json
if ($environment -notcontains 'SENDING_ENABLED=false') {
    throw 'Falta el bloqueo de producción: SENDING_ENABLED=false'
}
```

No copiar este fragmento sin confirmar los nombres y tipos reales del script.

Después:

1. ejecutar únicamente la prueba específica del parser/aserción;
2. reanudar `productionProfileSmoke`;
3. ejecutar `finalTreeClean`;
4. exigir `FUNCTIONAL_PASS` en JSON estructurado;
5. reconstruir el candidato sobre el `main` actual;
6. ejecutar las validaciones afectadas por la nueva base documental;
7. publicar en `main` solo si todo termina verde.

## Importante sobre la nueva base remota

Los archivos de `docs/continuity/` y la modificación de `AGENTS.md` son documentación añadida después del baseline del candidato. Al integrar el candidato:

- no forzar `main` de vuelta a `83e181c`;
- preservar el commit documental;
- aplicar o cherry-pickear los cambios funcionales encima del `HEAD` actual;
- recalcular el tree final;
- comprobar que no haya conflictos ni cambios colaterales;
- volver a validar los archivos afectados por la integración.

## No repetir

No repetir backend completo, frontend unitario, builds, migraciones, Playwright, bloqueo de envíos, cero enviados o backup/restore solo para volver a demostrar resultados ya cubiertos, salvo que:

- cambie código o configuración de esas fases;
- la evidencia no sea íntegra;
- cambien dependencias;
- el nuevo `main` introduzca solapamientos;
- el validador canónico exija una corrida integral final del commit exacto.

El cierre definitivo puede requerir una corrida integral sobre el commit que se publicará, aunque durante el diagnóstico se reutilicen fases anteriores.
