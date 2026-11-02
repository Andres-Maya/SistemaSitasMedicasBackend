# 7. Guía de preparación para la sustentación

> Esta guía **no** reemplaza que el equipo entienda las decisiones. Úsenla para practicar: uno pregunta, otro responde sin leer. Si alguien no puede explicar una fila con sus palabras, relean el ADR correspondiente y discútanlo hasta poder defenderlo **o cambiarlo**.

## Las cuatro preguntas, aplicadas a cada decisión

### D1 · Tres microservicios en lugar del monolito (ADR-001)

| Pregunta | Respuesta corta |
|---|---|
| ¿Qué driver la justifica y de dónde salió? | **Driver 2**: la historia clínica es un dato sensible (Ley 1581, Res. 1995). Necesitábamos que un solo proceso tuviera los datos clínicos. **Driver 3**: que la caída de los avisos o de la EPS no tumbe todo |
| ¿Qué alternativa descartaron? | El **monolito modular**: era la opción más fuerte y más simple de operar. Lo descartamos porque su aislamiento depende de la disciplina del equipo y no del despliegue, y porque no permite escalar solo la agenda |
| ¿Qué atributo empeoró y por qué es aceptable? | **Simplicidad operativa y consistencia inmediata.** Es aceptable porque la única consistencia fuerte que el negocio exige (el cupo) quedó dentro de un solo servicio |

### D2 · Eventos sobre RabbitMQ en lugar de REST entre servicios (ADR-002)

| Pregunta | Respuesta corta |
|---|---|
| Driver | **Driver 3**, escenario E4: Notificaciones caído no impide agendar. Lo mostramos en vivo |
| Alternativa descartada | **REST síncrono**: si Agenda cae, la historia no carga, y al revés. **Kafka**: el volumen no lo justifica, aunque reconocemos que perdemos el *replay* |
| Atributo que empeoró | **Consistencia eventual** y **riesgo de eventos perdidos** (R1, sin outbox). Aceptable porque mostrar una cita en la historia 50 ms tarde no afecta a nadie, y el outbox es una mitigación conocida |

### D3 · EPS con timeout de 2 s y estado "pendiente" (ADR-003)

| Pregunta | Respuesta corta |
|---|---|
| Driver | **Driver 3** (E2) a partir de una **restricción externa**: la autorización de la EPS es obligatoria y no controlamos ese sistema |
| Alternativa descartada | **Rechazar la cita si la EPS no responde** (paraliza la recepción) y **siempre asíncrono** (respuesta incierta para el 100 % de los pacientes) |
| Atributo que empeoró | **Consistencia con la EPS:** reservamos cupos que pueden terminar negados (R5). Es aceptable porque perder un cupo unas horas cuesta menos que perder al paciente |

### D4 · Agenda como único escritor del cupo (táctica T1)

| Pregunta | Respuesta corta |
|---|---|
| Driver | **Driver 1**, hoja C1 (la única A,A del árbol). Sale de la operación: en la apertura de agenda llegan muchas solicitudes por los mismos cupos |
| Alternativa descartada | Bloqueo optimista con versión (reintentos y peor experiencia en el pico) y bloqueo distribuido con Redis (más infraestructura para un problema que la base de datos resuelve con una restricción única) |
| Atributo que empeoró | **Escalabilidad**: el bloqueo vale para un proceso (R2). Aceptable en el esqueleto. En producción el bloqueo pasa a una restricción `UNIQUE` en PostgreSQL |

### D5 · Historia Clínica incluye prescripción y laboratorio (frontera de contexto)

| Pregunta | Respuesta corta |
|---|---|
| Driver | **Driver 2**: separarlos obligaría a enviar alergias por la red para validar una prescripción |
| Alternativa descartada | Servicios separados para Prescripción y Laboratorio |
| Atributo que empeoró | Historia Clínica es el servicio más grande y concentra más cambios. Aceptable porque todo lo que contiene comparte regulación y actor |

## "Si el negocio creciera diez veces, ¿qué se rompe primero?"

1. **Lo primero: la reserva de cupos en Agenda (R2).** A 3.000 solicitudes por minuto en el pico, una sola instancia con la sección crítica `synchronized` se convierte en el cuello de botella, y al poner una segunda réplica la exclusión deja de funcionar. *Arreglo:* PostgreSQL con `UNIQUE (doctor_id, slot)`, Agenda sin estado en memoria y réplicas detrás del gateway. Si aún no alcanza, particionar por sede o por médico.
2. **Lo segundo: la EPS.** Diez veces más autorizaciones pueden superar el límite de peticiones de la EPS, y nuestro timeout convertiría eso en muchas citas pendientes (R5). *Arreglo:* circuit breaker, cola de autorizaciones con reintento y, si el convenio lo permite, cachear la cobertura del afiliado.
3. **Lo que no se rompe:** RabbitMQ maneja miles de mensajes por segundo y diez veces nuestro volumen sigue muy por debajo. Historia Clínica crece en **datos** (retención de años), no en tráfico pico: su problema a 10× es de almacenamiento y archivado, no de CPU.

## Guion de la demostración en vivo (≈ 6 minutos)

| Minuto | Acción | Qué demuestra |
|---|---|---|
| 0:00 | `docker compose ps` → seis contenedores | El diagrama C4 nivel 2 corresponde a procesos reales |
| 0:30 | `bash demo.sh` (pasos 1–4): registrar y agendar | Recorrido de punta a punta. **Un evento, dos consumidores** (Agenda y Notificaciones reaccionan a `paciente.registrado`) |
| 2:00 | Paso 6: ráfaga de 8 reservas sobre el mismo cupo | E1 / driver 1: **exactamente una** gana |
| 3:00 | Paso 7: paciente `888…` | E2 / driver 3: respuesta en ~2 s con `PENDING_AUTHORIZATION` |
| 3:30 | `docker compose stop notificaciones`, agendar en el portal, `docker compose start notificaciones` | E4: el aviso llega al volver; agendar nunca falló |
| 5:00 | Abrir la consola de RabbitMQ (`localhost:15672`) → *Queues* | Una cola por consumidor; los mensajes se acumulan cuando el servicio está detenido |
| 5:30 | `GET /api/clinica/auditoria/{id}` | E3 / driver 2: auditoría de accesos |

**Si algo falla en vivo:** `docker compose logs -f agenda historia-clinica notificaciones` muestra cada evento publicado y consumido.
