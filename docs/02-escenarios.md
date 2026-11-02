# 2. Escenarios de atributos de calidad

Cada escenario tiene seis partes: fuente, estímulo, artefacto, entorno, respuesta y medida de la respuesta. Al final de cada uno se indica cómo se demuestra en el esqueleto.

---

## E1 · Dos pacientes piden el mismo cupo al mismo tiempo

*Atributo: integridad / concurrencia · Hoja C1 (A,A) · Driver 1*

| Parte | Valor |
|---|---|
| **Fuente** | Varios pacientes desde el portal web |
| **Estímulo** | 8 solicitudes simultáneas de cita para el **mismo** médico y el **mismo** cupo |
| **Artefacto** | Servicio Agenda (reserva de cupos) |
| **Entorno** | Operación pico: apertura de agenda, con la EPS respondiendo normalmente |
| **Respuesta** | Una sola solicitud obtiene el cupo. Las demás reciben un rechazo explícito ("el cupo ya no está disponible") y pueden elegir otro |
| **Medida** | **0** cupos asignados dos veces en 100 % de las ráfagas; exactamente **1** respuesta exitosa por cupo |

**En el esqueleto:** el paso 6 de `demo.sh` lanza 8 reservas en paralelo sobre el mismo cupo y cuenta las exitosas (debe dar 1). Código: `AgendaService.reservarCupo` (sección crítica con un único escritor).

---

## E2 · La EPS no responde

*Atributo: disponibilidad · Hoja D1 (A,M) · Driver 3*

| Parte | Valor |
|---|---|
| **Fuente** | Sistema externo de autorizaciones de la EPS |
| **Estímulo** | La EPS tarda más de 2 s en responder o rechaza la conexión |
| **Artefacto** | Servicio Agenda (adaptador de la EPS) |
| **Entorno** | Operación normal, en horario de atención, con un paciente que pide una especialidad que requiere autorización |
| **Respuesta** | Agenda corta la espera, **reserva el cupo** con estado `PENDING_AUTHORIZATION`, se lo informa al paciente y publica el evento para que Notificaciones y Historia Clínica se enteren |
| **Medida** | Respuesta al paciente en **< 3 s** (timeout de 2 s más el procesamiento); **0** errores HTTP 5xx hacia el paciente por culpa de la EPS |

**En el esqueleto:** los documentos que empiezan por `888` hacen que el simulador de la EPS tarde 5 s. También se puede detener la EPS en vivo con `docker compose stop eps-simulador`. Código: `EpsHttpAdapter` (timeout de 2 s) y `AgendaService.agendar` (degradación).

---

## E3 · Consulta de una historia clínica

*Atributo: seguridad (confidencialidad y no repudio) · Hojas S1 (A,M) y S2 (A,B) · Driver 2*

| Parte | Valor |
|---|---|
| **Fuente** | Un usuario interno (médico o recepcionista) o un paciente |
| **Estímulo** | Solicita la historia clínica completa de un paciente |
| **Artefacto** | Servicio Historia Clínica y sus datos |
| **Entorno** | Operación normal |
| **Respuesta** | La historia se entrega desde el **único** servicio que guarda datos clínicos, y queda un registro de auditoría con quién, cuándo y qué. Ningún otro servicio, cola ni evento tiene diagnósticos, alergias, prescripciones ni resultados |
| **Medida** | **100 %** de las consultas auditadas; **0** campos clínicos en los contratos de eventos (verificable leyendo [04-contratos-eventos.md](04-contratos-eventos.md)) |

**En el esqueleto:** `GET /api/clinica/historia/{id}` con la cabecera `X-Usuario`, y luego `GET /api/clinica/auditoria/{id}`. Código: `HistoriaClinicaService.verHistoriaCompleta`.
**Limitación consciente:** el esqueleto confía en la cabecera `X-Usuario` y no valida roles (hoja S4). Ver el riesgo R4 en [06-riesgos.md](06-riesgos.md).

---

## E4 · El servicio de notificaciones se cae

*Atributo: disponibilidad (aislamiento de fallos) · Hoja D2 (A,B) · Driver 3*

| Parte | Valor |
|---|---|
| **Fuente** | Falla interna (proceso de Notificaciones detenido o el proveedor de correo caído) |
| **Estímulo** | Un paciente agenda una cita mientras Notificaciones no está corriendo |
| **Artefacto** | Servicios Agenda y Notificaciones, y el broker |
| **Entorno** | Operación degradada: un servicio caído |
| **Respuesta** | La cita se agenda normalmente. El evento queda en la cola durable de Notificaciones y, cuando el servicio vuelve, envía el aviso. Un evento repetido no genera un aviso duplicado |
| **Medida** | **0** citas rechazadas por la caída; **0** avisos perdidos; **0** avisos duplicados |

**En el esqueleto:**
`docker compose stop notificaciones` → agendar → `docker compose start notificaciones` → `GET /api/notificaciones?patientId=...` muestra el aviso. Código: colas durables en `EventosConfig` y consumidor idempotente en `NotificacionService`.

---

## E5 · Se firma convenio con una EPS nueva

*Atributo: modificabilidad · Hoja M1 (M,M)*

| Parte | Valor |
|---|---|
| **Fuente** | Gerencia comercial de la IPS |
| **Estímulo** | Hay que integrar una EPS cuya API de autorizaciones usa otro formato |
| **Artefacto** | Servicio Agenda, módulo de integración con la EPS |
| **Entorno** | Tiempo de desarrollo |
| **Respuesta** | Se escribe un adaptador nuevo que implementa el puerto `AutorizadorEps`. El dominio de Agenda, los eventos y los demás servicios no cambian |
| **Medida** | ≤ **3 días-persona**; **0** archivos modificados fuera del paquete `eps` (salvo la configuración) |

---

## E6 · Apertura de agenda (pico)

*Atributo: rendimiento / escalabilidad · Hoja P1 (M,A)*

| Parte | Valor |
|---|---|
| **Fuente** | Pacientes de todas las sedes |
| **Estímulo** | 300 solicitudes de cita por minuto durante 15 minutos |
| **Artefacto** | Gateway y servicio Agenda |
| **Entorno** | Lunes a las 7:00, apertura mensual de agendas |
| **Respuesta** | Se atienden todas las solicitudes; solo Agenda necesita más recursos y Historia Clínica no se ve afectada |
| **Medida** | p95 < 2 s (sin contar la EPS); 0 % de errores 5xx |

**En el esqueleto:** **no** se demuestra; el diseño lo prepara (Agenda se escala aparte) pero el bloqueo en memoria lo impide hoy. Ver el riesgo R2.
