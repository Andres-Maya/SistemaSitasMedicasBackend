# 6. Riesgos identificados

Cada riesgo dice **qué podría salir mal**, **qué lo causa en el diseño**, **qué habría que verificar para descartarlo (o confirmarlo)** y la **mitigación** si se confirma.

| # | Riesgo | Probabilidad | Impacto | Driver afectado |
|---|---|---|---|---|
| R1 | Eventos perdidos por doble escritura sin outbox | Media | Alto | 3, 2 |
| R2 | La exclusión del cupo no sobrevive a varias réplicas de Agenda | Alta si se escala | Alto | **1** |
| R3 | Paciente recién registrado que Agenda todavía no conoce | Baja | Medio | 3 |
| R4 | Historia clínica legible sin autenticación ni rol | **Alta (hoy)** | **Crítico** | **2** |
| R5 | Cupos retenidos indefinidamente en `PENDING_AUTHORIZATION` | Media | Medio | 1, 3 |
| R6 | Datos personales replicados en tres servicios | Alta | Medio | 2 |
| R7 | RabbitMQ como punto único de falla para la propagación | Baja | Medio | 3 |

---

### R1 · Eventos perdidos por doble escritura sin outbox

- **Qué podría pasar:** Agenda guarda la cita y luego falla al publicar `cita.agendada` porque el broker está caído. La cita existe, pero Historia Clínica no la muestra y el paciente no recibe aviso. Lo mismo con `paciente.registrado`: el paciente existe, pero Agenda nunca podrá agendarle.
- **Causa en el diseño:** guardar el estado y publicar el evento son dos operaciones sin transacción común (ADR-002). Hoy el fallo solo queda en el log (`PublicadorRabbit`).
- **Qué verificar para descartarlo:** una prueba de caos. Con el sistema arriba, ejecutar `docker compose stop rabbitmq`, registrar un paciente y agendar, levantar RabbitMQ y comparar cuántas entidades se crearon con cuántos eventos llegaron a las colas (consola en `:15672`). **Si el conteo difiere, el riesgo se confirma** (en el esqueleto se confirma).
- **Mitigación:** patrón *transactional outbox*. El evento se guarda en una tabla en la misma transacción que la entidad y un proceso aparte lo publica con reintentos.

### R2 · La exclusión del cupo no sobrevive a varias réplicas

- **Qué podría pasar:** al escalar Agenda a dos instancias para el pico (P1), cada una tiene su propio `synchronized` y su propia memoria, y dos pacientes obtienen el mismo cupo. Se rompe el **driver 1**.
- **Causa en el diseño:** la táctica T1 (sección crítica en memoria) solo vale dentro de un proceso.
- **Qué verificar para descartarlo:** levantar dos réplicas de Agenda detrás del gateway sobre un almacén compartido y repetir la ráfaga del paso 6 de `demo.sh` 100 veces. **Se descarta solo si hay 0 duplicados en las 100 corridas.** Con la implementación actual no es posible descartarlo.
- **Mitigación:** PostgreSQL con restricción `UNIQUE (doctor_id, slot)` o `SELECT … FOR UPDATE`. Así el bloqueo pasa del proceso a la base de datos. Alternativa: particionar por médico, de modo que cada réplica sea dueña de un subconjunto de médicos.
- **Es la respuesta a "¿qué se rompe primero si el negocio crece diez veces?"** (ver [07](07-guia-sustentacion.md)).

### R3 · Paciente recién registrado que Agenda todavía no conoce

- **Qué podría pasar:** el paciente se registra y agenda de inmediato. Si `paciente.registrado` todavía no ha llegado a Agenda, recibe "Agenda no conoce al paciente".
- **Causa en el diseño:** consistencia eventual (ADR-002).
- **Qué verificar para descartarlo:** medir la latencia de propagación (`occurredAt` del evento frente a la hora de consumo en el log de Agenda) bajo la carga de P1. **Si el p99 < 500 ms**, que es menos de lo que tarda un humano en llenar el formulario de cita, el riesgo se descarta en la práctica.
- **Mitigación si se confirma:** reintento automático en el portal o un mensaje claro de "estamos preparando tu cuenta".

### R4 · Historia clínica legible sin autenticación ni rol

- **Qué podría pasar:** cualquiera que conozca o adivine un `patientId` ejecuta `GET /api/clinica/historia/{id}` y obtiene la historia. La cabecera `X-Usuario` la pone el cliente, así que la auditoría puede falsearse. **Incumple RS2 y el driver 2.**
- **Causa en el diseño:** el esqueleto no tiene un proveedor de identidad. El login devuelve el paciente, pero no un token.
- **Qué verificar para descartarlo:** una prueba básica de penetración: pedir la historia de otro paciente sin credenciales y con credenciales de otro paciente. **Se descarta solo si ambas peticiones devuelven 401/403.** Hoy devuelven 200: el riesgo está **confirmado y bloquea la salida a producción**.
- **Mitigación:** OIDC (por ejemplo, Keycloak) con roles `paciente`, `medico` y `auditor`. El gateway valida el JWT e Historia Clínica toma el usuario del token, no de una cabecera libre.

### R5 · Cupos retenidos en `PENDING_AUTHORIZATION`

- **Qué podría pasar:** durante una caída larga de la EPS se acumulan citas pendientes que ocupan cupos. Si después se niegan, esos cupos se pierden para otros pacientes.
- **Causa en el diseño:** la degradación de ADR-003 sin expiración ni reconciliación.
- **Qué verificar para descartarlo:** obtener del convenio con cada EPS su disponibilidad real (SLA) y la duración típica de sus caídas, y contar en el histórico qué porcentaje de autorizaciones termina negado. **Si las caídas son menores a una hora y las negaciones están por debajo del 2 %**, el impacto es tolerable con una revisión manual diaria.
- **Mitigación:** expiración automática (por ejemplo, 24 h o 48 h antes de la cita) más un reintento periódico de la autorización.

### R6 · Datos personales replicados

- **Qué podría pasar:** nombre, documento, correo y teléfono quedan copiados en Agenda y Notificaciones, además de Historia Clínica. Hay más lugares que proteger y más lugares donde atender una solicitud de supresión (Ley 1581).
- **Causa en el diseño:** las copias locales de ADR-002.
- **Qué verificar:** un inventario de datos personales por servicio, revisado con el responsable de protección de datos. Se debe confirmar que **ningún** dato clínico aparece fuera de Historia Clínica (revisando los contratos de [04](04-contratos-eventos.md)).
- **Mitigación:** minimizar las copias. Agenda solo necesita `documentNumber` y `eps`, y Notificaciones solo el contacto. Además, un evento `paciente.suprimido` para borrar en todas partes.

### R7 · RabbitMQ como punto único de falla

- **Qué podría pasar:** si el broker cae, se puede seguir agendando (Agenda no depende de él para responder), pero las copias locales y los avisos se detienen y aparece R1.
- **Qué verificar:** con `docker compose stop rabbitmq`, comprobar que el agendamiento de pacientes ya conocidos sigue respondiendo. Medir el tiempo de recuperación cuando el broker vuelve.
- **Mitigación:** clúster de RabbitMQ con colas *quorum* (3 nodos), sumado al outbox de R1.
