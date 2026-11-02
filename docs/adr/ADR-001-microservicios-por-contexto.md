# ADR-001 · Microservicios por contexto delimitado

*Fecha: 2026-11-02 · Decisores: equipo del proyecto*

## 1. Estado

**Aceptada.**

## 2. Contexto

La primera versión del sistema (carpeta `backend/` de este repositorio) es **un monolito**: un único proceso Spring Boot con un `ClinicaFacade` que coordina cinco servicios internos (`PacienteService`, `AgendaService`, `HistoriaClinicaService`, `PrescripcionService`, `LaboratorioService`) sobre mapas en memoria. Funciona, pero:

- **Driver 2 (confidencialidad, RS1/RS2):** cualquier clase del proceso puede leer las alergias, las prescripciones y los resultados. Un error o una vulnerabilidad en la parte de citas expone la historia clínica. Custodia, retención y auditoría no tienen un único dueño.
- **Driver 3 (disponibilidad):** todo cae junto. Si el envío de avisos o la llamada a la EPS bloquean hilos, se degradan también la historia y el login.
- **Driver 1 (integridad, C1)** y **P1 (picos):** la reserva de cupos es la parte que recibe las ráfagas, pero no puede escalarse sin replicar también los datos clínicos.

Restricciones que pesan: equipo de 3 personas, un semestre, stack Java/Spring + React conocido (RS6).

## 3. Decisión

Dividir el sistema en **tres servicios desplegados por separado, uno por contexto delimitado**: **Agenda**, **Historia Clínica** y **Notificaciones**. Cada uno tiene su almacén propio y nadie lee el almacén de otro. Un **API Gateway** (nginx) es el único punto de entrada y enruta por prefijo de URL. El frontend no sabe cuántos servicios hay.

La cantidad (tres, no más) es deliberada: solo se separa donde hay una señal clara (regulación, concurrencia o integración externa; ver [03 §3.2](../03-estilo-contextos-tacticas.md#32-contextos-delimitados)).

## 4. Alternativas consideradas y descartadas

| Alternativa | Qué tenía a favor | Por qué se descartó |
|---|---|---|
| **A. Mantener el monolito con Facade** (la versión actual) | Cero trabajo nuevo, una sola transacción, simple de depurar | No resuelve ningún driver: los datos clínicos siguen al alcance de todo el código (driver 2) y un fallo arrastra a todo (driver 3) |
| **B. Monolito modular en capas** (módulos con dependencias verificadas por ArchUnit, una sola base de datos con esquemas separados) | **Es la alternativa más fuerte.** Mucho más simple de operar, consistencia inmediata, sin broker. Resuelve bien el driver 1 | Los módulos comparten proceso, memoria y credenciales de base de datos: el aislamiento del driver 2 queda en *convención* y no en *despliegue*. No permite escalar solo la agenda en el pico (P1). Un fallo de recursos (hilos o memoria) por la EPS o los avisos sigue afectando a la historia (driver 3). **Se reconoce como la opción correcta si los drivers 2 y 3 no fueran prioritarios** |
| **C. SOA con bus empresarial (ESB)** | Orquestación central y transformación de mensajes | Requiere infraestructura pesada y un equipo de integración. Concentra la lógica en el bus, que se vuelve un nuevo monolito y un punto único de falla. Desproporcionado para tres contextos (RS6) |
| **D. Microservicios finos** (un servicio por entidad: pacientes, prescripción, laboratorio, citas, médicos…) | Máxima independencia | La regla de alergias necesitaría datos clínicos por la red (empeora el driver 2). Sin señal de cambio independiente, multiplicar servicios solo agrega latencia y operación. Inviable para 3 personas |
| **E. Serverless (funciones)** | Escala automática en el pico | Arranques en frío durante la apertura de agenda (P1). El estado de los cupos necesita un almacén externo con bloqueos. Hay dependencia de un proveedor de nube y presupuesto cero (RS6) |

## 5. Consecuencias

**Positivas**
- Los datos clínicos tienen un solo dueño y un solo proceso (driver 2). La auditoría y la futura exportación interoperable (RS3) viven en un lugar.
- Un fallo en Notificaciones o en la EPS no tumba la historia ni el login (driver 3). Se demuestra con `docker compose stop notificaciones`.
- Agenda puede escalarse sola en los picos (P1), **una vez** se resuelva R2.
- El estilo se demuestra: seis contenedores en el diagrama, seis procesos en `docker compose ps`.

**Negativas (aceptadas)**
- **Más complejidad operativa:** seis contenedores, un broker y logs repartidos. Para el equipo es el costo más visible.
- **Consistencia eventual entre contextos** (ver ADR-002). Por ejemplo, un paciente recién registrado no puede agendar durante unos milisegundos.
- **Sin transacciones entre servicios:** guardar y publicar no son atómicos (riesgo R1).
- **Duplicación de datos personales** (nombre, documento, correo) en Agenda y Notificaciones, lo que es más superficie frente a la Ley 1581 (riesgo R6).
- **Latencia de red adicional** por el gateway.
- La seguridad del borde (autenticación y roles en el gateway) pasa a ser obligatoria y todavía no está (riesgo R4).
