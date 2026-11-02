# ADR-002 · Integración entre servicios por eventos sobre RabbitMQ

*Fecha: 2026-11-02 · Decisores: equipo del proyecto*

## 1. Estado

**Aceptada.**

## 2. Contexto

Con tres servicios (ADR-001) hay información que debe cruzar las fronteras:

- Agenda necesita saber que un paciente existe y cuál es su EPS, para pedir la autorización.
- Historia Clínica debe mostrar las citas del paciente en la historia completa.
- Notificaciones debe avisar del registro, la confirmación, el estado pendiente y la cancelación.

Drivers implicados: **driver 3** (si un servicio está caído, los demás siguen; escenarios E2 y E4) y **driver 2** (los datos clínicos no deben salir de Historia Clínica; E3). El volumen esperado es bajo para un broker (cientos de mensajes por minuto en el pico, P1).

## 3. Decisión

- Los servicios **no se llaman entre sí** por HTTP. Se comunican **solo por eventos de dominio** publicados en un exchange `topic` de **RabbitMQ** (`clinica.eventos`).
- Cada consumidor declara **su propia cola durable**. Cada evento llega a **dos** consumidores independientes (ver [04-contratos-eventos.md](../04-contratos-eventos.md)).
- Cada consumidor guarda una **copia local** de lo que necesita: Agenda guarda pacientes conocidos y Historia guarda las citas. Así responde sin depender de que el otro servicio esté arriba.
- Los consumidores son **idempotentes** porque la entrega es *al menos una vez*.
- Los eventos llevan **solo identificadores y datos no clínicos**.

## 4. Alternativas consideradas y descartadas

| Alternativa | Qué tenía a favor | Por qué se descartó |
|---|---|---|
| **A. REST síncrono entre servicios** (Historia llama a Agenda para armar la historia; Agenda llama a Historia para validar al paciente) | Consistencia inmediata, más fácil de entender y depurar, sin broker | Acopla la disponibilidad: si Agenda cae, la historia completa falla; si Historia cae, no se puede agendar. Las latencias se suman en cadena. Agenda tendría que hacer **una llamada más** por cita durante el pico. Contradice el driver 3 |
| **B. Base de datos compartida** | Lo más simple: un JOIN | Rompe el driver 2 (Agenda tendría credenciales sobre las tablas clínicas) y anula la frontera de ADR-001: un cambio de esquema rompe a todos |
| **C. Apache Kafka** | Retención y re-lectura de eventos (*replay*), gran volumen, base para event sourcing | El volumen (cientos de mensajes/min) no lo justifica. Es mucho más pesado de operar en local (RS6). El enrutamiento por routing key de RabbitMQ cubre lo que necesitamos. **Se perdería poco hoy, pero se reconoce lo que se pierde:** el *replay* para reconstruir proyecciones |
| **D. Webhooks HTTP** (el publicador llama por HTTP a cada interesado) | No requiere broker | El publicador debe conocer a sus consumidores y reintentar si están caídos. Es reimplementar un broker, mal |
| **E. Orquestación con saga central** | Flujo explícito y visible | No hay transacciones largas de varios pasos que coordinar. Un orquestador sería un punto central que conoce a todos los servicios |

## 5. Consecuencias

**Positivas**
- Un servicio caído no impide que los otros trabajen. Los eventos esperan en la cola durable (E4, demostrable en vivo).
- Agregar un consumidor (por ejemplo, facturación o analítica) no exige modificar a los publicadores (M2).
- Los contratos de eventos son explícitos y revisables: se puede comprobar que no viajan datos clínicos (E3).

**Negativas (aceptadas)**
- **Consistencia eventual:** la historia completa puede no mostrar todavía una cita recién agendada, y un paciente recién registrado no puede agendar hasta que Agenda recibe el evento (riesgo R3).
- **Doble escritura sin outbox:** si RabbitMQ no está disponible al publicar, el estado se guarda pero el evento se pierde. Hoy solo se registra en el log (riesgo R1).
- **El broker es un punto único de falla** para la propagación, aunque no para agendar (riesgo R7).
- **Depuración más difícil:** un flujo atraviesa tres procesos y el broker, y no hay trazas distribuidas (la mitigación sería un *correlation id* y OpenTelemetry).
- Sin *replay*: si una proyección se corrompe, no se puede reconstruir desde el broker (sí se podría con Kafka).
- Datos personales (no clínicos) duplicados en las copias locales (riesgo R6).
