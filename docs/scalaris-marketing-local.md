# Scalaris CRM para Marketing

Esta edición reúne los tres servicios de Scalaris: **servicio técnico, desarrollo y venta a pedido**. El contacto vigente es **11 7278-9289** y la cobertura es **Zona Oeste, CABA y Mar del Plata**. El retiro y la entrega se coordinan según el caso.

## Opción recomendada: paquete Windows

1. Extraé el ZIP completo en una carpeta propia, por ejemplo `Documentos\ScalarisCRM`. Conservá todos los archivos y subcarpetas.
2. Abrí `AbrirCRMScalarisPortable.cmd` con doble clic. La primera apertura prepara la base local y puede tardar unos minutos.
3. El navegador abre el CRM. En la primera apertura aparece el archivo privado `Acceso-inicial.txt`, con las cuentas de Marketing y administración.
4. Ingresá con `scalaris-marketing`. Cambiá la contraseña inicial desde **Mi cuenta** y conservá la nueva clave en un lugar seguro.
5. Usá `DetenerCRMScalarisPortable.cmd` para cerrar los servicios. Cerrar el navegador permite volver a abrir el CRM; no elimina los contactos.

El paquete incluye Java 21, Node y PostgreSQL para Windows de 64 bits. No requiere instalar Java, Node, Maven ni Docker. Al extraerlo, la computadora necesita espacio para la aplicación y sus datos. Windows puede requerir los componentes de ejecución de Microsoft Visual C++ utilizados por PostgreSQL; si falta una biblioteca, la apertura informa el error y conserva los registros.

Los datos se guardan en `%LOCALAPPDATA%\ScalarisCRM\portable`, dentro del perfil de Windows que utiliza el CRM. Esa carpeta contiene la base de contactos, la configuración, las claves iniciales y los registros. Cambiar de usuario de Windows crea una instalación independiente. Copiar solo el ZIP no transfiere los contactos de una instalación existente.

La dirección habitual es `http://127.0.0.1:8787`. La primera apertura elige otros puertos libres si es necesario; consultá `Acceso-inicial.txt` para conocer la dirección efectiva. Todos los componentes escuchan únicamente en esta computadora.

## Alternativa desde el repositorio: Docker

Requiere [Docker Desktop para Windows](https://docs.docker.com/desktop/setup/install/windows-install/) instalado, abierto y configurado para contenedores Linux. La primera apertura necesita Internet para descargar componentes y compilar las imágenes.

Abrí `AbrirCRMScalaris.cmd`. El iniciador prepara credenciales privadas, selecciona un puerto libre y espera a que el sistema esté listo antes de abrir el navegador. Usá `DetenerCRMScalaris.cmd` para detenerlo y `RespaldarCRMScalaris.cmd` para crear una copia.

Esta modalidad utiliza un volumen Docker independiente. Sus datos no se comparten con el paquete Windows ni con otros proyectos. La configuración y las cuentas iniciales quedan en `%LOCALAPPDATA%\ScalarisCRM`; el volumen de PostgreSQL conserva los contactos.

## Trabajo diario de Marketing

1. Revisá **Resumen comercial** y **Agenda**: tareas vencidas, próximas acciones y oportunidades abiertas.
2. En **Prospectos**, registrá la persona o el negocio, su localidad y el origen de la consulta. Agregá sus contactos y canales.
3. Asigná una etiqueta de servicio: **Servicio técnico**, **Desarrollo** o **Venta a pedido**. Combiná etiquetas para segmentar por cobertura, canal u oportunidad cuando sea útil.
4. Dejá una nota con la necesidad concreta. Evitá diagnósticos, precios o plazos que todavía no se confirmaron.
5. Creá una tarea con fecha y próxima acción. Después del contacto, registrá el resultado y completá o reprogramá el seguimiento.
6. Cuando exista una propuesta concreta, creá una oportunidad y actualizá su etapa e importe. Registrá el motivo al cerrar una oportunidad.
7. Para preparar campañas, elegí una plantilla del servicio correspondiente, revisá cada variable y congelá la audiencia. La simulación permite comprobar el recorrido.
8. En **Exclusiones**, registrá bajas, rechazos, contactos inválidos o rebotes. Una exclusión impide que el CRM considere ese destino apto para nuevas comunicaciones.

Una dirección publicada no demuestra consentimiento para recibir publicidad. Un mensaje preparado o simulado no fue enviado. La aceptación de un correo por el proveedor tampoco confirma recepción, lectura ni respuesta. Las comunicaciones reales del CRM permanecen deshabilitadas en esta entrega.

## Importación de una planilla

Prepará una copia de trabajo de la planilla, conservando el original. El CRM acepta CSV UTF-8 separado por coma o punto y coma y XLSX con hoja `Prospectos` o `Maestro`, hasta 10 MiB y 10.000 filas. El nombre obligatorio puede figurar como `Institución`, `Empresa`, `Negocio`, `Empresa o persona` o `Persona o negocio`; la documentación de importación describe los encabezados admitidos.

Primero ejecutá la **vista previa**. Revisá aceptados, excluidos, rechazados y duplicados antes de confirmar. Las columnas adicionales se conservan como evidencia de importación; no se convierten automáticamente en seguimientos, oportunidades ni historial de mensajes. Usá las etiquetas y las actividades del CRM para completar esos datos.

Los archivos de contactos reales deben permanecer fuera del repositorio, las pruebas y el paquete de instalación. Esta entrega inicia sin prospectos reales; las plantillas y etiquetas del negocio sí se preparan automáticamente.

## Respaldo y recuperación

Abrí `RespaldarCRMScalarisPortable.cmd` después de una jornada de trabajo o antes de actualizar. El respaldo se guarda en `%LOCALAPPDATA%\ScalarisCRM\portable\respaldos` como un archivo `.dump` acompañado por `.sha256`. El iniciador comprueba que el archivo sea legible antes de confirmarlo. Copiá ambos archivos a un almacenamiento privado distinto de esta computadora. Los respaldos contienen datos comerciales y deben tratarse como privados.

La recuperación del paquete Windows está disponible para el responsable técnico:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/scalaris-portable.ps1 -Action Restore -BackupPath "C:\Ruta\ScalarisCRM-fecha.dump"
```

La recuperación verifica el checksum, crea una base nueva y restaura allí. Conserva la base anterior y cambia la configuración únicamente después de una restauración exitosa. Después hay que volver a abrir el CRM. Las credenciales de acceso serán las guardadas dentro del respaldo, que pueden diferir de las claves iniciales.

La modalidad Docker dispone de respaldo; una restauración debe realizarla el responsable técnico usando el procedimiento de backup/restore del repositorio. No mezclar respaldos entre versiones mayores de PostgreSQL sin una migración revisada.

## Actualizaciones y problemas frecuentes

Antes de actualizar, generá un respaldo. Detené el CRM, extraé la nueva entrega en otra carpeta y abrila con el mismo usuario de Windows. El iniciador reutiliza la carpeta de datos y aplica las migraciones compatibles. Conservá el paquete anterior hasta comprobar el nuevo. Si cambia la versión mayor de PostgreSQL, el iniciador detiene la actualización para exigir una migración con respaldo.

Si una contraseña se cambió dentro del CRM, `Acceso-inicial.txt` conserva la contraseña original. Administración puede gestionar los usuarios; cambiar ese archivo no cambia la contraseña del sistema. No borres la base para recuperar un acceso.

Si la apertura falla, consultá `%LOCALAPPDATA%\ScalarisCRM\portable\logs` en el paquete Windows, o el mensaje del iniciador Docker. Para obtener ayuda, compartí el error concreto evitando incluir claves, contactos o respaldos completos.

## Preparación técnica de un paquete nuevo

Compilar frontend y backend con sus controles pertinentes. Luego ejecutar desde el repositorio:

```powershell
.\scripts\package-scalaris-portable.ps1 -JavaHome "C:\Ruta\Java21" -PostgresHome "C:\Ruta\PostgreSQL18"
```

El empaquetador usa los binarios compilados y runtimes indicados, genera `VERSION.json`, un ZIP y su checksum. Nunca incluye la carpeta de datos de PostgreSQL ni las credenciales de la instalación. La procedencia y el estado de validación de esta edición se registran en `docs/validation/scalaris-marketing-local-2026-10-14.md`.
