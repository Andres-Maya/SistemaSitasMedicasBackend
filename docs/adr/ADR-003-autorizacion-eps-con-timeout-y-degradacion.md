# ADR-003 · Autorización de la EPS síncrona con timeout de 2 s y degradación a "pendiente"

*Fecha: 2026-11-02 · Decisores: equipo del proyecto*

## 1. Estado

**Aceptada.**

## 2. Contexto

Toda cita con un especialista (todo lo que no sea Medicina General) requiere la **autorización previa de la EPS** del paciente (restricción externa RS4). El sistema de la EPS:

- no lo controlamos: cada EPS tiene su API, su latencia y sus ventanas de mantenimiento;
- en el mejor caso responde en unos cientos de milisegundos, pero puede tardar segundos o no responder.

Drivers: **driver 3** (escenario E2: el paciente recibe respuesta en < 3 s aunque la EPS no responda), **driver 1** (mientras se espera a la EPS, otros pacientes deben poder reservar) y **M1** (otras EPS con otras APIs, escenario E5).

## 3. Decisión

1. Agenda **reserva el cupo primero**, dentro de la sección crítica (driver 1), y **después** llama a la EPS **fuera** del bloqueo.
2. La llamada es **síncrona, con un timeout de 2 s** (conexión y lectura).
3. Según el resultado:
   - **APROBADA** → la cita queda `SCHEDULED` con su número de autorización.
   - **NEGADA** → se libera el cupo y se informa al paciente el rechazo de su EPS.
   - **Sin respuesta, error o timeout** → la cita queda `PENDING_AUTHORIZATION`, con el cupo reservado, y se avisa al paciente. La recepción la resuelve después.
4. La EPS se integra mediante el puerto `AutorizadorEps` y el adaptador `EpsHttpAdapter` (capa anticorrupción). El dominio no conoce el formato de la EPS.

## 4. Alternativas consideradas y descartadas

| Alternativa | Qué tenía a favor | Por qué se descartó |
|---|---|---|
| **A. Llamada síncrona sin límite** (esperar lo que tarde la EPS) | La respuesta siempre es definitiva: aprobada o negada | Una EPS lenta deja al paciente esperando indefinidamente y retiene hilos de Agenda. En el pico eso agota el servidor (incumple E2 y P1) |
| **B. Totalmente asíncrona:** la cita siempre nace "pendiente" y la autorización llega después por un evento | Máximo desacoplamiento de la EPS | El 100 % de los pacientes recibe una respuesta incierta, aunque la EPS conteste en 200 ms. Empeora la usabilidad para el caso normal y obliga a construir desde ya el flujo de reconciliación |
| **C. No validar al agendar; validar el día de la cita** | Agendar sería instantáneo | Se descubre la negación cuando el paciente ya está en la sede y el cupo se perdió para otro. Para el negocio es el peor resultado |
| **D. Rechazar la cita si la EPS no responde** (fail-closed) | Nunca hay citas sin autorización | Una caída de la EPS paraliza el agendamiento de especialistas: es exactamente lo que el driver 3 quiere evitar |
| **E. Circuit breaker + reintentos** (por ejemplo, Resilience4j) | Deja de golpear a una EPS caída y reintenta errores transitorios | **No se descarta, se pospone:** el timeout ya cumple la medida de E2. Los reintentos dentro de la petición aumentarían la latencia percibida. Es el siguiente paso cuando haya varias EPS reales |

## 5. Consecuencias

**Positivas**
- El paciente siempre recibe respuesta en < 3 s (E2). Se demuestra con documentos `888…` o deteniendo la EPS en vivo.
- Una EPS lenta no frena otras reservas, porque la llamada ocurre fuera del bloqueo.
- Cambiar de EPS o de protocolo solo toca el paquete `eps` (E5).

**Negativas (aceptadas)**
- **Cupos "en el limbo":** una cita `PENDING_AUTHORIZATION` ocupa el cupo y puede terminar negada. Si la EPS nunca responde, el cupo queda retenido. Hace falta una expiración y una reconciliación que el esqueleto **no** tiene (riesgo R5).
- **Más estados y más trabajo humano:** la recepción tiene una nueva bandeja de pendientes.
- **Ventana de inconsistencia:** durante hasta 2 s la cita existe sin autorización confirmada.
- **El timeout de 2 s es una apuesta:** si una EPS responde normalmente en 2,5 s, todas sus citas quedarían pendientes. Hay que medir la latencia real de cada EPS y configurar el timeout por EPS.
