# 4. Contratos de eventos

- **Broker:** RabbitMQ · **Exchange:** `clinica.eventos` (tipo *topic*, durable)
- **Formato:** JSON (Jackson). El consumidor usa su propio tipo y no la clase del publicador (*lector tolerante*).
- **Garantía de entrega:** al menos una vez → los consumidores son **idempotentes**.
- **Regla de evolución:** solo se **agregan** campos. Nunca se renombra ni se elimina un campo (si hiciera falta, se crea un `v2` con otra routing key).
- **Regla de seguridad (driver 2):** ningún evento contiene datos clínicos (alergias, diagnósticos, prescripciones ni resultados).

## Tabla de eventos

| Routing key | Publicador | Colas consumidoras (servicio) | Para qué lo usa el consumidor |
|---|---|---|---|
| `paciente.registrado` | Historia Clínica | `agenda.paciente-registrado` (Agenda)<br>`notificaciones.paciente-registrado` (Notificaciones) | Agenda: saber que el paciente existe y cuál es su EPS para pedir autorización<br>Notificaciones: guardar el contacto y enviar la bienvenida |
| `cita.agendada` | Agenda | `historia.citas` (Historia Clínica)<br>`notificaciones.citas` (Notificaciones) | Historia: agregar la cita a la historia completa del paciente<br>Notificaciones: confirmación o aviso de "pendiente de autorización" |
| `cita.cancelada` | Agenda | `historia.citas` (Historia Clínica)<br>`notificaciones.citas` (Notificaciones) | Historia: marcar la cita como cancelada<br>Notificaciones: avisar la cancelación |

**Cada evento tiene dos consumidores que reaccionan por separado**, en procesos distintos y con colas distintas: si uno está caído, el otro procesa igual.

## Cargas útiles

### `paciente.registrado`
```json
{
  "patientId": "PAT-1A2B3C4D",
  "documentNumber": "1012345678",
  "fullName": "Ana Gómez",
  "email": "ana@correo.com",
  "phone": "3001234567",
  "eps": "Sura",
  "occurredAt": "2026-11-02T20:15:03.120Z"
}
```

### `cita.agendada` / `cita.cancelada`
```json
{
  "appointmentId": "APT-9F8E7D6C",
  "patientId": "PAT-1A2B3C4D",
  "doctorId": "DOC-001",
  "doctorName": "Dr. Andrés Morales",
  "specialty": "Cardiología",
  "date": "2026-11-04 10:00",
  "status": "SCHEDULED | PENDING_AUTHORIZATION | CANCELLED",
  "notes": "Recuerde llegar 15 minutos antes con su documento.",
  "occurredAt": "2026-11-02T20:16:10.004Z"
}
```

## Contrato síncrono con el sistema externo (EPS)

`POST {EPS_URL}/autorizaciones` · timeout de 2 s

```json
// solicitud
{ "documentNumber": "1012345678", "eps": "Sura", "specialty": "Cardiología" }
// respuesta
{ "decision": "APROBADA | NEGADA", "numeroAutorizacion": "AUT-123456" }
```

Comportamiento del simulador (`servicios/eps-simulador/mappings`):

| Documento del paciente | Respuesta de la EPS simulada |
|---|---|
| empieza por `999` | `NEGADA` |
| empieza por `888` | `APROBADA`, pero tarda 5 s (supera el timeout → `PENDING_AUTHORIZATION`) |
| cualquier otro | `APROBADA` con un número de autorización |
