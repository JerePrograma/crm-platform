# SEG-001 — Validación integral y cierre técnico

## Estado

La implementación funcional del vertical slice está sustancialmente completa. No está validada todavía por una ejecución observable de CI.

## Objetivo del próximo `continuar`

Ejecutar o inspeccionar todos los controles reales, corregir cualquier fallo y registrar evidencia precisa. No añadir nuevos módulos hasta estabilizar el árbol existente.

## Orden obligatorio

1. consultar GitHub Actions del último commit;
2. si existe un fallo, leer job y logs completos;
3. corregir compilación/formato antes de cualquier otra tarea;
4. ejecutar backend con Java 21 y Maven fijado;
5. confirmar Flyway + Hibernate + Testcontainers;
6. instalar frontend y generar `package-lock.json`;
7. ejecutar `npm run build`;
8. validar Compose;
9. construir imagen Docker;
10. revisar `git diff` contra `main`;
11. ejecutar búsqueda de secretos/datos reales;
12. registrar comandos, fechas, commit y resultados en `docs/validation/SEG-001.md`.

## Comandos esperados

Linux/macOS:

```bash
sh ./mvnw -B -f backend/pom.xml verify
(cd frontend && npm install && npm run build)
docker compose config
docker build -t gestudio-crm:seg-001 .
```

Windows PowerShell:

```powershell
.\mvnw.cmd -B -f backend\pom.xml verify
Push-Location frontend
npm install
npm run build
Pop-Location
docker compose config
docker build -t gestudio-crm:seg-001 .
```

## Correcciones esperables

- formato Spotless;
- imports o APIs incompatibles con Spring Boot 4;
- diferencias JPA/Flyway;
- tipos TypeScript estrictos;
- dependencia frontend incompatible;
- rutas de Docker;
- pruebas Testcontainers lentas o con limpieza incorrecta.

No anticipar ni ocultar errores: usar los resultados reales.

## Mejoras permitidas después de verde

- generar lockfile frontend;
- pruebas de API para confirmación de importación;
- prueba PHONE/WHATSAPP;
- archivo corrupto y límite de 10 MB;
- acción auditada para resolver DuplicateReview;
- retry explícito de importación fallida;
- documentación de validación final.

## Criterios de cierre

- backend verde;
- frontend verde;
- Flyway/JPA verde;
- Compose verde;
- imagen Docker verde;
- sin secretos ni datos reales;
- resultados documentados;
- `SEG-001` marcado `COMPLETE`;
- `SEG-002` marcado `ACTIVE`;
- `docs/next-step.md` reemplazado por identidad/RBAC.

## Restricciones

- no abrir PR;
- no fusionar a `main`;
- no desplegar;
- no habilitar envíos;
- no importar el XLSX real durante CI;
- no declarar éxito sin evidencia.
